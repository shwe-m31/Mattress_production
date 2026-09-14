package com.peps.production.dto;

import java.time.LocalDateTime;

public class RecentProduction {
    private String productType;
    private String variety;
    private String size;
    private LocalDateTime completionTime;
    private int quantity;
    private double cycleTime;
    private String productionLine;
    private String status;
    
    public RecentProduction() {}
    
    public RecentProduction(String productType, String variety, String size, LocalDateTime completionTime, 
                          int quantity, double cycleTime, String productionLine, String status) {
        this.productType = productType;
        this.variety = variety;
        this.size = size;
        this.completionTime = completionTime;
        this.quantity = quantity;
        this.cycleTime = cycleTime;
        this.productionLine = productionLine;
        this.status = status;
    }
    
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    
    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }
    
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    
    public LocalDateTime getCompletionTime() { return completionTime; }
    public void setCompletionTime(LocalDateTime completionTime) { this.completionTime = completionTime; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public double getCycleTime() { return cycleTime; }
    public void setCycleTime(double cycleTime) { this.cycleTime = cycleTime; }
    
    public String getProductionLine() { return productionLine; }
    public void setProductionLine(String productionLine) { this.productionLine = productionLine; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
