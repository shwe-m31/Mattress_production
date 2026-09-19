package com.peps.production.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_data", indexes = {
    @Index(name = "idx_completion_time", columnList = "completion_time"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_product_type", columnList = "product_type"),
    @Index(name = "idx_production_line", columnList = "production_line"),
    @Index(name = "idx_status_completion", columnList = "status, completion_time"),
    @Index(name = "idx_production_date", columnList = "production_date"),
    @Index(name = "idx_shift", columnList = "shift")
})
public class ProductionData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ProductType productType;
    @Column(length = 50)
    private String variety;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private MattressSize size;
    @Column(length = 50)
    private String productionLine;
    @Column
    private LocalDateTime startTime;
    @Column(nullable = false)
    private LocalDateTime completionTime;
    @Column
    private Double cycleTime; // in minutes
    @Column(nullable = false)
    private LocalDateTime productionTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ProductionStatus status;
    @Column(length = 20)
    private String dataSource; // "SIMULATOR" or "REAL" to identify simulated historical data
    @Column
    private LocalDate productionDate;
    @Column(length = 50)
    private String shift;
    @Column(length = 50)
    private String sourceMode; // "SIMULATED" or "DATA_SOURCE"

    @PrePersist
    @PreUpdate
    public void ensureDefaults() {
        if (this.productionDate == null) {
            if (this.completionTime != null) {
                this.productionDate = this.completionTime.toLocalDate();
            } else if (this.productionTime != null) {
                this.productionDate = this.productionTime.toLocalDate();
            } else {
                this.productionDate = LocalDate.now();
            }
        }
        if (this.startTime == null) {
            if (this.completionTime != null) {
                this.startTime = this.completionTime;
            } else if (this.productionTime != null) {
                this.startTime = this.productionTime;
            } else {
                this.startTime = LocalDateTime.now();
            }
        }
        if (this.productionTime == null) {
            if (this.completionTime != null) {
                this.productionTime = this.completionTime;
            } else {
                this.productionTime = LocalDateTime.now();
            }
        }
    }

    public ProductionData() { }
    
    // Constructor for single mattress event (one event = one mattress)
    public ProductionData(ProductType productType, MattressSize size, LocalDateTime completionTime, String shift, String sourceMode) {
        this.productType = productType; 
        this.size = size;
        this.completionTime = completionTime; 
        this.startTime = completionTime;
        this.productionTime = completionTime;
        this.status = ProductionStatus.COMPLETED;
        this.dataSource = "SIMULATOR";
        this.productionDate = completionTime.toLocalDate();
        this.shift = shift;
        this.sourceMode = sourceMode != null ? sourceMode : "SIMULATED";
    }
    
    // Constructor for detailed event creation
    public ProductionData(ProductType productType, String variety, MattressSize size, 
                         String productionLine, LocalDateTime startTime, LocalDateTime completionTime, 
                         Double cycleTime, ProductionStatus status, String dataSource, String shift, String sourceMode) {
        this.productType = productType;
        this.variety = variety;
        this.size = size;
        this.productionLine = productionLine;
        this.startTime = startTime;
        this.completionTime = completionTime;
        this.cycleTime = cycleTime;
        this.status = status;
        this.productionTime = completionTime;
        this.dataSource = dataSource != null ? dataSource : "SIMULATOR";
        this.productionDate = completionTime.toLocalDate();
        this.shift = shift;
        this.sourceMode = sourceMode != null ? sourceMode : "SIMULATED";
    }
    
    public Long getId() { return id; }
    public ProductType getProductType() { return productType; }
    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }
    public MattressSize getSize() { return size; }
    public String getProductionLine() { return productionLine; }
    public void setProductionLine(String productionLine) { this.productionLine = productionLine; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getCompletionTime() { return completionTime; }
    public Double getCycleTime() { return cycleTime; }
    public void setCycleTime(Double cycleTime) { this.cycleTime = cycleTime; }
    public LocalDateTime getProductionTime() { return productionTime; }
    public void setProductionTime(LocalDateTime productionTime) { this.productionTime = productionTime; }
    public ProductionStatus getStatus() { return status; }
    public void setStatus(ProductionStatus status) { this.status = status; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }
    public String getSourceMode() { return sourceMode; }
    public void setSourceMode(String sourceMode) { this.sourceMode = sourceMode; }
}
