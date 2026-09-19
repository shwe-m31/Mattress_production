package com.peps.production.controller;

import com.peps.production.dto.*;
import com.peps.production.model.*;
import com.peps.production.repository.*;
import com.peps.production.service.AlertService;
import com.peps.production.service.DashboardService;
import com.peps.production.service.DowntimeSimulationService;
import com.peps.production.service.SettingsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ProductionController {
    private final ProductionRepository repository;
    private final DashboardService dashboardService;
    private final SettingsService settingsService;
    private final AlertService alertService;
    private final DowntimeSimulationService downtimeService;
    private final DowntimeEventRepository downtimeRepository;
    private final ProductionTargetRepository targetRepository;

    public ProductionController(ProductionRepository repository,
                                DashboardService dashboardService,
                                SettingsService settingsService,
                                AlertService alertService,
                                DowntimeSimulationService downtimeService,
                                DowntimeEventRepository downtimeRepository,
                                ProductionTargetRepository targetRepository) {
        this.repository = repository;
        this.dashboardService = dashboardService;
        this.settingsService = settingsService;
        this.alertService = alertService;
        this.downtimeService = downtimeService;
        this.downtimeRepository = downtimeRepository;
        this.targetRepository = targetRepository;
    }

    @GetMapping({"/dashboard", "/production/dashboard"})
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }

    @GetMapping({"/daily", "/production/daily"})
    public ResponseEntity<List<DailyProduction>> getDailyProduction() {
        List<DailyProduction> dailyData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int hourlyTarget = settingsService.calculateTotalHourlyTarget();
        int dailyTarget = hourlyTarget > 0 ? hourlyTarget * 8 : 656; // 8-hour target baseline

        for (int i = 0; i < 14; i++) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = (i == 0) ? LocalDateTime.now() : date.atTime(23, 59, 59);

            List<ProductionData> dayEvents = repository.findByStatusAndCompletionTimeBetween(
                    ProductionStatus.COMPLETED, start, end);

            DailyProduction dp = new DailyProduction();
            dp.setDate(date.format(DateTimeFormatter.ofPattern("MMM d")));

            int spring = 0, hypnos = 0;
            int king = 0, queen = 0, doubleSize = 0, single = 0;

            for (ProductionData event : dayEvents) {
                if (event.getProductType() == ProductType.SPRING) {
                    spring++;
                } else {
                    hypnos++;
                }

                if (event.getSize() != null) {
                    switch (event.getSize()) {
                        case KING -> king++;
                        case QUEEN -> queen++;
                        case DOUBLE -> doubleSize++;
                        case SINGLE -> single++;
                    }
                }
            }

            int total = spring + hypnos;
            dp.setSpring(spring);
            dp.setHypnos(hypnos);
            dp.setTotal(total);
            dp.setKing(king);
            dp.setQueen(queen);
            dp.setDouble(doubleSize);
            dp.setSingle(single);

            int efficiency = (dailyTarget > 0 && total > 0) ? 
                    (int) Math.min(100, Math.round(((double) total / dailyTarget) * 100.0)) : 0;
            dp.setEfficiency(efficiency);

            Long downtimeMins = downtimeRepository.sumDurationMinutesBetween(start, end);
            dp.setDowntime(downtimeMins != null ? downtimeMins.intValue() : 0);

            dailyData.add(dp);
        }

        return ResponseEntity.ok(dailyData);
    }

    @GetMapping({"/weekly", "/production/weekly"})
    public ResponseEntity<WeeklyResponse> getWeeklyProduction() {
        List<WeeklyProduction> weeklySummary = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int hourlyTarget = settingsService.calculateTotalHourlyTarget();
        int weeklyTarget = (hourlyTarget > 0 ? hourlyTarget * 8 * 6 : 3936); // 6 working days * 8h

        // 8 weeks history
        for (int i = 0; i < 8; i++) {
            LocalDate weekEnd = today.minusWeeks(i);
            LocalDate weekStart = weekEnd.minusDays(6);

            LocalDateTime start = weekStart.atStartOfDay();
            LocalDateTime end = (i == 0) ? LocalDateTime.now() : weekEnd.atTime(23, 59, 59);

            List<ProductionData> weekEvents = repository.findByStatusAndCompletionTimeBetween(
                    ProductionStatus.COMPLETED, start, end);

            WeeklyProduction wp = new WeeklyProduction();
            wp.setWeek("W" + (8 - i));

            int spring = 0, hypnos = 0;
            for (ProductionData event : weekEvents) {
                if (event.getProductType() == ProductType.SPRING) {
                    spring++;
                } else {
                    hypnos++;
                }
            }

            int total = spring + hypnos;
            wp.setSpring(spring);
            wp.setHypnos(hypnos);
            wp.setTotal(total);
            wp.setTarget(weeklyTarget);
            wp.setEfficiency(weeklyTarget > 0 ? (int) Math.min(100, Math.round(((double) total / weeklyTarget) * 100.0)) : 0);

            weeklySummary.add(wp);
        }

        // Calculate "Order Fulfilment by Product" for current week
        LocalDate thisWeekStart = today.with(DayOfWeek.MONDAY);
        LocalDateTime currentWeekStart = thisWeekStart.atStartOfDay();
        LocalDateTime currentWeekEnd = LocalDateTime.now();

        List<ProductionData> currentWeekEvents = repository.findByStatusAndCompletionTimeBetween(
                ProductionStatus.COMPLETED, currentWeekStart, currentWeekEnd);

        List<ProductFulfilmentDto> fulfilmentList = new ArrayList<>();
        for (ProductType pType : ProductType.values()) {
            for (MattressSize size : MattressSize.values()) {
                String label = (pType == ProductType.SPRING ? "Spring " : "Hypnos ") +
                        size.name().charAt(0) + size.name().substring(1).toLowerCase();

                int hourlyProductTarget = targetRepository.findByProductTypeAndSize(pType, size)
                        .map(ProductionTarget::getHourlyTarget).orElse(10);
                
                // Weekly planned = hourly target * 48 hours (6 days * 8h)
                int planned = hourlyProductTarget * 48;

                // Actual produced this week for this product & size
                long actualCount = currentWeekEvents.stream()
                        .filter(e -> e.getProductType() == pType && e.getSize() == size)
                        .count();

                fulfilmentList.add(new ProductFulfilmentDto(label, planned, (int) actualCount));
            }
        }

        return ResponseEntity.ok(new WeeklyResponse(weeklySummary, fulfilmentList));
    }

    @GetMapping({"/monthly", "/production/monthly"})
    public ResponseEntity<List<MonthlyProduction>> getMonthlyProduction() {
        List<MonthlyProduction> monthlyData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int hourlyTarget = settingsService.calculateTotalHourlyTarget();
        int monthlyTarget = (hourlyTarget > 0 ? hourlyTarget * 8 * 25 : 16400); // 25 working days * 8h

        for (int i = 0; i < 12; i++) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            LocalDateTime start = monthStart.atStartOfDay();
            LocalDateTime end = (i == 0) ? LocalDateTime.now() : monthEnd.atTime(23, 59, 59);

            List<ProductionData> monthEvents = repository.findByStatusAndCompletionTimeBetween(
                    ProductionStatus.COMPLETED, start, end);

            MonthlyProduction mp = new MonthlyProduction();
            mp.setMonth(monthStart.format(DateTimeFormatter.ofPattern("MMM yyyy")));

            int spring = 0, hypnos = 0;
            int king = 0, queen = 0, doubleSize = 0, single = 0;

            for (ProductionData event : monthEvents) {
                if (event.getProductType() == ProductType.SPRING) {
                    spring++;
                } else {
                    hypnos++;
                }

                if (event.getSize() != null) {
                    switch (event.getSize()) {
                        case KING -> king++;
                        case QUEEN -> queen++;
                        case DOUBLE -> doubleSize++;
                        case SINGLE -> single++;
                    }
                }
            }

            int total = spring + hypnos;
            mp.setSpring(spring);
            mp.setHypnos(hypnos);
            mp.setTotal(total);
            mp.setTarget(monthlyTarget);
            mp.setKing(king);
            mp.setQueen(queen);
            mp.setDouble(doubleSize);
            mp.setSingle(single);

            monthlyData.add(mp);
        }

        return ResponseEntity.ok(monthlyData);
    }

    @GetMapping({"/recent", "/production/recent"})
    public ResponseEntity<List<RecentProduction>> getRecentProduction(
            @RequestParam(defaultValue = "15") int limit) {
        List<ProductionData> recent = repository.findTop10ByStatusOrderByCompletionTimeDesc(ProductionStatus.COMPLETED);
        return ResponseEntity.ok(recent.stream()
                .limit(limit)
                .map(item -> new RecentProduction(
                        item.getProductType().name(),
                        item.getVariety() != null ? item.getVariety() : "Standard",
                        item.getSize().name(),
                        item.getCompletionTime(),
                        item.getProductionLine() != null ? item.getProductionLine() : "Line 1",
                        item.getStatus().name(),
                        item.getShift() != null ? item.getShift() : "Current Shift"
                ))
                .toList());
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<ProductionAlert>> getAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    @GetMapping("/downtime")
    public ResponseEntity<Map<String, Object>> getDowntime() {
        Map<String, Object> dt = new HashMap<>();
        dt.put("todayDowntimeMinutes", downtimeService.getTodayDowntime());
        dt.put("inDowntime", downtimeService.isCurrentlyInDowntime());
        return ResponseEntity.ok(dt);
    }

    @GetMapping("/status")
    public ResponseEntity<ProductionStatusResponse> getProductionStatus() {
        ProductionStatusResponse status = new ProductionStatusResponse();
        status.setConnectionStatus("Connected");
        status.setDataSource("Simulated Data Acquisition");
        status.setLastUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        status.setSimulatorActive(true);
        status.setTotalRecords((int) repository.count());
        return ResponseEntity.ok(status);
    }
}