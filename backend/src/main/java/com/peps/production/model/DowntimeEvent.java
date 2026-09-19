package com.peps.production.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "downtime_events", indexes = {
    @Index(name = "idx_downtime_start", columnList = "start_time"),
    @Index(name = "idx_downtime_end", columnList = "end_time"),
    @Index(name = "idx_downtime_shift", columnList = "shift"),
    @Index(name = "idx_downtime_line", columnList = "production_line")
})
public class DowntimeEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    @Column(nullable = false)
    private LocalDateTime endTime;
    
    @Column(nullable = false)
    private long durationMinutes;
    
    @Column(length = 100, nullable = false)
    private String reason;
    
    @Column(length = 50)
    private String category;
    
    @Column(length = 50)
    private String productionLine;
    
    @Column(length = 50)
    private String shift;
    
    @Column(length = 20)
    private String status = "ACTIVE"; // "ACTIVE", "RESOLVED"
    
    @Column(length = 20)
    private String sourceMode; // "SIMULATED" or "DATA_SOURCE"
    
    @Column(nullable = false)
    private LocalDateTime createdTimestamp;
    
    public DowntimeEvent() {
        this.createdTimestamp = LocalDateTime.now();
    }
    
    public DowntimeEvent(LocalDateTime startTime, LocalDateTime endTime, String reason, String shift, String sourceMode) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        this.reason = reason;
        this.shift = shift;
        this.sourceMode = sourceMode;
        this.createdTimestamp = LocalDateTime.now();
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { 
        this.endTime = endTime;
        if (startTime != null && endTime != null) {
            this.durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        }
    }
    
    public long getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(long durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getProductionLine() { return productionLine; }
    public void setProductionLine(String productionLine) { this.productionLine = productionLine; }
    
    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getSourceMode() { return sourceMode; }
    public void setSourceMode(String sourceMode) { this.sourceMode = sourceMode; }
    
    public LocalDateTime getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(LocalDateTime createdTimestamp) { this.createdTimestamp = createdTimestamp; }
}