package com.peps.production.dto;

public class ProductionStatusResponse {
    private String connectionStatus;
    private String dataSource;
    private String lastUpdateTime;
    private boolean simulatorActive;
    private long totalRecords;
    
    public ProductionStatusResponse() {}
    
    public String getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(String connectionStatus) { this.connectionStatus = connectionStatus; }
    
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    
    public String getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(String lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    
    public boolean isSimulatorActive() { return simulatorActive; }
    public void setSimulatorActive(boolean simulatorActive) { this.simulatorActive = simulatorActive; }
    
    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }
}