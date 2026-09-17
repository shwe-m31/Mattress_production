package com.peps.production.controller;

import com.peps.production.dto.*;
import com.peps.production.model.ProductionData;
import com.peps.production.model.ProductionStatus;
import com.peps.production.repository.ProductionRepository;
import com.peps.production.service.DashboardService;
import com.peps.production.service.ProductionSimulatorService;
import com.peps.production.service.ProductionTimeService;
import com.peps.production.service.ProductionSimulationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/production")
public class ProductionController {
    private final ProductionRepository repository;
    private final DashboardService dashboardService;
    private final ProductionSimulatorService simulatorService;
    private final ProductionTimeService timeService;
    private final ProductionSimulationService simulationService;
    
    @Value("${production.daily-target:500}")
    private int dailyTarget;
    
    @Value("${production.weekly-target:2500}")
    private int weeklyTarget;
    
    @Value("${production.monthly-target:10000}")
    private int monthlyTarget;
    
    public ProductionController(ProductionRepository repository, 
                               DashboardService dashboardService,
                               ProductionSimulatorService simulatorService,
                               ProductionTimeService timeService,
                               ProductionSimulationService simulationService) {
        this.repository = repository;
        this.dashboardService = dashboardService;
        this.simulatorService = simulatorService;
        this.timeService = timeService;
        this.simulationService = simulationService;
    }
    
    @GetMapping("/hourly")
    public ResponseEntity<HourlyProduction> getHourlyProduction() {
        // Synchronize production before returning data
        simulationService.synchronizeProduction();
        
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        
        // Only query up to current time, not future
        List<ProductionData> today = repository.findByStatusAndCompletionTimeBetween(
            ProductionStatus.COMPLETED, dayStart, now);
        
        HourlyProduction hourly = new HourlyProduction();
        int currentHour = now.getHour();
        
        for (ProductionData item : today) {
            int hourIndex = item.getCompletionTime().getHour();
            // Only include hours that have already passed
            if (hourIndex <= currentHour && hourIndex >= 0 && hourIndex < 24) {
                boolean isSpring = item.getProductType().name().equals("SPRING");
                if (isSpring) {
                    hourly.getSpring()[hourIndex] += item.getQuantity();
                } else {
                    hourly.getHypnos()[hourIndex] += item.getQuantity();
                }
            }
        }
        
        // Future hours remain zero (default)
        return ResponseEntity.ok(hourly);
    }
    
    @GetMapping("/daily")
    public ResponseEntity<List<DailyProduction>> getDailyProduction() {
        // READ-ONLY: No synchronization needed for historical data
        List<DailyProduction> dailyData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 13; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(23, 59, 59);
            
            List<ProductionData> dayData = repository.findByStatusAndCompletionTimeBetween(
                ProductionStatus.COMPLETED, dayStart, dayEnd);
            
            DailyProduction daily = new DailyProduction();
            daily.setDate(date.format(DateTimeFormatter.ofPattern("MMM d")));
            
            int spring = 0, hypnos = 0;
            int king = 0, queen = 0, doubleSize = 0, single = 0;
            
            for (ProductionData item : dayData) {
                int quantity = item.getQuantity();
                if (item.getProductType().name().equals("SPRING")) {
                    spring += quantity;
                } else {
                    hypnos += quantity;
                }
                
                switch (item.getSize().name()) {
                    case "KING": king += quantity; break;
                    case "QUEEN": queen += quantity; break;
                    case "DOUBLE": doubleSize += quantity; break;
                    case "SINGLE": single += quantity; break;
                }
            }
            
            daily.setSpring(spring);
            daily.setHypnos(hypnos);
            daily.setTotal(spring + hypnos);
            daily.setKing(king);
            daily.setQueen(queen);
            daily.setDouble(doubleSize);
            daily.setSingle(single);
            
            // Use centralized daily target for efficiency calculation
            daily.setEfficiency(calculateEfficiency(spring + hypnos, dailyTarget));
            daily.setDowntime(calculateDowntime(dayData));
            
            dailyData.add(daily);
        }
        
