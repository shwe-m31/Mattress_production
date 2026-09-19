package com.peps.production.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_settings")
public class ProductionSettings {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String settingsKey = "DEFAULT";
    
    // Mode selection
    @Column(nullable = false)
    private String preferredMode = "SIMULATED"; // "SIMULATED" or "DATA_SOURCE"
    
    // Dashboard auto-update interval (in minutes)
    @Column(nullable = false)
    private int dashboardUpdateInterval = 15;
    
    // Data Source configuration (only used in DATA_SOURCE mode)
    @Column(length = 255)
    private String dataSourceUrl;
    @Column(length = 50)
    private String authenticationMethod;
    @Column
    private Integer dataSourcePollingInterval; // in seconds
    
    // Connection status
    @Column(length = 50)
    private String connectionStatus = "DISCONNECTED";
    
    // Timestamps
    @Column(nullable = false)
    private LocalDateTime lastUpdated;
    
    public ProductionSettings() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSettingsKey() { return settingsKey; }
    public void setSettingsKey(String settingsKey) { this.settingsKey = settingsKey; }
    
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
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}