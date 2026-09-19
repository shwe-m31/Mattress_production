package com.peps.production.config;

import com.peps.production.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;
    
    public ApplicationStartup(SettingsService settingsService,
                            HistoricalSeedingService historicalSeedingService,
                            ContinuousSimulationService continuousSimulationService,
                            JdbcTemplate jdbcTemplate) {
        this.settingsService = settingsService;
        this.historicalSeedingService = historicalSeedingService;
        this.continuousSimulationService = continuousSimulationService;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Application startup initialization began");
        
        try {
            // Backfill legacy records if necessary
            backfillLegacyData();

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

    private void backfillLegacyData() {
        try {
            int updatedDates = jdbcTemplate.update(
                "UPDATE production_data SET production_date = CAST(completion_time AS DATE) WHERE production_date IS NULL AND completion_time IS NOT NULL"
            );
            if (updatedDates > 0) {
                logger.info("Backfilled production_date for {} legacy records", updatedDates);
            }
            int updatedStarts = jdbcTemplate.update(
                "UPDATE production_data SET start_time = completion_time WHERE start_time IS NULL AND completion_time IS NOT NULL"
            );
            if (updatedStarts > 0) {
                logger.info("Backfilled start_time for {} legacy records", updatedStarts);
            }
        } catch (Exception e) {
            logger.warn("Note: Legacy backfill skipped or completed: {}", e.getMessage());
        }
    }
}