        return ResponseEntity.ok(dailyData);
    }
    
    @GetMapping("/weekly")
    public ResponseEntity<List<WeeklyProduction>> getWeeklyProduction() {
        // READ-ONLY: No synchronization needed for historical data
        List<WeeklyProduction> weeklyData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 7; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            
            LocalDateTime start = weekStart.atStartOfDay();
            LocalDateTime end = weekEnd.atTime(23, 59, 59);
            
            List<ProductionData> weekData = repository.findByStatusAndCompletionTimeBetween(
                ProductionStatus.COMPLETED, start, end);
            
            WeeklyProduction weekly = new WeeklyProduction();
            weekly.setWeek("W" + (8 - i));
            
            int spring = 0, hypnos = 0;
            for (ProductionData item : weekData) {
                if (item.getProductType().name().equals("SPRING")) {
                    spring += item.getQuantity();
                } else {
                    hypnos += item.getQuantity();
                }
            }
            
            weekly.setSpring(spring);
            weekly.setHypnos(hypnos);
            weekly.setTotal(spring + hypnos);
            weekly.setTarget(weeklyTarget); // Use centralized weekly target
            weekly.setEfficiency(calculateEfficiency(spring + hypnos, weeklyTarget));
            
            weeklyData.add(weekly);
        }
        
        return ResponseEntity.ok(weeklyData);
    }
    
    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyProduction>> getMonthlyProduction() {
        // READ-ONLY: No synchronization needed for historical data
        List<MonthlyProduction> monthlyData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 11; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            
            LocalDateTime start = monthStart.atStartOfDay();
            LocalDateTime end = monthEnd.atTime(23, 59, 59);
            
            List<ProductionData> monthData = repository.findByStatusAndCompletionTimeBetween(
                ProductionStatus.COMPLETED, start, end);
            
            MonthlyProduction monthly = new MonthlyProduction();
            monthly.setMonth(monthStart.format(DateTimeFormatter.ofPattern("MMM")));
            
            int spring = 0, hypnos = 0;
            int king = 0, queen = 0, doubleSize = 0, single = 0;
            
            for (ProductionData item : monthData) {
                int quantity = item.getQuantity();
                if (item.getProductType().name().equals("SPRING")) {
                    spring += quantity;
                } else {
                    hypnos += quantity;
                }
                
                switch (item.getSize().name()) {
                    case "KING": king += quantity; break;
                    case "QUEEN": queen += quantity; break;
                    case "DOUBLE": doubleSize += quantity; break;
                    case "SINGLE": single += quantity; break;
                }
            }
            
            monthly.setSpring(spring);
            monthly.setHypnos(hypnos);
            monthly.setTotal(spring + hypnos);
            monthly.setTarget(monthlyTarget); // Use centralized monthly target
            monthly.setKing(king);
            monthly.setQueen(queen);
            monthly.setDouble(doubleSize);
            monthly.setSingle(single);
            
            monthlyData.add(monthly);
        }
        
        return ResponseEntity.ok(monthlyData);
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<RecentProduction>> getRecentProduction(
            @RequestParam(defaultValue = "10") int limit) {
        // READ-ONLY: No synchronization needed, just query existing data
        List<ProductionData> recent = repository.findTop10ByStatusOrderByCompletionTimeDesc(ProductionStatus.COMPLETED);
        return ResponseEntity.ok(recent.stream()
            .limit(limit)
            .map(item -> new RecentProduction(
                item.getProductType().name(),
                item.getVariety() != null ? item.getVariety() : "Standard",
                item.getSize().name(),
                item.getCompletionTime(),
                item.getQuantity(),
                item.getCycleTime() != null ? item.getCycleTime() : 0.0,
                item.getProductionLine() != null ? item.getProductionLine() : "Line 1",
                item.getStatus().name()
            ))
            .toList());
    }
    
    @GetMapping("/status")
    public ResponseEntity<ProductionStatusResponse> getProductionStatus() {
        // READ-ONLY: Just return current system status
        ProductionStatusResponse status = new ProductionStatusResponse();
        status.setConnectionStatus("Connected");
        status.setDataSource("Simulated HMI/PLC");
        status.setLastUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        status.setSimulatorActive(true);
        status.setTotalRecords((int) repository.count());
        
        // Add current production info
        int currentProduction = simulationService.getCurrentProductionCount();
        int expectedProduction = timeService.calculateExpectedProduction(LocalDateTime.now());
        status.setCurrentProduction(currentProduction);
        status.setExpectedProduction(expectedProduction);
        
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<ProductionData>> getProductionHistory(
            @RequestParam(required = false) String line,
            @RequestParam(required = false) String shift,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // READ-ONLY: No synchronization needed for historical queries
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
        
        List<ProductionData> data = repository.findByStatusAndCompletionTimeBetween(
            ProductionStatus.COMPLETED, start, end);
        
        // Apply filters if provided
        if (line != null && !line.equals("all")) {
            data = data.stream()
                .filter(item -> line.equals(item.getProductionLine()))
                .collect(Collectors.toList());
        }
        
        if (shift != null && !shift.equals("all")) {
            data = filterByShift(data, shift);
        }
        
        return ResponseEntity.ok(data);
    }
    
    private List<ProductionData> filterByShift(List<ProductionData> data, String shift) {
        // Use production window from timeService for "current" shift
        if (shift == null || shift.equals("current")) {
            int startHour = timeService.getStartTime().getHour();
            int endHour = timeService.getEndTime().getHour();
            
            return data.stream()
                .filter(item -> {
                    int hour = item.getCompletionTime().getHour();
                    return hour >= startHour && hour < endHour;
                })
                .collect(Collectors.toList());
        }
        
        // For specific shifts, maintain compatibility with existing shift logic
        int shiftStartHour, shiftEndHour;
        switch (shift) {
            case "A": shiftStartHour = 6; shiftEndHour = 14; break;
            case "B": shiftStartHour = 14; shiftEndHour = 22; break;
            case "C": shiftStartHour = 22; shiftEndHour = 6; break;
            default: return data;
        }
        
        return data.stream()
            .filter(item -> {
                int hour = item.getCompletionTime().getHour();
                if (shift.equals("C")) {
                    return hour >= 22 || hour < 6;
                }
                return hour >= shiftStartHour && hour < shiftEndHour;
            })
            .collect(Collectors.toList());
    }
    
    private int calculateEfficiency(int actual, int target) {
        if (target == 0) return 0;
        return (int) Math.min(100, Math.round((actual * 100.0) / target));
    }
    
    private int calculateDowntime(List<ProductionData> dayData) {
        // Improved downtime calculation based on production window
        if (dayData.isEmpty()) return 0;
        
        // Use actual production window duration
        long totalMinutes = timeService.getTotalProductionMinutes();
        
        // Calculate actual production time from cycle times
        long productionMinutes = dayData.stream()
            .filter(item -> item.getCycleTime() != null)
            .mapToLong(item -> (long) item.getCycleTime().doubleValue())
            .sum();
        
        // If no cycle times, estimate based on quantity
        if (productionMinutes == 0) {
            productionMinutes = dayData.stream()
                .mapToInt(ProductionData::getQuantity)
                .sum() * 5; // 5 minutes per unit as fallback
        }
        
        return Math.max(0, (int) (totalMinutes - productionMinutes));
    }
}