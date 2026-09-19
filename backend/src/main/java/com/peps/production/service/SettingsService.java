package com.peps.production.service;

import com.peps.production.dto.Settings;
import com.peps.production.model.*;
import com.peps.production.repository.ProductionSettingsRepository;
import com.peps.production.repository.ProductionTargetRepository;
import com.peps.production.repository.ShiftConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
     * Get complete settings object
     */
    public Settings getSettings() {
        Settings settings = new Settings();

        ProductionSettings prodSettings = settingsRepository.findBySettingsKey("DEFAULT")
                .orElseGet(() -> {
                    ProductionSettings defaultSetting = new ProductionSettings();
                    defaultSetting.setSettingsKey("DEFAULT");
                    return settingsRepository.save(defaultSetting);
                });

        settings.setPreferredMode(prodSettings.getPreferredMode());
        settings.setDashboardUpdateInterval(prodSettings.getDashboardUpdateInterval());
        settings.setDataSourceUrl(prodSettings.getDataSourceUrl());
        settings.setAuthenticationMethod(prodSettings.getAuthenticationMethod());
        settings.setDataSourcePollingInterval(prodSettings.getDataSourcePollingInterval());
        settings.setConnectionStatus(prodSettings.getConnectionStatus());
        settings.setLastUpdated(prodSettings.getLastUpdated() != null ? prodSettings.getLastUpdated().toString() : LocalDateTime.now().toString());

        // 8 Product targets
        Map<String, Integer> targets = new LinkedHashMap<>();
        for (ProductType productType : ProductType.values()) {
            for (MattressSize size : MattressSize.values()) {
                String key = productType.name() + "_" + size.name();
                Optional<ProductionTarget> target = targetRepository.findByProductTypeAndSize(productType, size);
                targets.put(key, target.map(ProductionTarget::getHourlyTarget).orElse(0));
            }
        }
        settings.setProductionTargets(targets);

        // Shifts
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
     * Update settings and recalculate targets
     */
    @Transactional
    public Settings updateSettings(Settings settings) {
        ProductionSettings prodSettings = settingsRepository.findBySettingsKey("DEFAULT")
                .orElseGet(() -> {
                    ProductionSettings s = new ProductionSettings();
                    s.setSettingsKey("DEFAULT");
                    return s;
                });

        String mode = "DATA_SOURCE".equalsIgnoreCase(settings.getPreferredMode()) ? "DATA_SOURCE" : "SIMULATED";
        prodSettings.setPreferredMode(mode);
        if (settings.getDashboardUpdateInterval() > 0) {
            prodSettings.setDashboardUpdateInterval(settings.getDashboardUpdateInterval());
        }

        if ("DATA_SOURCE".equals(mode)) {
            prodSettings.setDataSourceUrl(settings.getDataSourceUrl());
            prodSettings.setAuthenticationMethod(settings.getAuthenticationMethod());
            prodSettings.setDataSourcePollingInterval(settings.getDataSourcePollingInterval());
            prodSettings.setConnectionStatus("READY");
        } else {
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
                        int value = Math.max(0, entry.getValue() != null ? entry.getValue() : 0);

                        Optional<ProductionTarget> existing = targetRepository.findByProductTypeAndSize(productType, size);
                        if (existing.isPresent()) {
                            ProductionTarget target = existing.get();
                            target.setHourlyTarget(value);
                            target.setLastUpdated(LocalDateTime.now());
                            targetRepository.save(target);
                        } else {
                            ProductionTarget newTarget = new ProductionTarget(productType, size, value);
                            targetRepository.save(newTarget);
                        }
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid product type or size key in settings: {}", entry.getKey());
                    }
                }
            }
        }

        // Update shift configurations
        if (settings.getShiftConfigurations() != null && !settings.getShiftConfigurations().isEmpty()) {
            for (Settings.ShiftConfigDto shiftDto : settings.getShiftConfigurations()) {
                Optional<ShiftConfiguration> existing = shiftRepository.findByShiftName(shiftDto.getShiftName());
                LocalTime start = LocalTime.parse(shiftDto.getStartTime());
                LocalTime end = LocalTime.parse(shiftDto.getEndTime());
                
                if (existing.isPresent()) {
                    ShiftConfiguration shift = existing.get();
                    shift.setStartTime(start);
                    shift.setEndTime(end);
                    shift.setActive(shiftDto.isActive());
                    shift.setLastUpdated(LocalDateTime.now());
                    shiftRepository.save(shift);
                } else {
                    ShiftConfiguration newShift = new ShiftConfiguration(shiftDto.getShiftName(), start, end);
                    newShift.setActive(shiftDto.isActive());
                    shiftRepository.save(newShift);
                }
            }
        }

        logger.info("Production settings successfully updated in MySQL database. Mode: {}", mode);
        return getSettings();
    }

    public int calculateSpringHourlyTarget() {
        return targetRepository.findByProductType(ProductType.SPRING)
                .stream().mapToInt(ProductionTarget::getHourlyTarget).sum();
    }

    public int calculateHypnosHourlyTarget() {
        return targetRepository.findByProductType(ProductType.HYPNOS)
                .stream().mapToInt(ProductionTarget::getHourlyTarget).sum();
    }

    public int calculateTotalHourlyTarget() {
        return targetRepository.findAll()
                .stream().mapToInt(ProductionTarget::getHourlyTarget).sum();
    }

    public int calculateShiftTarget(String shiftName) {
        Optional<ShiftConfiguration> shiftOpt = shiftRepository.findByShiftName(shiftName);
        if (shiftOpt.isEmpty()) {
            return calculateTotalHourlyTarget() * 8; // fallback 8h shift
        }
        ShiftConfiguration shift = shiftOpt.get();
        double durationHours = shift.getDurationHours();
        return (int) Math.round(calculateTotalHourlyTarget() * durationHours);
    }

    public Optional<ShiftConfiguration> getCurrentShift() {
        LocalTime now = LocalTime.now();
        List<ShiftConfiguration> shifts = shiftRepository.findByActiveTrueOrderByStartTime();
        for (ShiftConfiguration shift : shifts) {
            if (isTimeInShift(now, shift)) {
                return Optional.of(shift);
            }
        }
        return shifts.isEmpty() ? Optional.empty() : Optional.of(shifts.get(0));
    }

    private boolean isTimeInShift(LocalTime time, ShiftConfiguration shift) {
        LocalTime start = shift.getStartTime();
        LocalTime end = shift.getEndTime();
        if (end.isAfter(start)) {
            return !time.isBefore(start) && !time.isAfter(end);
        } else {
            // Overnight shift (e.g. 22:00 to 06:00)
            return !time.isBefore(start) || !time.isAfter(end);
        }
    }

    @Transactional
    public void initializeDefaultSettings() {
        if (settingsRepository.findBySettingsKey("DEFAULT").isPresent()) {
            return;
        }

        logger.info("Initializing baseline default production settings in MySQL...");

        ProductionSettings defaultSettings = new ProductionSettings();
        defaultSettings.setSettingsKey("DEFAULT");
        defaultSettings.setPreferredMode("SIMULATED");
        defaultSettings.setDashboardUpdateInterval(15);
        defaultSettings.setConnectionStatus("DISCONNECTED");
        settingsRepository.save(defaultSettings);

        // Baseline targets
        Map<String, Integer> defaultTargets = Map.of(
                "SPRING_SINGLE", 10,
                "SPRING_DOUBLE", 12,
                "SPRING_QUEEN", 15,
                "SPRING_KING", 8,
                "HYPNOS_SINGLE", 8,
                "HYPNOS_DOUBLE", 10,
                "HYPNOS_QUEEN", 12,
                "HYPNOS_KING", 7
        );

        for (Map.Entry<String, Integer> entry : defaultTargets.entrySet()) {
            String[] parts = entry.getKey().split("_");
            ProductType type = ProductType.valueOf(parts[0]);
            MattressSize size = MattressSize.valueOf(parts[1]);
            ProductionTarget target = new ProductionTarget(type, size, entry.getValue());
            targetRepository.save(target);
        }

        // Baseline shifts
        List<ShiftConfiguration> defaultShifts = Arrays.asList(
                new ShiftConfiguration("Morning Shift", LocalTime.of(6, 0), LocalTime.of(14, 0)),
                new ShiftConfiguration("Evening Shift", LocalTime.of(14, 0), LocalTime.of(22, 0)),
                new ShiftConfiguration("Night Shift", LocalTime.of(22, 0), LocalTime.of(6, 0))
        );
        shiftRepository.saveAll(defaultShifts);

        logger.info("Baseline default settings initialized successfully");
    }
}