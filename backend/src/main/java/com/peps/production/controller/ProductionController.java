package com.peps.production.controller;

import com.peps.production.dto.*;
import com.peps.production.model.ProductionData;
import com.peps.production.model.ProductionStatus;
import com.peps.production.repository.ProductionRepository;
import com.peps.production.service.DashboardService;
import com.peps.production.service.ProductionSimulatorService;
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
    
    public ProductionController(ProductionRepository repository, 
                               DashboardService dashboardService,
                               ProductionSimulatorService simulatorService) {
        this.repository = repository;
        this.dashboardService = dashboardService;
        this.simulatorService = simulatorService;
    }
    
    @GetMapping("/hourly")
    public ResponseEntity<HourlyProduction> getHourlyProduction() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        List<ProductionData> today = repository.findByStatusAndCompletionTimeBetween(
            ProductionStatus.COMPLETED, dayStart, dayStart.plusDays(1));
        
        HourlyProduction hourly = new HourlyProduction();
        for (ProductionData item : today) {
            int hourIndex = item.getCompletionTime().getHour() - 6; // Start from 6 AM
            if (hourIndex >= 0 && hourIndex < 24) {
                boolean isSpring = item.getProductType().name().equals("SPRING");
                if (isSpring) {
                    hourly.getSpring()[hourIndex] += item.getQuantity();
                } else {
                    hourly.getHypnos()[hourIndex] += item.getQuantity();
                }
            }
        }
        return ResponseEntity.ok(hourly);
    }
    
    @GetMapping("/daily")
    public ResponseEntity<List<DailyProduction>> getDailyProduction() {
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
            daily.setEfficiency(calculateEfficiency(spring + hypnos, 960)); // Assuming 960 daily target
            daily.setDowntime(calculateDowntime(dayData));
            
            dailyData.add(daily);
        }
        
        return ResponseEntity.ok(dailyData);
    }
    
    @GetMapping("/weekly")
    public ResponseEntity<List<WeeklyProduction>> getWeeklyProduction() {
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
            weekly.setTarget(4800); // Weekly target
            weekly.setEfficiency(calculateEfficiency(spring + hypnos, 4800));
            
            weeklyData.add(weekly);
        }
        
        return ResponseEntity.ok(weeklyData);
    }
    
    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyProduction>> getMonthlyProduction() {
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
            monthly.setTarget(20000); // Monthly target
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
        ProductionStatusResponse status = new ProductionStatusResponse();
        status.setConnectionStatus("Connected");
        status.setDataSource("Simulated HMI/PLC");
        status.setLastUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        status.setSimulatorActive(true);
        status.setTotalRecords((int) repository.count());
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<ProductionData>> getProductionHistory(
            @RequestParam(required = false) String line,
            @RequestParam(required = false) String shift,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
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
        
        if (shift != null && !shift.equals("current")) {
            data = filterByShift(data, shift);
        }
        
        return ResponseEntity.ok(data);
    }
    
    private List<ProductionData> filterByShift(List<ProductionData> data, String shift) {
        int startHour, endHour;
        switch (shift) {
            case "A": startHour = 6; endHour = 14; break;
            case "B": startHour = 14; endHour = 22; break;
            case "C": startHour = 22; endHour = 6; break;
            default: return data;
        }
        
        return data.stream()
            .filter(item -> {
                int hour = item.getCompletionTime().getHour();
                if (shift.equals("C")) {
                    return hour >= 22 || hour < 6;
                }
                return hour >= startHour && hour < endHour;
            })
            .collect(Collectors.toList());
    }
    
    private int calculateEfficiency(int actual, int target) {
        if (target == 0) return 0;
        return (int) Math.min(100, Math.round((actual * 100.0) / target));
    }
    
    private int calculateDowntime(List<ProductionData> dayData) {
        // Simplified downtime calculation
        // In a real system, this would be calculated from actual downtime records
        if (dayData.isEmpty()) return 0;
        
        int totalMinutes = 8 * 60; // 8-hour shift in minutes
        int productionMinutes = dayData.size() * 5; // Assuming 5 minutes per unit
        return Math.max(0, totalMinutes - productionMinutes);
    }
}