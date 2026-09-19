package com.peps.production.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Settings {
    // Mode selection
    private String preferredMode; // "SIMULATED" or "DATA_SOURCE"
    
    // Dashboard auto-update interval (in minutes)
    private int dashboardUpdateInterval;
    
    // Data Source configuration (only used in DATA_SOURCE mode)
    private String dataSourceUrl;
    private String authenticationMethod;
    private Integer dataSourcePollingInterval; // in seconds
    private String connectionStatus;
    
    // Production targets (8 combinations)
    @JsonAlias({"productTargets", "targets"})
    private Map<String, Integer> productionTargets; // Key: "SPRING_SINGLE", "HYPNOS_KING", etc.
    
    // Shift configurations
    private List<ShiftConfigDto> shiftConfigurations;
    
    // Display settings
    private boolean tvMode;
    private boolean autoRotate;
    private int rotateInterval; // in seconds
    
    private String lastUpdated;
    
    public Settings() {
        // Default values
        this.preferredMode = "SIMULATED";
        this.dashboardUpdateInterval = 15;
        this.connectionStatus = "DISCONNECTED";
        this.tvMode = false;
        this.autoRotate = false;
        this.rotateInterval = 30;
    }
    
    public String getPreferredMode() { return preferredMode; }
    public void setPreferredMode(String preferredMode) { this.preferredMode = preferredMode; }
    
    public int getDashboardUpdateInterval() { return dashboardUpdateInterval; }
    public void setDashboardUpdateInterval(int dashboardUpdateInterval) { this.dashboardUpdateInterval = dashboardUpdateInterval; }
    
    public String getDataSourceUrl() { return dataSourceUrl; }
    public void setDataSourceUrl(String dataSourceUrl) { this.dataSourceUrl = dataSourceUrl; }
    
    public String getAuthenticationMethod() { return authenticationMethod; }
    public void setAuthenticationMethod(String authenticationMethod) { this.authenticationMethod = authenticationMethod; }
    
    public Integer getDataSourcePollingInterval() { return dataSourcePollingInterval; }
    public void setDataSourcePollingInterval(Integer dataSourcePollingInterval) { this.dataSourcePollingInterval = dataSourcePollingInterval; }
    
    public String getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(String connectionStatus) { this.connectionStatus = connectionStatus; }
    
    public Map<String, Integer> getProductionTargets() { return productionTargets; }
    public void setProductionTargets(Map<String, Integer> productionTargets) { this.productionTargets = productionTargets; }
    
    public List<ShiftConfigDto> getShiftConfigurations() { return shiftConfigurations; }
    public void setShiftConfigurations(List<ShiftConfigDto> shiftConfigurations) { this.shiftConfigurations = shiftConfigurations; }
    
    public boolean isTvMode() { return tvMode; }
    public void setTvMode(boolean tvMode) { this.tvMode = tvMode; }
    
    public boolean isAutoRotate() { return autoRotate; }
    public void setAutoRotate(boolean autoRotate) { this.autoRotate = autoRotate; }
    
    public int getRotateInterval() { return rotateInterval; }
    public void setRotateInterval(int rotateInterval) { this.rotateInterval = rotateInterval; }
    
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    
    // Inner class for shift configuration
    public static class ShiftConfigDto {
        private String shiftName;
        private String startTime;
        private String endTime;
        private boolean active;
        
        public ShiftConfigDto() {}
        public ShiftConfigDto(String shiftName, String startTime, String endTime, boolean active) {
            this.shiftName = shiftName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.active = active;
        }
        
        public String getShiftName() { return shiftName; } public void setShiftName(String value) { shiftName = value; }
        public String getStartTime() { return startTime; } public void setStartTime(String value) { startTime = value; }
        public String getEndTime() { return endTime; } public void setEndTime(String value) { endTime = value; }
        public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    }
}