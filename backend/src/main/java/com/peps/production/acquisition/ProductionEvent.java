package com.peps.production.acquisition;

import com.peps.production.model.MattressSize;
import com.peps.production.model.ProductType;
import com.peps.production.model.ProductionStatus;

import java.time.LocalDateTime;

public class ProductionEvent {
    private ProductType productType;
    private MattressSize size;
    private String variety;
    private String productionLine;
    private LocalDateTime completionTime;
    private LocalDateTime startTime;
    private Double cycleTime;
    private ProductionStatus status;
    private String sourceMode; // "SIMULATED" or "DATA_SOURCE"

    public ProductionEvent() {}

    public ProductionEvent(ProductType productType, MattressSize size, String variety,
                           String productionLine, LocalDateTime startTime, LocalDateTime completionTime,
                           Double cycleTime, ProductionStatus status, String sourceMode) {
        this.productType = productType;
        this.size = size;
        this.variety = variety;
        this.productionLine = productionLine;
        this.startTime = startTime;
        this.completionTime = completionTime;
        this.cycleTime = cycleTime;
        this.status = status;
        this.sourceMode = sourceMode;
    }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public MattressSize getSize() { return size; }
    public void setSize(MattressSize size) { this.size = size; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public String getProductionLine() { return productionLine; }
    public void setProductionLine(String productionLine) { this.productionLine = productionLine; }

    public LocalDateTime getCompletionTime() { return completionTime; }
    public void setCompletionTime(LocalDateTime completionTime) { this.completionTime = completionTime; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public Double getCycleTime() { return cycleTime; }
    public void setCycleTime(Double cycleTime) { this.cycleTime = cycleTime; }

    public ProductionStatus getStatus() { return status; }
    public void setStatus(ProductionStatus status) { this.status = status; }

    public String getSourceMode() { return sourceMode; }
    public void setSourceMode(String sourceMode) { this.sourceMode = sourceMode; }
}
