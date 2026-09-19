package com.peps.production.service;

import com.peps.production.model.*;
import com.peps.production.repository.ProductionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * Historical Seeding Service
 * Generates simulated historical production data for demonstration purposes
 * Only runs when database is empty to avoid duplicating data
 */
@Service
public class HistoricalSeedingService {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalSeedingService.class);
    
    private final ProductionRepository repository;
    private final ProductionTimeService timeService;
    private final SimulationClockService simulationClockService;
    
    @Value("${simulation.history-days:30}")
    private int historyDays;
    
    @Value("${simulation.enabled:false}")
    private boolean simulationEnabled;
    
    // Product type distribution (58% Spring, 42% Hypnos)
    private static final double SPRING_RATIO = 0.58;
    
    // Product varieties
    private static final String[] SPRING_VARIETIES = {"Bonnell", "Pocket", "Offset", "Continuous"};
    private static final String[] HYPNOS_VARIETIES = {"Comfort", "Ortho", "Pillow Top", "Euro Top"};
    
    // Production lines
    private static final String[] SPRING_LINES = {"SPRING-01", "SPRING-02"};
    private static final String[] HYPNOS_LINES = {"HYPNOS-01", "HYPNOS-02"};
    
    // Cycle time range in minutes
    private static final double MIN_CYCLE_TIME = 3.5;
    private static final double MAX_CYCLE_TIME = 5.5;
    
    public HistoricalSeedingService(ProductionRepository repository,
                                   ProductionTimeService timeService,
                                   SimulationClockService simulationClockService) {
        this.repository = repository;
        this.timeService = timeService;
        this.simulationClockService = simulationClockService;
    }
    
    /**
     * Check if historical seeding is needed
     * Returns true if database is empty and simulation is enabled
     */
    public boolean needsSeeding() {
        if (!simulationEnabled) {
            logger.info("Simulation is disabled, skipping historical seeding");
            return false;
        }
        
        long count = repository.count();
        if (count > 0) {
            logger.info("Database already contains {} production records, skipping historical seeding", count);
            return false;
        }
        
        logger.info("Database is empty, historical seeding needed");
        return true;
    }
    
    /**
     * Check if specific date range already has data
     * Prevents duplicate seeding for specific periods
     */
    public boolean hasDataForDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        return repository.existsByCompletionTimeBetween(start, end);
    }
    
    /**
     * Seed historical production data
     * Generates production events for the configured number of days before today
     */
    @Transactional
    public void seedHistoricalData() {
        if (!needsSeeding()) {
            return;
        }
        
        logger.info("Starting historical data seeding for {} days", historyDays);
        
        LocalDate today = timeService.getCurrentDate();
        LocalDate startDate = today.minusDays(historyDays);
        
        // Double-check that the specific date range doesn't have data
        if (hasDataForDateRange(startDate, today.minusDays(1))) {
            logger.warn("Historical date range already contains data, skipping seeding");
            return;
        }
        
        int totalEventsGenerated = 0;
        
        // Generate data for each historical day (excluding today)
        for (LocalDate date = startDate; date.isBefore(today); date = date.plusDays(1)) {
            int dayEvents = generateHistoricalDay(date);
            totalEventsGenerated += dayEvents;
            logger.debug("Generated {} events for {}", dayEvents, date);
        }
        
        logger.info("Historical seeding completed. Total events generated: {}", totalEventsGenerated);
    }
    
    /**
     * Generate production events for a single historical day
     * Now generates single mattress events (one event = one mattress)
     */
    private int generateHistoricalDay(LocalDate date) {
        LocalDateTime windowStart = timeService.getProductionWindowStart(date);
        LocalDateTime windowEnd = timeService.getProductionWindowEnd(date);
        
        // Use deterministic random for consistent results
        Random dayRandom = new Random(date.toEpochDay());
        
        // Calculate target production for this day with some variation
        int dailyTarget = timeService.getDailyTarget();
        int dayVariation = (int) (dayRandom.nextGaussian() * dailyTarget * 0.1); // 10% variation
        int targetProduction = Math.max(0, dailyTarget + dayVariation);
        
        // Adjust for weekends (lower production)
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            targetProduction = (int) (targetProduction * 0.7); // 30% less on weekends
        }
        
        int generatedEvents = 0;
        LocalDateTime currentTime = windowStart;
        
        // Determine shift for historical data
        String historicalShift = determineHistoricalShift(date);
        
        // Generate events throughout the production window (one event = one mattress)
        while (generatedEvents < targetProduction && currentTime.isBefore(windowEnd)) {
            // Calculate cycle time
            double cycleTime = MIN_CYCLE_TIME + dayRandom.nextDouble() * (MAX_CYCLE_TIME - MIN_CYCLE_TIME);
            
            // Advance time
            currentTime = currentTime.plusMinutes((long) cycleTime);
            
            // Don't create events outside production window
            if (currentTime.isAfter(windowEnd)) {
                break;
            }
            
            // Generate and save the production event (quantity = 1 for single mattress)
            ProductionData event = generateHistoricalEvent(currentTime, 1, dayRandom, historicalShift);
            repository.save(event);
            
            generatedEvents++;
        }
        
        return generatedEvents;
    }
    
    /**
     * Determine shift for historical data
     */
    private String determineHistoricalShift(LocalDate date) {
        // Simple determination based on date
        int dayOfMonth = date.getDayOfMonth();
        if (dayOfMonth % 3 == 0) {
            return "Morning Shift";
        } else if (dayOfMonth % 3 == 1) {
            return "Evening Shift";
        } else {
            return "Night Shift";
        }
    }
    
    /**
     * Generate a single historical production event
     * Now generates single mattress events (quantity = 1)
     */
    private ProductionData generateHistoricalEvent(LocalDateTime completionTime, int quantity, Random random, String shift) {
        // Determine product type (58% Spring, 42% Hypnos)
        ProductType productType = random.nextDouble() < SPRING_RATIO ? ProductType.SPRING : ProductType.HYPNOS;
        
        // Select variety based on product type
        String variety;
        String[] varieties;
        String[] lines;
        
        if (productType == ProductType.SPRING) {
            varieties = SPRING_VARIETIES;
            lines = SPRING_LINES;
        } else {
            varieties = HYPNOS_VARIETIES;
            lines = HYPNOS_LINES;
        }
        
        variety = varieties[random.nextInt(varieties.length)];
        String productionLine = lines[random.nextInt(lines.length)];
        
        // Select size
        MattressSize[] sizes = MattressSize.values();
        MattressSize size = sizes[random.nextInt(sizes.length)];
        
        // Calculate start time and cycle time
        double cycleTime = MIN_CYCLE_TIME + random.nextDouble() * (MAX_CYCLE_TIME - MIN_CYCLE_TIME);
        LocalDateTime startTime = completionTime.minusMinutes((long) cycleTime);
        
        // Create the production event (one event = one mattress)
        ProductionData event = new ProductionData(
            productType,
            variety,
            size,
            productionLine,
            startTime,
            completionTime,
            cycleTime,
            ProductionStatus.COMPLETED,
            "SIMULATOR",
            shift,
            "SIMULATED"
        );
        
        event.setProductionTime(completionTime);
        
        return event;
    }
    
    /**
     * Get the number of historical days configured
     */
    public int getHistoryDays() {
        return historyDays;
    }
    
    /**
     * Check if simulation is enabled
     */
    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }
}