package com.peps.production.service;

import com.peps.production.model.DowntimeEvent;
import com.peps.production.repository.DowntimeEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Service for simulating downtime events
 * Creates realistic downtime periods during production
 */
@Service
public class DowntimeSimulationService {
    private static final Logger logger = LoggerFactory.getLogger(DowntimeSimulationService.class);
    
    private final DowntimeEventRepository downtimeRepository;
    private final ProductionTimeService timeService;
    private final SettingsService settingsService;
    
    // Downtime reasons for simulation
    private static final String[] DOWNTIME_REASONS = {
        "Machine stoppage",
        "Maintenance required",
        "Material delay",
        "Operator delay",
        "Changeover",
        "Quality check"
    };
    
    // Downtime categories
    private static final String[] DOWNTIME_CATEGORIES = {
        "MECHANICAL",
        "MAINTENANCE",
        "MATERIAL",
        "OPERATOR",
        "CHANGEOVER",
        "QUALITY"
    };
    
    public DowntimeSimulationService(DowntimeEventRepository downtimeRepository,
                                    ProductionTimeService timeService,
                                    SettingsService settingsService) {
        this.downtimeRepository = downtimeRepository;
        this.timeService = timeService;
        this.settingsService = settingsService;
    }
    
    /**
     * Simulate a downtime event
     * Creates a downtime record with random duration and reason
     */
    @Transactional
    public DowntimeEvent simulateDowntime(LocalDateTime startTime, int durationMinutes) {
        Random random = new Random(startTime.toEpochSecond());
        
        int reasonIndex = random.nextInt(DOWNTIME_REASONS.length);
        String reason = DOWNTIME_REASONS[reasonIndex];
        String category = DOWNTIME_CATEGORIES[reasonIndex];
        
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
        
        // Determine current shift
        String shift = determineCurrentShift(startTime);
        
        DowntimeEvent downtime = new DowntimeEvent(startTime, endTime, reason, shift, "SIMULATED");
        downtime.setCategory(category);
        downtime.setStatus("RESOLVED");
        
        DowntimeEvent saved = downtimeRepository.save(downtime);
        
        logger.info("Simulated downtime event: {} minutes, reason: {}, shift: {}", 
            durationMinutes, reason, shift);
        
        return saved;
    }
    
    /**
     * Check if a downtime should occur based on probability
     * Returns true if downtime should be simulated
     */
    public boolean shouldSimulateDowntime(LocalDateTime currentTime) {
        // Only simulate downtime during production hours
        if (!timeService.isWithinProductionWindow(currentTime)) {
            return false;
        }
        
        // 5% chance of downtime every 30 minutes during production
        Random random = new Random(currentTime.toEpochSecond());
        return random.nextDouble() < 0.05;
    }
    
    /**
     * Get random downtime duration between 5 and 30 minutes
     */
    public int getRandomDowntimeDuration(LocalDateTime currentTime) {
        Random random = new Random(currentTime.toEpochSecond());
        return 5 + random.nextInt(26); // 5-30 minutes
    }
    
    /**
     * Determine current shift based on time
     */
    private String determineCurrentShift(LocalDateTime dateTime) {
        java.util.Optional<com.peps.production.model.ShiftConfiguration> currentShift = settingsService.getCurrentShift();
        return currentShift.map(shift -> shift.getShiftName()).orElse("Unknown");
    }
    
    /**
     * Get total downtime for today
     */
    public long getTodayDowntime() {
        LocalDateTime dayStart = timeService.getCurrentDate().atStartOfDay();
        LocalDateTime now = timeService.getCurrentTime();
        return downtimeRepository.sumDurationMinutesBetween(dayStart, now);
    }
}