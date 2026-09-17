package com.peps.production.service;

import com.peps.production.dto.*;
import com.peps.production.model.*;
import com.peps.production.repository.ProductionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    
    private final ProductionRepository repository;
    private final ProductionTimeService timeService;
    private final ProductionSimulationService simulationService;
    
    @Value("${production.daily-target:500}")
    private int dailyTarget;
    
    public DashboardService(ProductionRepository repository, 
                           ProductionTimeService timeService,
                           ProductionSimulationService simulationService) {
        this.repository = repository;
        this.timeService = timeService;
        this.simulationService = simulationService;
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
        
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        
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
        
        for (ProductionData item : today) {
            int quantity = item.getQuantity();
            boolean isSpring = item.getProductType() == ProductType.SPRING;
            
            if (isSpring) {
                spring += quantity;
            } else {
                hypnos += quantity;
            }
            
            SizeBreakdown size = breakdown.get(item.getSize().name().toLowerCase());
            if (isSpring) {
                size.addSpring(quantity);
            } else {
                size.addHypnos(quantity);
            }
            
            // Hourly data - only include hours that have passed
            int hourIndex = item.getCompletionTime().getHour();
            if (hourIndex >= 0 && hourIndex < 24) {
                if (isSpring) {
                    hourly.getSpring()[hourIndex] += quantity;
                } else {
                    hourly.getHypnos()[hourIndex] += quantity;
                }
            }
        }
        
        response.setSpringCount(spring);
        response.setHypnosCount(hypnos);
        response.setTotalProduction(spring + hypnos);
        
        // Calculate efficiency based on actual production vs expected for elapsed time
        int expectedProduction = timeService.calculateExpectedProduction(now);
        int efficiency = calculateEfficiency(spring + hypnos, expectedProduction);
        response.setEfficiency(efficiency);
        
        response.setSizeBreakdown(breakdown);
        response.setHourlyData(hourly);
        
        // Recent production items
        response.setRecentItems(repository.findTop10ByStatusOrderByCompletionTimeDesc(ProductionStatus.COMPLETED).stream()
            .map(item -> new RecentProduction(
                item.getProductType().name(),
                item.getVariety() != null ? item.getVariety() : "Standard",
                item.getSize().name(),
                item.getCompletionTime(),
                item.getQuantity(),
                item.getCycleTime() != null ? item.getCycleTime() : 0.0,
                item.getProductionLine() != null ? item.getProductionLine() : "Line 1",
                item.getStatus().name()
            )).toList());
        
        return response;
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
