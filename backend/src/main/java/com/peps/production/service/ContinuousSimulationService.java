package com.peps.production.service;

import com.peps.production.acquisition.DataAcquisitionService;
import com.peps.production.acquisition.ProductionEvent;
import com.peps.production.acquisition.SimulatedProductionDataSource;
import com.peps.production.model.ProductionData;
import com.peps.production.model.ProductionSettings;
import com.peps.production.model.ProductionStatus;
import com.peps.production.model.ShiftConfiguration;
import com.peps.production.repository.ProductionRepository;
import com.peps.production.repository.ProductionSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Continuous Simulation Service
 * Runs in the background and drives simulated production events
 * through the Data Acquisition Layer into MySQL.
 */
@Service
public class ContinuousSimulationService {
    private static final Logger logger = LoggerFactory.getLogger(ContinuousSimulationService.class);
    
    private final SimulatedProductionDataSource simulatedDataSource;
    private final DataAcquisitionService dataAcquisitionService;
    private final ProductionSettingsRepository settingsRepository;
    private final ProductionRepository productionRepository;
    private final SettingsService settingsService;
    private final AlertService alertService;
    
    private boolean isRunning = false;
    
    public ContinuousSimulationService(SimulatedProductionDataSource simulatedDataSource,
                                       DataAcquisitionService dataAcquisitionService,
                                       ProductionSettingsRepository settingsRepository,
                                       ProductionRepository productionRepository,
                                       SettingsService settingsService,
                                       AlertService alertService) {
        this.simulatedDataSource = simulatedDataSource;
        this.dataAcquisitionService = dataAcquisitionService;
        this.settingsRepository = settingsRepository;
        this.productionRepository = productionRepository;
        this.settingsService = settingsService;
        this.alertService = alertService;
    }
    
    /**
     * Main background simulator ticker — runs every 60 seconds.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 5000)
    public void executeSimulationCycle() {
        if (!isSimulatedMode()) {
            return;
        }
        
        try {
            // Fetch simulated events for this time step
            List<ProductionEvent> events = simulatedDataSource.fetchNextProductionEvents();
            if (!events.isEmpty()) {
                List<ProductionData> persisted = dataAcquisitionService.ingestEvents(events);
                logger.info("Simulation tick generated & ingested {} production events", persisted.size());
            }
            
            // Check for low production alerts
            checkAlertStatus();
        } catch (Exception e) {
            logger.error("Error during simulation cycle", e);
        }
    }
    
    private void checkAlertStatus() {
        Optional<ShiftConfiguration> currentShift = settingsService.getCurrentShift();
        if (currentShift.isEmpty()) {
            return;
        }
        
        ShiftConfiguration shift = currentShift.get();
        int hourlyTarget = settingsService.calculateTotalHourlyTarget();
        if (hourlyTarget <= 0) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        LocalTime shiftStart = shift.getStartTime();
        
        long elapsedMinutes;
        if (shift.getEndTime().isAfter(shiftStart)) {
            elapsedMinutes = Duration.between(shiftStart, currentTime).toMinutes();
        } else {
            // Overnight shift
            if (currentTime.isBefore(shift.getEndTime())) {
                elapsedMinutes = Duration.between(shiftStart, LocalTime.MAX).toMinutes() + Duration.between(LocalTime.MIN, currentTime).toMinutes() + 1;
            } else {
                elapsedMinutes = Duration.between(shiftStart, currentTime).toMinutes();
            }
        }
        
        if (elapsedMinutes <= 15) {
            // Give 15 min warm-up buffer before triggering low-production alert
            return;
        }
        
        int expectedProduction = (int) Math.round((elapsedMinutes / 60.0) * hourlyTarget);
        LocalDateTime shiftStartDateTime = now.toLocalDate().atTime(shiftStart);
        if (shift.getEndTime().isBefore(shiftStart) && currentTime.isBefore(shift.getEndTime())) {
            shiftStartDateTime = shiftStartDateTime.minusDays(1);
        }
        
        long actualProduction = productionRepository.countByStatusAndCompletionTimeBetween(
                ProductionStatus.COMPLETED, shiftStartDateTime, now);
        
        alertService.checkLowProductionAlert((int) actualProduction, expectedProduction);
    }
    
    public boolean isSimulatedMode() {
        try {
            return settingsRepository.findBySettingsKey("DEFAULT")
                    .map(s -> "SIMULATED".equalsIgnoreCase(s.getPreferredMode()))
                    .orElse(true);
        } catch (Exception e) {
            return true;
        }
    }
    
    public void startSimulation() {
        this.isRunning = true;
        logger.info("Continuous simulation service marked ACTIVE");
    }
    
    public void stopSimulation() {
        this.isRunning = false;
        logger.info("Continuous simulation service marked INACTIVE");
    }
    
    public boolean isRunning() {
        return isRunning;
    }
}