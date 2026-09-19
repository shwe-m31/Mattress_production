package com.peps.production.service;

import com.peps.production.model.*;
import com.peps.production.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Continuous Simulation Service
 * Runs in the background to continuously generate production events
 * Simulates a realistic production flow with gradual mattress completion
 */
@Service
public class ContinuousSimulationService {
    private static final Logger logger = LoggerFactory.getLogger(ContinuousSimulationService.class);
    
    private final ProductionRepository productionRepository;
    private final ProductionTimeService timeService;
    private final SettingsService settingsService;
    private final DowntimeSimulationService downtimeSimulationService;
    private final AlertService alertService;
    private final com.peps.production.repository.ProductionSettingsRepository productionSettingsRepository;
    
    // Simulation state
    private boolean isRunning = false;
    private LocalDateTime lastSimulationTime;
    private boolean inDowntime = false;
    private LocalDateTime downtimeStartTime;
    
    // Product distribution (should match configured targets)
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
    
    public ContinuousSimulationService(ProductionRepository productionRepository,
                                      ProductionTimeService timeService,
                                      SettingsService settingsService,
                                      DowntimeSimulationService downtimeSimulationService,
                                      AlertService alertService,
                                      com.peps.production.repository.ProductionSettingsRepository productionSettingsRepository) {
        this.productionRepository = productionRepository;
        this.timeService = timeService;
        this.settingsService = settingsService;
        this.downtimeSimulationService = downtimeSimulationService;
        this.alertService = alertService;
        this.productionSettingsRepository = productionSettingsRepository;
    }
    
    /**
     * Main simulation loop - runs every minute
     * Generates production events based on configured targets and current time
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void simulateProductionStep() {
        if (!isSimulationEnabled()) {
            return;
        }
        
        LocalDateTime now = timeService.getCurrentTime();
        
        // Check if we're in production window
        if (!timeService.isWithinProductionWindow(now)) {
            logger.debug("Outside production window, skipping simulation");
            return;
        }
        
        // Check if we're in downtime
        if (inDowntime) {
            handleDowntime(now);
            return;
        }
        
        // Check if we should start downtime
        if (downtimeSimulationService.shouldSimulateDowntime(now)) {
            startDowntime(now);
            return;
        }
        
        // Generate production events
        generateProductionEvents(now);
        
        // Check for low production alerts
        checkProductionAlerts(now);
        
        lastSimulationTime = now;
    }
    
    /**
     * Generate production events for the current time step
     * Distributes production across the hour based on configured targets
     */
    private void generateProductionEvents(LocalDateTime now) {
        int totalHourlyTarget = settingsService.calculateTotalHourlyTarget();
        
        if (totalHourlyTarget == 0) {
            logger.debug("No hourly target configured, skipping production");
            return;
        }
        
        // Calculate how many events we should generate this minute
        // Target per minute = hourly target / 60
        double eventsPerMinute = totalHourlyTarget / 60.0;
        int eventsToGenerate = (int) Math.round(eventsPerMinute);
        
        // Add some randomness to make it realistic
        Random random = new Random(now.toEpochSecond());
        if (random.nextDouble() < (eventsPerMinute - eventsToGenerate)) {
            eventsToGenerate++;
        }
        
        // Generate the events
        String currentShift = determineCurrentShift(now);
        for (int i = 0; i < eventsToGenerate; i++) {
            generateSingleProductionEvent(now, currentShift, random);
        }
        
        if (eventsToGenerate > 0) {
            logger.debug("Generated {} production events at {}", eventsToGenerate, now);
        }
    }
    
    /**
     * Generate a single production event (one mattress)
     */
    private void generateSingleProductionEvent(LocalDateTime completionTime, String shift, Random random) {
        // Determine product type based on configured targets if available
        ProductType productType = determineProductType(random);
        
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
        
        // Select size based on configured targets
        MattressSize size = determineSize(productType, random);
        
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
        productionRepository.save(event);
    }
    
    /**
     * Determine product type based on configured targets
     */
    private ProductType determineProductType(Random random) {
        // For now, use the fixed ratio (can be enhanced to use actual configured targets)
        return random.nextDouble() < SPRING_RATIO ? ProductType.SPRING : ProductType.HYPNOS;
    }
    
    /**
     * Determine mattress size based on configured targets
     */
    private MattressSize determineSize(ProductType productType, Random random) {
        // For now, use random distribution (can be enhanced to use actual configured targets)
        MattressSize[] sizes = MattressSize.values();
        return sizes[random.nextInt(sizes.length)];
    }
    
    /**
     * Start a downtime period
     */
    private void startDowntime(LocalDateTime now) {
        int duration = downtimeSimulationService.getRandomDowntimeDuration(now);
        downtimeSimulationService.simulateDowntime(now, duration);
        
        inDowntime = true;
        downtimeStartTime = now;
        
        logger.info("Starting downtime period: {} minutes", duration);
    }
    
    /**
     * Handle ongoing downtime
     */
    private void handleDowntime(LocalDateTime now) {
        int elapsedMinutes = (int) java.time.Duration.between(downtimeStartTime, now).toMinutes();
        
        // Assume downtime lasts between 5-30 minutes
        // For simplicity, we'll end downtime after 15 minutes
        if (elapsedMinutes >= 15) {
            inDowntime = false;
            downtimeStartTime = null;
            logger.info("Downtime period ended");
        }
    }
    
    /**
     * Check for low production alerts
     */
    private void checkProductionAlerts(LocalDateTime now) {
        LocalDateTime dayStart = timeService.getCurrentDate().atStartOfDay();
        
        // Get actual production count
        long actualProduction = productionRepository.countByStatusAndCompletionTimeBetween(
            ProductionStatus.COMPLETED, dayStart, now);
        
        // Calculate expected production based on elapsed time
        int expectedProduction = timeService.calculateExpectedProduction(now);
        
        // Check alert condition
        alertService.checkLowProductionAlert((int) actualProduction, expectedProduction);
    }
    
    /**
     * Determine current shift based on time
     */
    private String determineCurrentShift(LocalDateTime dateTime) {
        java.util.Optional<ShiftConfiguration> currentShift = settingsService.getCurrentShift();
        return currentShift.map(shift -> shift.getShiftName()).orElse("Unknown");
    }
    
    /**
     * Check if simulation is enabled
     */
    private boolean isSimulationEnabled() {
        try {
            java.util.Optional<com.peps.production.model.ProductionSettings> settingsOpt = 
                productionSettingsRepository.findBySettingsKey("DEFAULT");
            if (settingsOpt.isPresent()) {
                com.peps.production.model.ProductionSettings settings = settingsOpt.get();
                return "SIMULATED".equals(settings.getPreferredMode());
            }
            return true; // Default to enabled if no settings exist
        } catch (Exception e) {
            logger.error("Error checking simulation enabled status", e);
            return true; // Default to enabled if there's an error
        }
    }
    
    /**
     * Start the continuous simulation
     */
    public void startSimulation() {
        isRunning = true;
        lastSimulationTime = timeService.getCurrentTime();
        logger.info("Continuous simulation started");
    }
    
    /**
     * Stop the continuous simulation
     */
    public void stopSimulation() {
        isRunning = false;
        logger.info("Continuous simulation stopped");
    }
    
    /**
     * Get simulation status
     */
    public boolean isRunning() {
        return isRunning;
    }
}