package com.peps.production.service;

import com.peps.production.model.DowntimeEvent;
import com.peps.production.model.ShiftConfiguration;
import com.peps.production.repository.DowntimeEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Random;

/**
 * Service for managing and simulating downtime events.
 * Downtime is calculated strictly from explicit stoppage intervals.
 */
@Service
public class DowntimeSimulationService {
    private static final Logger logger = LoggerFactory.getLogger(DowntimeSimulationService.class);
    
    private final DowntimeEventRepository downtimeRepository;
    private final SettingsService settingsService;
    
    // Downtime state
    private boolean inDowntime = false;
    private LocalDateTime downtimeStartTime;
    private int currentDowntimeDurationMinutes = 0;
    
    // Downtime reasons for simulation
    private static final String[] DOWNTIME_REASONS = {
        "Machine stoppage",
        "Maintenance required",
        "Material delay",
        "Operator delay",
        "Changeover",
        "Quality check"
    };
    
    private static final String[] DOWNTIME_CATEGORIES = {
        "MECHANICAL",
        "MAINTENANCE",
        "MATERIAL",
        "OPERATOR",
        "CHANGEOVER",
        "QUALITY"
    };
    
    public DowntimeSimulationService(DowntimeEventRepository downtimeRepository,
                                     SettingsService settingsService) {
        this.downtimeRepository = downtimeRepository;
        this.settingsService = settingsService;
    }
    
    public boolean isCurrentlyInDowntime() {
        if (!inDowntime) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (downtimeStartTime != null && 
            java.time.Duration.between(downtimeStartTime, now).toMinutes() >= currentDowntimeDurationMinutes) {
            inDowntime = false;
            downtimeStartTime = null;
            currentDowntimeDurationMinutes = 0;
            logger.info("Downtime period completed at {}", now);
            return false;
        }
        return true;
    }
    
    /**
     * Start a new simulated downtime event
     */
    @Transactional
    public DowntimeEvent startDowntime(LocalDateTime startTime) {
        long seed = startTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        Random random = new Random(seed);
        
        int reasonIndex = random.nextInt(DOWNTIME_REASONS.length);
        String reason = DOWNTIME_REASONS[reasonIndex];
        String category = DOWNTIME_CATEGORIES[reasonIndex];
        int durationMinutes = 5 + random.nextInt(16); // 5 to 20 minutes
        
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
        
        String shift = determineCurrentShift(startTime);
        
        DowntimeEvent downtime = new DowntimeEvent(startTime, endTime, reason, shift, "SIMULATED");
        downtime.setCategory(category);
        downtime.setStatus("RESOLVED");
        downtime.setProductionLine("SPRING-01");
        
        DowntimeEvent saved = downtimeRepository.save(downtime);
        
        this.inDowntime = true;
        this.downtimeStartTime = startTime;
        this.currentDowntimeDurationMinutes = durationMinutes;
        
        logger.info("Simulated downtime event started: {} min, reason: {}, category: {}",
                durationMinutes, reason, category);
        
        return saved;
    }
    
    /**
     * Check if a downtime should occur based on controlled probability
     */
    public boolean shouldSimulateDowntime(LocalDateTime currentTime) {
        if (inDowntime) {
            return false;
        }
        
        // Only during active shift
        Optional<ShiftConfiguration> currentShift = settingsService.getCurrentShift();
        if (currentShift.isEmpty()) {
            return false;
        }
        
        long seed = currentTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        Random random = new Random(seed);
        // ~2% chance per minute during production
        return random.nextDouble() < 0.02;
    }
    
    private String determineCurrentShift(LocalDateTime dateTime) {
        Optional<ShiftConfiguration> currentShift = settingsService.getCurrentShift();
        return currentShift.map(ShiftConfiguration::getShiftName).orElse("Standard Shift");
    }
    
    /**
     * Get total downtime for today in minutes
     */
    public long getTodayDowntime() {
        LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        Long total = downtimeRepository.sumDurationMinutesBetween(dayStart, now);
        return total != null ? total : 0L;
    }
}