package com.peps.production.service;

import com.peps.production.dto.Settings;
import com.peps.production.model.*;
import com.peps.production.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);
    
    private final ProductionSettingsRepository settingsRepository;
    private final ProductionTargetRepository targetRepository;
    private final ShiftConfigurationRepository shiftRepository;
    
    public SettingsService(ProductionSettingsRepository settingsRepository,
                          ProductionTargetRepository targetRepository,
                          ShiftConfigurationRepository shiftRepository) {
        this.settingsRepository = settingsRepository;
        this.targetRepository = targetRepository;
        this.shiftRepository = shiftRepository;
    }
    
    /**
     * Get complete settings including production targets and shift configurations
     */
    public Settings getSettings() {
        Settings settings = new Settings();
        
        // Get production settings
        ProductionSettings prodSettings = settingsRepository.findBySettingsKey("DEFAULT")
            .orElseGet(() -> {
                ProductionSettings newSettings = new ProductionSettings();
                newSettings.setSettingsKey("DEFAULT");
                return settingsRepository.save(newSettings);
            });
        
        settings.setPreferredMode(prodSettings.getPreferredMode());
        settings.setDashboardUpdateInterval(prodSettings.getDashboardUpdateInterval());
        settings.setDataSourceUrl(prodSettings.getDataSourceUrl());
        settings.setAuthenticationMethod(prodSettings.getAuthenticationMethod());
        settings.setDataSourcePollingInterval(prodSettings.getDataSourcePollingInterval());
        settings.setConnectionStatus(prodSettings.getConnectionStatus());
        settings.setLastUpdated(prodSettings.getLastUpdated().toString());
        
        // Get production targets
        Map<String, Integer> targets = new LinkedHashMap<>();
        for (ProductType productType : ProductType.values()) {
            for (MattressSize size : MattressSize.values()) {
                String key = productType.name() + "_" + size.name();
                Optional<ProductionTarget> target = targetRepository.findByProductTypeAndSize(productType, size);
                targets.put(key, target.map(ProductionTarget::getHourlyTarget).orElse(0));
            }
        }
        settings.setProductionTargets(targets);
        
        // Get shift configurations
        List<Settings.ShiftConfigDto> shiftConfigs = shiftRepository.findByActiveTrueOrderByStartTime()
            .stream()
            .map(shift -> new Settings.ShiftConfigDto(
                shift.getShiftName(),
                shift.getStartTime().toString(),
                shift.getEndTime().toString(),
                shift.isActive()
            ))
            .collect(Collectors.toList());
        settings.setShiftConfigurations(shiftConfigs);
        
        return settings;
    }
    
    /**
     * Update settings including production targets and shift configurations
     */
    @Transactional
    public Settings updateSettings(Settings settings) {
        // Update production settings
        ProductionSettings prodSettings = settingsRepository.findBySettingsKey("DEFAULT")
            .orElseGet(() -> {
                ProductionSettings newSettings = new ProductionSettings();
                newSettings.setSettingsKey("DEFAULT");
                return newSettings;
            });
        
        prodSettings.setPreferredMode(settings.getPreferredMode());
        prodSettings.setDashboardUpdateInterval(settings.getDashboardUpdateInterval());
        
        // Only update data source settings if in DATA_SOURCE mode
        if ("DATA_SOURCE".equals(settings.getPreferredMode())) {
            prodSettings.setDataSourceUrl(settings.getDataSourceUrl());
            prodSettings.setAuthenticationMethod(settings.getAuthenticationMethod());
            prodSettings.setDataSourcePollingInterval(settings.getDataSourcePollingInterval());
        } else {
            // Clear data source settings in SIMULATED mode
            prodSettings.setDataSourceUrl(null);
            prodSettings.setAuthenticationMethod(null);
            prodSettings.setDataSourcePollingInterval(null);
            prodSettings.setConnectionStatus("DISCONNECTED");
        }
        
        prodSettings.setLastUpdated(LocalDateTime.now());
        settingsRepository.save(prodSettings);
        
        // Update production targets
        if (settings.getProductionTargets() != null) {
            for (Map.Entry<String, Integer> entry : settings.getProductionTargets().entrySet()) {
                String[] parts = entry.getKey().split("_");
                if (parts.length == 2) {
                    try {
                        ProductType productType = ProductType.valueOf(parts[0]);
                        MattressSize size = MattressSize.valueOf(parts[1]);
                        
                        Optional<ProductionTarget> existing = targetRepository.findByProductTypeAndSize(productType, size);
                        if (existing.isPresent()) {
                            ProductionTarget target = existing.get();
                            target.setHourlyTarget(entry.getValue());
                            target.setLastUpdated(LocalDateTime.now());
                            targetRepository.save(target);
                        } else {
                            ProductionTarget newTarget = new ProductionTarget(productType, size, entry.getValue());
                            targetRepository.save(newTarget);
                        }
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid product type or size in target: {}", entry.getKey());
                    }
                }
            }
        }
        
        // Update shift configurations
        if (settings.getShiftConfigurations() != null) {
            for (Settings.ShiftConfigDto shiftConfig : settings.getShiftConfigurations()) {
                Optional<ShiftConfiguration> existing = shiftRepository.findByShiftName(shiftConfig.getShiftName());
                if (existing.isPresent()) {
                    ShiftConfiguration shift = existing.get();
                    shift.setStartTime(java.time.LocalTime.parse(shiftConfig.getStartTime()));
                    shift.setEndTime(java.time.LocalTime.parse(shiftConfig.getEndTime()));
                    shift.setActive(shiftConfig.isActive());
                    shift.setLastUpdated(LocalDateTime.now());
                    shiftRepository.save(shift);
                } else {
                    ShiftConfiguration newShift = new ShiftConfiguration(
                        shiftConfig.getShiftName(),
                        java.time.LocalTime.parse(shiftConfig.getStartTime()),
                        java.time.LocalTime.parse(shiftConfig.getEndTime())
                    );
                    newShift.setActive(shiftConfig.isActive());
                    shiftRepository.save(newShift);
                }
            }
        }
        
        logger.info("Settings updated successfully. Mode: {}, Dashboard interval: {} min", 
            settings.getPreferredMode(), settings.getDashboardUpdateInterval());
        
        return getSettings();
    }
    
    /**
     * Calculate total hourly target from all configured product targets
     */
    public int calculateTotalHourlyTarget() {
        return targetRepository.findAll()
            .stream()
            .mapToInt(ProductionTarget::getHourlyTarget)
            .sum();
    }
    
    /**
     * Calculate shift target based on hourly target and shift duration
     */
    public int calculateShiftTarget(String shiftName) {
        Optional<ShiftConfiguration> shiftOpt = shiftRepository.findByShiftName(shiftName);
        if (shiftOpt.isEmpty()) {
            return 0;
        }
        
        ShiftConfiguration shift = shiftOpt.get();
        double durationHours = shift.getDurationHours();
        int hourlyTarget = calculateTotalHourlyTarget();
        
        return (int) (hourlyTarget * durationHours);
    }
    
    /**
     * Get current shift based on current time
     */
    public Optional<ShiftConfiguration> getCurrentShift() {
        java.time.LocalTime now = java.time.LocalTime.now();
        List<ShiftConfiguration> activeShifts = shiftRepository.findByActiveTrueOrderByStartTime();
        
        for (ShiftConfiguration shift : activeShifts) {
            if (isTimeInShift(now, shift)) {
                return Optional.of(shift);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Check if a given time falls within a shift
     */
    private boolean isTimeInShift(java.time.LocalTime time, ShiftConfiguration shift) {
        java.time.LocalTime start = shift.getStartTime();
        java.time.LocalTime end = shift.getEndTime();
        
        if (end.isAfter(start)) {
            // Normal shift (e.g., 06:00 to 14:00)
            return !time.isBefore(start) && !time.isAfter(end);
        } else {
            // Overnight shift (e.g., 22:00 to 06:00)
            return !time.isBefore(start) || !time.isAfter(end);
        }
    }
    
    /**
     * Initialize default settings if they don't exist
     */
    @Transactional
    public void initializeDefaultSettings() {
        // Check if settings already exist
        if (settingsRepository.findBySettingsKey("DEFAULT").isPresent()) {
            return;
        }
        
        logger.info("Initializing default production settings");
        
        // Create default production settings
        ProductionSettings defaultSettings = new ProductionSettings();
        defaultSettings.setSettingsKey("DEFAULT");
        defaultSettings.setPreferredMode("SIMULATED");
        defaultSettings.setDashboardUpdateInterval(15);
        defaultSettings.setConnectionStatus("DISCONNECTED");
        settingsRepository.save(defaultSettings);
        
        // Create default production targets (all zeros initially)
        for (ProductType productType : ProductType.values()) {
            for (MattressSize size : MattressSize.values()) {
                ProductionTarget target = new ProductionTarget(productType, size, 0);
                targetRepository.save(target);
            }
        }
        
        // Create default shift configurations
        List<ShiftConfiguration> defaultShifts = Arrays.asList(
            new ShiftConfiguration("Morning Shift", java.time.LocalTime.of(6, 0), java.time.LocalTime.of(14, 0)),
            new ShiftConfiguration("Evening Shift", java.time.LocalTime.of(14, 0), java.time.LocalTime.of(22, 0)),
            new ShiftConfiguration("Night Shift", java.time.LocalTime.of(22, 0), java.time.LocalTime.of(6, 0))
        );
        
        for (ShiftConfiguration shift : defaultShifts) {
            shiftRepository.save(shift);
        }
        
        logger.info("Default settings initialized successfully");
    }
}