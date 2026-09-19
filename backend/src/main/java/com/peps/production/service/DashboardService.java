package com.peps.production.service;

import com.peps.production.dto.*;
import com.peps.production.model.*;
import com.peps.production.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final ProductionRepository repository;
    private final SettingsService settingsService;
    private final DowntimeEventRepository downtimeRepository;
    private final ProductionAlertRepository alertRepository;

    public DashboardService(ProductionRepository repository,
                            SettingsService settingsService,
                            DowntimeEventRepository downtimeRepository,
                            ProductionAlertRepository alertRepository) {
        this.repository = repository;
        this.settingsService = settingsService;
        this.downtimeRepository = downtimeRepository;
        this.alertRepository = alertRepository;
    }

    /**
     * Get real-time dashboard snapshot strictly from persisted MySQL data and deterministic calculations.
     * READ-ONLY: Calling this method never creates production events.
     */
    public DashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime dayStart = today.atStartOfDay();

        // 1. Fetch today's completed production records up to now
        List<ProductionData> todayEvents = repository.findByStatusAndCompletionTimeBetween(
                ProductionStatus.COMPLETED, dayStart, now);

        DashboardResponse response = new DashboardResponse();

        int springCount = 0;
        int hypnosCount = 0;

        Map<String, SizeBreakdown> sizeBreakdown = new LinkedHashMap<>();
        for (MattressSize size : MattressSize.values()) {
            sizeBreakdown.put(size.name().toLowerCase(), new SizeBreakdown());
        }

        HourlyProduction hourly = new HourlyProduction();
        int currentHour = now.getHour();

        for (ProductionData item : todayEvents) {
            boolean isSpring = item.getProductType() == ProductType.SPRING;
            if (isSpring) {
                springCount++;
            } else {
                hypnosCount++;
            }

            // Size aggregation
            String sizeKey = item.getSize().name().toLowerCase();
            SizeBreakdown sb = sizeBreakdown.get(sizeKey);
            if (sb != null) {
                if (isSpring) {
                    sb.addSpring(1);
                } else {
                    sb.addHypnos(1);
                }
            }

            // Hourly aggregation (only past/current hours)
            int eventHour = item.getCompletionTime().getHour();
            if (eventHour >= 0 && eventHour < 24 && eventHour <= currentHour) {
                if (isSpring) {
                    hourly.getSpring()[eventHour]++;
                } else {
                    hourly.getHypnos()[eventHour]++;
                }
            }
        }

        int totalProduction = springCount + hypnosCount;
        response.setSpringCount(springCount);
        response.setHypnosCount(hypnosCount);
        response.setTotalProduction(totalProduction);

        // 2. Current Shift and Target calculations
        Optional<ShiftConfiguration> currentShiftOpt = settingsService.getCurrentShift();
        DashboardResponse.CurrentShiftInfo shiftInfo = new DashboardResponse.CurrentShiftInfo();

        int totalHourlyTarget = settingsService.calculateTotalHourlyTarget();
        int shiftTarget = 0;
        int targetPercentage = 0;
        int efficiency = 0;

        if (currentShiftOpt.isPresent()) {
            ShiftConfiguration shift = currentShiftOpt.get();
            double durationHours = shift.getDurationHours();
            shiftTarget = (int) Math.round(totalHourlyTarget * durationHours);

            shiftInfo.setShiftName(shift.getShiftName());
            shiftInfo.setStartTime(shift.getStartTime().toString());
            shiftInfo.setEndTime(shift.getEndTime().toString());
            shiftInfo.setDurationHours(durationHours);
            shiftInfo.setActive(true);

            // Target % = Actual Production / Shift Target * 100
            if (shiftTarget > 0) {
                targetPercentage = (int) Math.round(((double) totalProduction / shiftTarget) * 100.0);
            }

            // Efficiency = Actual Production / Expected Production By Now * 100
            double elapsedHours = calculateElapsedShiftHours(shift, now.toLocalTime());
            double expectedByNow = totalHourlyTarget * elapsedHours;

            if (expectedByNow > 0) {
                efficiency = (int) Math.min(100, Math.round(((double) totalProduction / expectedByNow) * 100.0));
            } else if (elapsedHours == 0 && totalProduction > 0) {
                efficiency = 100;
            } else {
                efficiency = 0;
            }
        } else {
            shiftInfo.setShiftName("No Active Shift");
            shiftInfo.setActive(false);
            if (totalHourlyTarget > 0) {
                shiftTarget = totalHourlyTarget * 8;
                if (shiftTarget > 0) {
                    targetPercentage = (int) Math.round(((double) totalProduction / shiftTarget) * 100.0);
                }
            }
        }

        response.setCurrentShift(shiftInfo);
        response.setShiftTarget(shiftTarget);
        response.setTargetPercentage(targetPercentage);
        response.setEfficiency(efficiency);

        // 3. Downtime today
        Long downtimeMinutes = downtimeRepository.sumDurationMinutesBetween(dayStart, now);
        response.setDowntimeMinutes(downtimeMinutes != null ? downtimeMinutes : 0L);

        // 4. Size Breakdown & Hourly Data
        response.setSizeBreakdown(sizeBreakdown);
        response.setHourlyData(hourly);

        // 5. Recent production events feed (1 completed mattress = 1 row, newest first)
        List<RecentProduction> recentItems = repository.findTop10ByStatusOrderByCompletionTimeDesc(ProductionStatus.COMPLETED)
                .stream()
                .map(item -> new RecentProduction(
                        item.getProductType().name(),
                        item.getVariety() != null ? item.getVariety() : "Standard",
                        item.getSize().name(),
                        item.getCompletionTime(),
                        item.getProductionLine() != null ? item.getProductionLine() : "Line 1",
                        item.getStatus().name(),
                        item.getShift() != null ? item.getShift() : "Current Shift"
                ))
                .toList();
        response.setRecentItems(recentItems);

        // 6. Active alerts
        List<DashboardResponse.AlertDto> alerts = alertRepository.findByStatusOrderByCreatedTimestampDesc("ACTIVE")
                .stream()
                .limit(10)
                .map(a -> new DashboardResponse.AlertDto(
                        a.getAlertType(),
                        a.getMessage(),
                        a.getSeverity(),
                        a.getStatus(),
                        a.getCreatedTimestamp().toString()
                ))
                .toList();
        response.setAlerts(alerts);

        return response;
    }

    /**
     * Compute elapsed scheduled hours in the active shift
     */
    private double calculateElapsedShiftHours(ShiftConfiguration shift, LocalTime currentTime) {
        LocalTime start = shift.getStartTime();
        LocalTime end = shift.getEndTime();

        long elapsedMinutes = 0;
        if (end.isAfter(start)) {
            // Normal shift within same day
            if (currentTime.isBefore(start)) {
                return 0.0;
            } else if (currentTime.isAfter(end)) {
                elapsedMinutes = Duration.between(start, end).toMinutes();
            } else {
                elapsedMinutes = Duration.between(start, currentTime).toMinutes();
            }
        } else {
            // Overnight shift (e.g. 22:00 to 06:00)
            if (currentTime.isBefore(start) && currentTime.isAfter(end)) {
                return 0.0;
            }
            if (currentTime.isBefore(end)) {
                elapsedMinutes = Duration.between(start, LocalTime.MAX).toMinutes() + Duration.between(LocalTime.MIN, currentTime).toMinutes() + 1;
            } else {
                elapsedMinutes = Duration.between(start, currentTime).toMinutes();
            }
        }

        return Math.max(0.0, elapsedMinutes / 60.0);
    }
}
