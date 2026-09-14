package com.peps.production.dto;

public class SimulatorStatusResponse {
    private boolean active;
    private String lastEventTime;
    private long eventsGenerated;
    private String status;
    
    public SimulatorStatusResponse() {}
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public String getLastEventTime() { return lastEventTime; }
    public void setLastEventTime(String lastEventTime) { this.lastEventTime = lastEventTime; }
    
    public long getEventsGenerated() { return eventsGenerated; }
    public void setEventsGenerated(long eventsGenerated) { this.eventsGenerated = eventsGenerated; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}