package com.peps.production.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.*;

/**
 * Simulation Clock Service
 * Provides simulated time support for accelerated production demonstration
 * When simulation is enabled, time progresses faster than real time
 */
@Service
public class SimulationClockService {
    private static final Logger logger = LoggerFactory.getLogger(SimulationClockService.class);
    
    @Value("${simulation.time-multiplier:30}")
    private int timeMultiplier;
    
    @Value("${simulation.enabled:false}")
    private boolean simulationEnabled;
    
    private LocalDateTime simulationStartTime;
    private LocalDateTime realStartTime;
    private boolean initialized = false;
    
    /**
     * Initialize the simulation clock
     * Should be called on application startup
     */
    public void initialize() {
        if (simulationEnabled && !initialized) {
            this.realStartTime = LocalDateTime.now();
            this.simulationStartTime = realStartTime;
            this.initialized = true;
            logger.info("Simulation clock initialized with multiplier: {}", timeMultiplier);
        }
    }
    
    /**
     * Get current simulated time
     * If simulation is disabled, returns real time
     */
    public LocalDateTime getCurrentSimulatedTime() {
        if (!simulationEnabled || !initialized) {
            return LocalDateTime.now();
        }
        
        LocalDateTime realNow = LocalDateTime.now();
        Duration realElapsed = Duration.between(realStartTime, realNow);
        Duration simulatedElapsed = realElapsed.multipliedBy(timeMultiplier);
        
        return simulationStartTime.plus(simulatedElapsed);
    }
    
    /**
     * Get current date in simulated time
     */
    public LocalDate getCurrentSimulatedDate() {
        return getCurrentSimulatedTime().toLocalDate();
    }
    
    /**
     * Check if simulation is enabled
     */
    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }
    
    /**
     * Get the time multiplier
     */
    public int getTimeMultiplier() {
        return timeMultiplier;
    }
    
    /**
     * Reset the simulation clock (useful for testing)
     */
    public void reset() {
        this.initialized = false;
        this.simulationStartTime = null;
        this.realStartTime = null;
        logger.info("Simulation clock reset");
    }
    
    /**
     * Set a specific simulation start time (useful for testing scenarios)
     */
    public void setSimulationStartTime(LocalDateTime startTime) {
        this.simulationStartTime = startTime;
        this.realStartTime = LocalDateTime.now();
        this.initialized = true;
        logger.info("Simulation clock set to start at: {}", startTime);
    }
    
    /**
     * Get real time (always actual system time)
     */
    public LocalDateTime getRealTime() {
        return LocalDateTime.now();
    }
    
    /**
     * Check if a given simulated time is in the future (relative to current simulated time)
     */
    public boolean isSimulatedTimeInFuture(LocalDateTime dateTime) {
        return dateTime.isAfter(getCurrentSimulatedTime());
    }
    
    /**
     * Check if a given simulated time is in the past (relative to current simulated time)
     */
    public boolean isSimulatedTimeInPast(LocalDateTime dateTime) {
        return dateTime.isBefore(getCurrentSimulatedTime());
    }
}