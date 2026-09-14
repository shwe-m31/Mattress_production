package com.peps.production.dto;

public class Settings {
    private int refreshInterval; // in minutes
    private boolean tvMode;
    private boolean autoRotate;
    private int rotateInterval; // in seconds
    private boolean simulatorMode;
    private String lastUpdated;
    
    public Settings() {
        // Default values
        this.refreshInterval = 15;
        this.tvMode = false;
        this.autoRotate = false;
        this.rotateInterval = 30;
        this.simulatorMode = true;
    }
    
    public int getRefreshInterval() { return refreshInterval; }
    public void setRefreshInterval(int refreshInterval) { this.refreshInterval = refreshInterval; }
    
    public boolean isTvMode() { return tvMode; }
    public void setTvMode(boolean tvMode) { this.tvMode = tvMode; }
    
    public boolean isAutoRotate() { return autoRotate; }
    public void setAutoRotate(boolean autoRotate) { this.autoRotate = autoRotate; }
    
    public int getRotateInterval() { return rotateInterval; }
    public void setRotateInterval(int rotateInterval) { this.rotateInterval = rotateInterval; }
    
    public boolean isSimulatorMode() { return simulatorMode; }
    public void setSimulatorMode(boolean simulatorMode) { this.simulatorMode = simulatorMode; }
    
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
}