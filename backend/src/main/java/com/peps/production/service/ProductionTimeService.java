package com.peps.production.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.*;

@Service
public class ProductionTimeService {
    @Value("${production.start-time:09:00}")
    private String productionStartTime;
    
    @Value("${production.end-time:17:00}")
    private String productionEndTime;
    
    @Value("${production.daily-target:500}")
    private int dailyTarget;
    
    @Value("${production.rate:65}")
    private int productionRate;
    
    private LocalTime startTime;
    private LocalTime endTime;
    
    @PostConstruct
    public void init() {
        // Initialize times from configuration after dependency injection
        this.startTime = LocalTime.parse(productionStartTime);
        this.endTime = LocalTime.parse(productionEndTime);
    }
    
    /**
     * Get the current production window start time for today
     */
    public LocalDateTime getProductionWindowStart(LocalDate date) {
        return LocalDateTime.of(date, startTime);
    }
    
    /**
     * Get the current production window end time for today
     */
    public LocalDateTime getProductionWindowEnd(LocalDate date) {
        return LocalDateTime.of(date, endTime);
    }
    
    /**
     * Check if current time is within production window
     */
    public boolean isWithinProductionWindow(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }
    
    /**
     * Check if current time is before production start
     */
    public boolean isBeforeProductionStart(LocalDateTime dateTime) {
        return dateTime.toLocalTime().isBefore(startTime);
    }
    
    /**
     * Check if current time is after production end
     */
    public boolean isAfterProductionEnd(LocalDateTime dateTime) {
        return dateTime.toLocalTime().isAfter(endTime);
    }
    
    /**
     * Calculate elapsed production minutes in the current window
     */
    public long getElapsedProductionMinutes(LocalDateTime currentTime) {
        LocalDate today = currentTime.toLocalDate();
        LocalDateTime windowStart = getProductionWindowStart(today);
        LocalDateTime windowEnd = getProductionWindowEnd(today);
        
        if (currentTime.isBefore(windowStart)) {
            return 0;
        }
        
        if (currentTime.isAfter(windowEnd)) {
            return Duration.between(windowStart, windowEnd).toMinutes();
        }
        
        return Duration.between(windowStart, currentTime).toMinutes();
    }
    
    /**
     * Calculate total production minutes in the window
     */
    public long getTotalProductionMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }
    
    /**
     * Calculate expected production based on elapsed time
     * Uses a linear model with small controlled variation
     */
    public int calculateExpectedProduction(LocalDateTime currentTime) {
        LocalDate today = currentTime.toLocalDate();
        LocalDateTime windowStart = getProductionWindowStart(today);
        LocalDateTime windowEnd = getProductionWindowEnd(today);
        
        // Before production window
        if (currentTime.isBefore(windowStart)) {
            return 0;
        }
        
        // After production window
        if (currentTime.isAfter(windowEnd)) {
            return dailyTarget;
        }
        
        // Within production window - calculate based on elapsed time
        long elapsedMinutes = getElapsedProductionMinutes(currentTime);
        long totalMinutes = getTotalProductionMinutes();
        
        if (totalMinutes == 0) {
            return 0;
        }
        
        // Base linear calculation
        double baseProduction = (double) elapsedMinutes / totalMinutes * dailyTarget;
        
        // Add small deterministic variation based on time
        // This ensures same time always produces same result
        int timeBasedVariation = (int) (currentTime.getMinute() * 0.1 + currentTime.getSecond() * 0.01);
        
        int expectedProduction = (int) baseProduction + timeBasedVariation;
        
        // Ensure production doesn't exceed daily target
        return Math.min(expectedProduction, dailyTarget);
    }
    
    /**
     * Get the daily production target
     */
    public int getDailyTarget() {
        return dailyTarget;
    }
    
    /**
     * Get the production rate (units per hour)
     */
    public int getProductionRate() {
        return productionRate;
    }
    
    /**
     * Get production window start time
     */
    public LocalTime getStartTime() {
        return startTime;
    }
    
    /**
     * Get production window end time
     */
    public LocalTime getEndTime() {
        return endTime;
    }
}
