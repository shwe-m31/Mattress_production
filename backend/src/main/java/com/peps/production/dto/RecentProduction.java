package com.peps.production.dto;

import java.time.LocalDateTime;

public class RecentProduction {
    private String productType;
    private String variety;
    private String size;
    private LocalDateTime completionTime;
    private String productionLine;
    private String status;
    private String shift;
    
    public RecentProduction() {}
    
    public RecentProduction(String productType, String variety, String size, LocalDateTime completionTime, 
                          String productionLine, String status, String shift) {
        this.productType = productType;
        this.variety = variety;
        this.size = size;
        this.completionTime = completionTime;
        this.productionLine = productionLine;
        this.status = status;
        this.shift = shift;
    }
    
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    
    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }
    
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    
    public LocalDateTime getCompletionTime() { return completionTime; }
    public void setCompletionTime(LocalDateTime completionTime) { this.completionTime = completionTime; }
    
    public String getProductionLine() { return productionLine; }
    public void setProductionLine(String productionLine) { this.productionLine = productionLine; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }
}
