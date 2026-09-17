package com.peps.production.service;

import com.peps.production.model.*;
import com.peps.production.repository.ProductionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProductionSimulatorService implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ProductionSimulatorService.class);
    
    private final ProductionRepository repository;
    private final ProductionTimeService timeService;
    private final ProductionSimulationService simulationService;
    
    public ProductionSimulatorService(ProductionRepository repository, 
                                      ProductionTimeService timeService,
                                      ProductionSimulationService simulationService) {
        this.repository = repository;
        this.timeService = timeService;
        this.simulationService = simulationService;
    }

    @Override
    public void run(String... args) {
        logger.info("Production Simulator Service starting...");
        
        // On startup, synchronize production with expected state
        // This ensures restart persistence and proper initialization
        try {
            simulationService.synchronizeProduction();
            logger.info("Initial production synchronization completed");
        } catch (Exception e) {
            logger.error("Error during initial production synchronization", e);
        }
    }

    /**
     * Scheduled synchronization instead of random event generation
     * This ensures production stays synchronized with expected time-based progression
     */
    @Scheduled(fixedDelayString = "${simulation.sync-interval:60000}", initialDelayString = "10000")
    public void synchronizeProduction() {
        try {
            if (simulationService.needsSynchronization()) {
                simulationService.synchronizeProduction();
                logger.debug("Production synchronization completed");
            }
        } catch (Exception e) {
            logger.error("Error during production synchronization", e);
        }
    }
    
    /**
     * Manual simulation endpoint for testing purposes
     * This allows manual triggering of production events for testing
     * Only creates events within valid time constraints
     */
    public ProductionData simulateManualProductionEvent(ProductType productType, String variety, 
                                                         MattressSize size, int quantity, 
                                                         String productionLine) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        // Check if within production window
        if (!timeService.isWithinProductionWindow(now)) {
            throw new IllegalStateException("Cannot create production events outside production window");
        }
        
        // Calculate realistic cycle time
        double cycleTime = 3.5 + ThreadLocalRandom.current().nextDouble() * 2.0; // 3.5-5.5 minutes
        LocalDateTime startTime = now.minusMinutes((long) cycleTime);
        
        // Ensure start time is within production window
        LocalDateTime windowStart = timeService.getProductionWindowStart(today);
        if (startTime.isBefore(windowStart)) {
            startTime = windowStart;
        }
        
        ProductionData event = new ProductionData(
            productType,
            variety,
            size,
            quantity,
            productionLine,
            startTime,
            now,
            cycleTime,
            ProductionStatus.COMPLETED
        );
        
        event.setProductionTime(now);
        
        ProductionData saved = repository.save(event);
        logger.info("Manual production event created: {} units of {} {}", 
            quantity, productType, size);
        
        return saved;
    }
}
