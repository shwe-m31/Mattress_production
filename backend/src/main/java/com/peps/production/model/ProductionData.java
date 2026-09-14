package com.peps.production.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_data")
public class ProductionData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ProductType productType;
    @Column(length = 50)
    private String variety;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private MattressSize size;
    @Column(nullable = false)
    private int quantity;
    @Column(length = 50)
    private String productionLine;
    @Column(nullable = false)
    private LocalDateTime startTime;
    @Column(nullable = false)
    private LocalDateTime completionTime;
    @Column
    private Double cycleTime; // in minutes
    @Column(nullable = false)
    private LocalDateTime productionTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ProductionStatus status;

    public ProductionData() { }
    
    public ProductionData(ProductType productType, MattressSize size, int quantity, LocalDateTime completionTime) {
        this.productType = productType; this.size = size; this.quantity = quantity;
        this.completionTime = completionTime; this.startTime = completionTime;
        this.status = ProductionStatus.COMPLETED;
    }
    
    // Constructor for detailed event creation
    public ProductionData(ProductType productType, String variety, MattressSize size, int quantity, 
                         String productionLine, LocalDateTime startTime, LocalDateTime completionTime, 
                         Double cycleTime, ProductionStatus status) {
        this.productType = productType;
        this.variety = variety;
        this.size = size;
        this.quantity = quantity;
        this.productionLine = productionLine;
        this.startTime = startTime;
        this.completionTime = completionTime;
        this.cycleTime = cycleTime;
        this.status = status;
        this.productionTime = completionTime; // Set productionTime for compatibility
    }
    
    public Long getId() { return id; }
    public ProductType getProductType() { return productType; }
    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }
    public MattressSize getSize() { return size; }
    public int getQuantity() { return quantity; }
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
}
