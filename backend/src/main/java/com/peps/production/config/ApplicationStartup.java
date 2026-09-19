package com.peps.production.config;

import com.peps.production.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Application startup handler
 * Initializes default settings and starts simulation services
 */
@Component
public class ApplicationStartup {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartup.class);
    
    private final SettingsService settingsService;
    private final HistoricalSeedingService historicalSeedingService;
    private final ContinuousSimulationService continuousSimulationService;
    
    public ApplicationStartup(SettingsService settingsService,
                            HistoricalSeedingService historicalSeedingService,
                            ContinuousSimulationService continuousSimulationService) {
        this.settingsService = settingsService;
        this.historicalSeedingService = historicalSeedingService;
        this.continuousSimulationService = continuousSimulationService;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Application startup initialization began");
        
        try {
            // Initialize default settings if they don't exist
            settingsService.initializeDefaultSettings();
            logger.info("Default settings initialized");
            
            // Seed historical data if needed
            if (historicalSeedingService.needsSeeding()) {
                logger.info("Historical data seeding started");
                historicalSeedingService.seedHistoricalData();
                logger.info("Historical data seeding completed");
            }
            
            // Start continuous simulation
            continuousSimulationService.startSimulation();
            logger.info("Continuous simulation started");
            
            logger.info("Application startup initialization completed successfully");
        } catch (Exception e) {
            logger.error("Error during application startup initialization", e);
        }
    }
}