package com.peps.production.service;

import com.peps.production.model.ProductionAlert;
import com.peps.production.repository.ProductionAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing production alerts
 * Generates alerts for low production and other operational issues
 */
@Service
public class AlertService {
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    
    private final ProductionAlertRepository alertRepository;
    private final ProductionTimeService timeService;
    private final SettingsService settingsService;
    
    @Value("${alerts.low-production-threshold:0.8}")
    private double lowProductionThreshold; // 80% by default
    
    public AlertService(ProductionAlertRepository alertRepository,
                       ProductionTimeService timeService,
                       SettingsService settingsService) {
        this.alertRepository = alertRepository;
        this.timeService = timeService;
        this.settingsService = settingsService;
    }
    
    /**
     * Check and generate low production alert if needed
     * Compares actual production vs expected production for current time
     */
    @Transactional
    public void checkLowProductionAlert(int actualProduction, int expectedProduction) {
        if (expectedProduction == 0) {
            return; // No expected production, no alert
        }
        
        double productionRatio = (double) actualProduction / expectedProduction;
        
        if (productionRatio < lowProductionThreshold) {
            // Check if we already have an active low production alert for this time period
            LocalDateTime now = timeService.getCurrentTime();
            LocalDateTime alertCheckWindow = now.minusMinutes(30); // Check for alerts in last 30 minutes
            
            List<ProductionAlert> recentAlerts = alertRepository.findByCreatedTimestampAfterOrderByCreatedTimestampDesc(alertCheckWindow);
            boolean hasRecentLowProductionAlert = recentAlerts.stream()
                .anyMatch(alert -> "LOW_PRODUCTION".equals(alert.getAlertType()) && "ACTIVE".equals(alert.getStatus()));
            
            if (!hasRecentLowProductionAlert) {
                generateLowProductionAlert(actualProduction, expectedProduction, productionRatio);
            }
        }
    }
    
    /**
     * Generate a low production alert
     */
    private void generateLowProductionAlert(int actual, int expected, double ratio) {
        LocalDateTime now = timeService.getCurrentTime();
        String shift = determineCurrentShift(now);
        
        String message = String.format(
            "Low production detected: %d actual vs %d expected (%.1f%% of target)",
            actual, expected, (ratio * 100)
        );
        
        ProductionAlert alert = new ProductionAlert("LOW_PRODUCTION", message, shift, "HIGH");
        alert.setProductionLine("ALL");
        
        alertRepository.save(alert);
        
        logger.warn("Low production alert generated: {}", message);
    }
    
    /**
     * Get active alerts
     */
    public List<ProductionAlert> getActiveAlerts() {
        return alertRepository.findByStatusOrderByCreatedTimestampDesc("ACTIVE");
    }
    
    /**
     * Resolve an alert
     */
    @Transactional
    public void resolveAlert(Long alertId) {
        ProductionAlert alert = alertRepository.findById(alertId).orElse(null);
        if (alert != null) {
            alert.setStatus("RESOLVED");
            alert.setResolvedTimestamp(LocalDateTime.now());
            alertRepository.save(alert);
            logger.info("Alert resolved: {}", alert.getAlertType());
        }
    }
    
    /**
     * Resolve all active alerts
     */
    @Transactional
    public void resolveAllActiveAlerts() {
        List<ProductionAlert> activeAlerts = getActiveAlerts();
        for (ProductionAlert alert : activeAlerts) {
            alert.setStatus("RESOLVED");
            alert.setResolvedTimestamp(LocalDateTime.now());
            alertRepository.save(alert);
        }
        logger.info("Resolved {} active alerts", activeAlerts.size());
    }
    
    /**
     * Determine current shift based on time
     */
    private String determineCurrentShift(LocalDateTime dateTime) {
        java.util.Optional<com.peps.production.model.ShiftConfiguration> currentShift = settingsService.getCurrentShift();
        return currentShift.map(shift -> shift.getShiftName()).orElse("Unknown");
    }
    
    /**
     * Clean up old resolved alerts (older than 7 days)
     */
    @Transactional
    public void cleanupOldAlerts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<ProductionAlert> oldAlerts = alertRepository.findByCreatedTimestampAfterOrderByCreatedTimestampDesc(cutoff);
        
        // Filter for resolved alerts older than cutoff
        oldAlerts.stream()
            .filter(alert -> "RESOLVED".equals(alert.getStatus()) && 
                            alert.getResolvedTimestamp() != null && 
                            alert.getResolvedTimestamp().isBefore(cutoff))
            .forEach(alertRepository::delete);
        
        logger.info("Cleaned up old resolved alerts");
    }
}