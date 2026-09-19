package com.peps.production.model;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_configuration")
public class ShiftConfiguration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String shiftName;
    
    @Column(nullable = false)
    private LocalTime startTime;
    
    @Column(nullable = false)
    private LocalTime endTime;
    
    @Column(nullable = false)
    private boolean active = true;
    
    @Column(nullable = false)
    private LocalDateTime lastUpdated;
    
    public ShiftConfiguration() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    public ShiftConfiguration(String shiftName, LocalTime startTime, LocalTime endTime) {
        this.shiftName = shiftName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }
    
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    /**
     * Calculate shift duration in hours
     */
    public double getDurationHours() {
        if (endTime.isAfter(startTime)) {
            return java.time.Duration.between(startTime, endTime).toMinutes() / 60.0;
        } else {
            // Overnight shift (e.g., 22:00 to 06:00)
            return (24 * 60 - java.time.Duration.between(endTime, startTime).toMinutes()) / 60.0;
        }
    }
}