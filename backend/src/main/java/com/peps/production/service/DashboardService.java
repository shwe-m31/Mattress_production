package com.peps.production.service;

import com.peps.production.dto.*;
import com.peps.production.model.*;
import com.peps.production.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    
    private final ProductionRepository repository;
    private final ProductionTimeService timeService;
    private final ProductionSimulationService simulationService;
    private final SettingsService settingsService;
    private final DowntimeEventRepository downtimeRepository;
    private final ProductionAlertRepository alertRepository;
    
    public DashboardService(ProductionRepository repository, 
                           ProductionTimeService timeService,
                           ProductionSimulationService simulationService,
                           SettingsService settingsService,
                           DowntimeEventRepository downtimeRepository,
                           ProductionAlertRepository alertRepository) {
        this.repository = repository;
        this.timeService = timeService;
        this.simulationService = simulationService;
        this.settingsService = settingsService;
        this.downtimeRepository = downtimeRepository;
        this.alertRepository = alertRepository;
    }

    /**
     * Get dashboard data - READ-ONLY operation
     * Synchronizes production state before returning data to ensure consistency
     */
    public DashboardResponse getDashboard() {
        // First synchronize production to ensure data is up-to-date
        try {
            simulationService.synchronizeProduction();
        } catch (Exception e) {
            logger.error("Error during production synchronization in dashboard", e);
        }
        
        LocalDateTime dayStart = timeService.getCurrentDate().atStartOfDay();
        LocalDateTime now = timeService.getCurrentTime();
        
        // Only get production up to current time, not future
        LocalDateTime queryEndTime = now;
        
        List<ProductionData> today = repository.findByStatusAndCompletionTimeBetween(
            ProductionStatus.COMPLETED, dayStart, queryEndTime);
        
        DashboardResponse response = new DashboardResponse();
        Map<String, SizeBreakdown> breakdown = new LinkedHashMap<>();
        for (MattressSize size : MattressSize.values()) {
            breakdown.put(size.name().toLowerCase(), new SizeBreakdown());
        }
        
        HourlyProduction hourly = new HourlyProduction();
        int spring = 0, hypnos = 0;
        
        // Count production events (one event = one mattress)
        for (ProductionData item : today) {
            boolean isSpring = item.getProductType() == ProductType.SPRING;
            
            if (isSpring) {
                spring++;
            } else {
                hypnos++;
            }
            
            SizeBreakdown size = breakdown.get(item.getSize().name().toLowerCase());
            if (isSpring) {
                size.addSpring(1);
            } else {
                size.addHypnos(1);
            }
            
            // Hourly data - only include hours that have passed
            int hourIndex = item.getCompletionTime().getHour();
            if (hourIndex >= 0 && hourIndex < 24) {
                if (isSpring) {
                    hourly.getSpring()[hourIndex]++;
                } else {
                    hourly.getHypnos()[hourIndex]++;
                }
            }
        }
        
        response.setSpringCount(spring);
        response.setHypnosCount(hypnos);
        response.setTotalProduction(spring + hypnos);
        
        // Get current shift and calculate targets
        Optional<ShiftConfiguration> currentShift = settingsService.getCurrentShift();
        int shiftTarget = 0;
        DashboardResponse.CurrentShiftInfo shiftInfo = new DashboardResponse.CurrentShiftInfo();
        
        if (currentShift.isPresent()) {
            ShiftConfiguration shift = currentShift.get();
            shiftTarget = settingsService.calculateShiftTarget(shift.getShiftName());
            shiftInfo.setShiftName(shift.getShiftName());
            shiftInfo.setStartTime(shift.getStartTime().toString());
            shiftInfo.setEndTime(shift.getEndTime().toString());
            shiftInfo.setDurationHours(shift.getDurationHours());
            shiftInfo.setActive(true);
        } else {
            shiftInfo.setShiftName("No Active Shift");
            shiftInfo.setActive(false);
        }
        
        response.setCurrentShift(shiftInfo);
        response.setShiftTarget(shiftTarget);
        
        // Calculate target percentage (actual / full shift target)
        int targetPercentage = shiftTarget > 0 ? calculateTargetPercentage(spring + hypnos, shiftTarget) : 0;
        response.setTargetPercentage(targetPercentage);
        
        // Calculate efficiency based on actual production vs expected for elapsed time
        int expectedProduction = calculateExpectedProductionByNow(currentShift, now);
        int efficiency = calculateEfficiency(spring + hypnos, expectedProduction);
        response.setEfficiency(efficiency);
        
        // Calculate downtime for today
        long downtimeMinutes = downtimeRepository.sumDurationMinutesBetween(dayStart, queryEndTime);
        response.setDowntimeMinutes(downtimeMinutes);
        
        response.setSizeBreakdown(breakdown);
        response.setHourlyData(hourly);
        
        // Recent production items (one event = one mattress)
        response.setRecentItems(repository.findTop10ByStatusOrderByCompletionTimeDesc(ProductionStatus.COMPLETED).stream()
            .map(item -> new RecentProduction(
                item.getProductType().name(),
                item.getVariety() != null ? item.getVariety() : "Standard",
                item.getSize().name(),
                item.getCompletionTime(),
                item.getProductionLine() != null ? item.getProductionLine() : "Line 1",
                item.getStatus().name(),
                item.getShift() != null ? item.getShift() : "Unknown"
            )).toList());
        
        // Get active alerts
        List<DashboardResponse.AlertDto> alerts = alertRepository.findByStatusOrderByCreatedTimestampDesc("ACTIVE")
            .stream()
            .limit(10)
            .map(alert -> new DashboardResponse.AlertDto(
                alert.getAlertType(),
                alert.getMessage(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getCreatedTimestamp().toString()
            ))
            .toList();
        response.setAlerts(alerts);
        
        return response;
    }
    
    /**
     * Calculate expected production by current time based on shift and hourly target
     */
    private int calculateExpectedProductionByNow(Optional<ShiftConfiguration> currentShift, LocalDateTime now) {
        if (currentShift.isEmpty()) {
            return 0;
        }
        
        ShiftConfiguration shift = currentShift.get();
        int hourlyTarget = settingsService.calculateTotalHourlyTarget();
        
        if (hourlyTarget == 0) {
            return 0;
        }
        
        LocalTime currentTime = now.toLocalTime();
        LocalTime shiftStart = shift.getStartTime();
        LocalTime shiftEnd = shift.getEndTime();
        
        // Calculate elapsed minutes in current shift
        long elapsedMinutes = 0;
        
        if (shiftEnd.isAfter(shiftStart)) {
            // Normal shift
            if (currentTime.isBefore(shiftStart)) {
                elapsedMinutes = 0;
            } else if (currentTime.isAfter(shiftEnd)) {
                elapsedMinutes = Duration.between(shiftStart, shiftEnd).toMinutes();
            } else {
                elapsedMinutes = Duration.between(shiftStart, currentTime).toMinutes();
            }
        } else {
            // Overnight shift
            if (currentTime.isBefore(shiftStart) && currentTime.isAfter(shiftEnd)) {
                elapsedMinutes = 0; // Between shift end and start
            } else {
                // Within shift hours
                if (currentTime.isBefore(shiftEnd)) {
                    elapsedMinutes = Duration.between(shiftStart, LocalTime.MAX).toMinutes() + 
                                   Duration.between(LocalTime.MIN, currentTime).toMinutes() + 1;
                } else {
                    elapsedMinutes = Duration.between(shiftStart, currentTime).toMinutes();
                }
            }
        }
        
        // Calculate expected production
        double expectedProduction = (elapsedMinutes / 60.0) * hourlyTarget;
        return (int) expectedProduction;
    }
    
    /**
     * Calculate target percentage (actual vs full shift target)
     */
    private int calculateTargetPercentage(int actual, int target) {
        if (target == 0) {
            return 0;
        }
        
        int percentage = (int) Math.round((actual * 100.0) / target);
        return Math.min(100, Math.max(0, percentage));
    }
    
    /**
     * Calculate efficiency based on actual vs expected production
     */
    private int calculateEfficiency(int actual, int expected) {
        if (expected == 0) {
            return 0;
        }
        
        int efficiency = (int) Math.round((actual * 100.0) / expected);
        
        // Cap efficiency at 100% for realistic display
        return Math.min(100, Math.max(0, efficiency));
    }
}
