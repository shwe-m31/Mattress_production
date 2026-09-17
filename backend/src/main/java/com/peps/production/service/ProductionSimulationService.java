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

@Service
public class ProductionSimulationService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionSimulationService.class);
    
    private final ProductionRepository repository;
    private final ProductionTimeService timeService;
    
    // Deterministic random for consistent product distribution
    private final Random deterministicRandom = new Random(42); // Fixed seed for consistency
    
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
    
    public ProductionSimulationService(ProductionRepository repository, 
                                       ProductionTimeService timeService) {
        this.repository = repository;
        this.timeService = timeService;
    }
    
    /**
     * Synchronize production with expected state based on current time
     * This method is idempotent - calling it multiple times produces the same result
     */
    @Transactional
    public void synchronizeProduction() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(23, 59, 59);
        
        // Get current actual production from database
        List<ProductionData> todayProduction = repository.findByStatusAndCompletionTimeBetween(
            ProductionStatus.COMPLETED, dayStart, dayEnd);
        
        int actualProduction = todayProduction.stream()
            .mapToInt(ProductionData::getQuantity)
            .sum();
        
        // Calculate expected production based on time
        int expectedProduction = timeService.calculateExpectedProduction(now);
        
        logger.info("Production sync - Actual: {}, Expected: {}, Time: {}", 
            actualProduction, expectedProduction, now);
        
        // Only generate events if we need more production
        if (actualProduction < expectedProduction) {
            int neededProduction = expectedProduction - actualProduction;
            generateMissingProductionEvents(today, now, neededProduction, todayProduction);
        }
        
        // If actual exceeds expected (shouldn't happen normally), log warning
        if (actualProduction > expectedProduction) {
            logger.warn("Actual production ({}) exceeds expected ({}) at time {}", 
                actualProduction, expectedProduction, now);
        }
    }
    
    /**
     * Generate missing production events to reach expected production
     */
    private void generateMissingProductionEvents(LocalDate date, LocalDateTime currentTime, 
                                                 int neededQuantity, List<ProductionData> existingEvents) {
        LocalDateTime windowStart = timeService.getProductionWindowStart(date);
        LocalDateTime windowEnd = timeService.getProductionWindowEnd(date);
        
        // If we're before production window, don't generate anything
        if (currentTime.isBefore(windowStart)) {
            return;
        }
        
        // Determine the time range for new events
        LocalDateTime lastEventTime = existingEvents.isEmpty() ? windowStart : 
            existingEvents.stream()
                .map(ProductionData::getCompletionTime)
                .max(LocalDateTime::compareTo)
                .orElse(windowStart);
        
        // Ensure last event time is at least window start
        lastEventTime = lastEventTime.isBefore(windowStart) ? windowStart : lastEventTime;
        
        // Don't create events in the future
        LocalDateTime maxEventTime = currentTime.isAfter(windowEnd) ? windowEnd : currentTime;
        
        // Generate events to fill the gap
        int remainingQuantity = neededQuantity;
        LocalDateTime eventTime = lastEventTime;
        
        // Use deterministic seeding based on date to ensure consistency
        long dateSeed = date.toEpochDay();
        Random dayRandom = new Random(dateSeed);
        
        while (remainingQuantity > 0 && eventTime.isBefore(maxEventTime)) {
            // Calculate quantity for this event (1-2 units)
            int eventQuantity = Math.min(remainingQuantity, 1 + dayRandom.nextInt(2));
            
            // Advance time by cycle time
            double cycleTime = MIN_CYCLE_TIME + dayRandom.nextDouble() * (MAX_CYCLE_TIME - MIN_CYCLE_TIME);
            eventTime = eventTime.plusMinutes((long) cycleTime);
            
            // Don't create events in the future
            if (eventTime.isAfter(maxEventTime)) {
                break;
            }
            
            // Generate and save the production event
            ProductionData event = generateDeterministicEvent(eventTime, eventQuantity, dayRandom);
            repository.save(event);
            
            remainingQuantity -= eventQuantity;
            
            logger.debug("Generated production event: {} units at {}", eventQuantity, eventTime);
        }
        
        logger.info("Generated {} production units to reach expected total", neededQuantity - remainingQuantity);
    }
    
    /**
     * Generate a deterministic production event
     * Uses the random generator to ensure consistent distribution
     */
    private ProductionData generateDeterministicEvent(LocalDateTime completionTime, int quantity, Random random) {
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
        
        // Create the production event
        ProductionData event = new ProductionData(
            productType,
            variety,
            size,
            quantity,
            productionLine,
            startTime,
            completionTime,
            cycleTime,
            ProductionStatus.COMPLETED
        );
        
        event.setProductionTime(completionTime);
        
        return event;
    }
    
    /**
     * Get current production count for today
     */
    public int getCurrentProductionCount() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(23, 59, 59);
        
        List<ProductionData> todayProduction = repository.findByStatusAndCompletionTimeBetween(
            ProductionStatus.COMPLETED, dayStart, dayEnd);
        
        return todayProduction.stream()
            .mapToInt(ProductionData::getQuantity)
            .sum();
    }
    
    /**
     * Check if production synchronization is needed
     */
    public boolean needsSynchronization() {
        LocalDateTime now = LocalDateTime.now();
        int actualProduction = getCurrentProductionCount();
        int expectedProduction = timeService.calculateExpectedProduction(now);
        
        return actualProduction < expectedProduction;
    }
}
