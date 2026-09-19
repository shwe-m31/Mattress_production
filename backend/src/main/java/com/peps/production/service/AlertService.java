package com.peps.production.service;

import com.peps.production.model.ProductionAlert;
import com.peps.production.model.ShiftConfiguration;
import com.peps.production.repository.ProductionAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing production alerts.
 * Automatically generates LOW PRODUCTION alerts based on deterministic target formulas.
 */
@Service
public class AlertService {
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    
    private final ProductionAlertRepository alertRepository;
    private final SettingsService settingsService;
    
    @Value("${alerts.low-production-threshold:0.8}")
    private double lowProductionThreshold; // 80% by default
    
    public AlertService(ProductionAlertRepository alertRepository,
                        SettingsService settingsService) {
        this.alertRepository = alertRepository;
        this.settingsService = settingsService;
    }
    
    /**
     * Check and generate low production alert if needed.
     * Expected production = configured hourly target × elapsed minutes / 60.
     * If actual production < expected * threshold, creates alert.
     */
    @Transactional
    public void checkLowProductionAlert(int actualProduction, int expectedProduction) {
        if (expectedProduction <= 0) {
            return;
        }
        
        double ratio = (double) actualProduction / expectedProduction;
        
        if (ratio < lowProductionThreshold) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime checkWindow = now.minusMinutes(60);
            
            List<ProductionAlert> recentAlerts = alertRepository.findByCreatedTimestampAfterOrderByCreatedTimestampDesc(checkWindow);
            boolean hasRecentAlert = recentAlerts.stream()
                .anyMatch(a -> "LOW_PRODUCTION".equals(a.getAlertType()) && "ACTIVE".equals(a.getStatus()));
            
            if (!hasRecentAlert) {
                String shift = determineCurrentShift(now);
                String message = String.format(
                    "Low production detected: %d actual units vs %d expected (%.1f%% pace, threshold: %.0f%%)",
                    actualProduction, expectedProduction, (ratio * 100), (lowProductionThreshold * 100)
                );
                
                ProductionAlert alert = new ProductionAlert("LOW_PRODUCTION", message, shift, "HIGH");
                alert.setProductionLine("ALL LINES");
                alert.setStatus("ACTIVE");
                
                alertRepository.save(alert);
                logger.warn("Low production alert generated: {}", message);
            }
        }
    }
    
    public List<ProductionAlert> getActiveAlerts() {
        return alertRepository.findByStatusOrderByCreatedTimestampDesc("ACTIVE");
    }
    
    @Transactional
    public void resolveAlert(Long alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.setStatus("RESOLVED");
            alert.setResolvedTimestamp(LocalDateTime.now());
            alertRepository.save(alert);
            logger.info("Resolved alert #{}", alertId);
        });
    }
    
    @Transactional
    public void resolveAllActiveAlerts() {
        List<ProductionAlert> activeAlerts = getActiveAlerts();
        for (ProductionAlert alert : activeAlerts) {
            alert.setStatus("RESOLVED");
            alert.setResolvedTimestamp(LocalDateTime.now());
            alertRepository.save(alert);
        }
        logger.info("Resolved all {} active alerts", activeAlerts.size());
    }
    
    private String determineCurrentShift(LocalDateTime dateTime) {
        Optional<ShiftConfiguration> currentShift = settingsService.getCurrentShift();
        return currentShift.map(ShiftConfiguration::getShiftName).orElse("Standard Shift");
    }
}