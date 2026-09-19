package com.peps.production.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_alerts", indexes = {
    @Index(name = "idx_alert_time", columnList = "created_timestamp"),
    @Index(name = "idx_alert_shift", columnList = "shift"),
    @Index(name = "idx_alert_status", columnList = "status")
})
public class ProductionAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 50, nullable = false)
    private String alertType;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(nullable = false)
    private LocalDateTime createdTimestamp;
    
    @Column(length = 50)
    private String shift;
    
    @Column(length = 50)
    private String productionLine;
    
    @Column(length = 20)
    private String severity = "MEDIUM"; // "LOW", "MEDIUM", "HIGH"
    
    @Column(length = 20)
    private String status = "ACTIVE"; // "ACTIVE", "RESOLVED"
    
    @Column
    private LocalDateTime resolvedTimestamp;
    
    public ProductionAlert() {
        this.createdTimestamp = LocalDateTime.now();
    }
    
    public ProductionAlert(String alertType, String message, String shift, String severity) {
        this.alertType = alertType;
        this.message = message;
        this.shift = shift;
        this.severity = severity;
        this.createdTimestamp = LocalDateTime.now();
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(LocalDateTime createdTimestamp) { this.createdTimestamp = createdTimestamp; }
    
    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }
    
    public String getProductionLine() { return productionLine; }
    public void setProductionLine(String productionLine) { this.productionLine = productionLine; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getResolvedTimestamp() { return resolvedTimestamp; }
    public void setResolvedTimestamp(LocalDateTime resolvedTimestamp) { this.resolvedTimestamp = resolvedTimestamp; }
}