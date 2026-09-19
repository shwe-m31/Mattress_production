package com.peps.production.dto;

import java.util.*;

public class DashboardResponse {
    private int totalProduction;
    private int springCount;
    private int hypnosCount;
    private int shiftTarget;
    private int targetPercentage;
    private int efficiency;
    private long downtimeMinutes;
    private Map<String, SizeBreakdown> sizeBreakdown;
    private List<RecentProduction> recentItems;
    private HourlyProduction hourlyData;
    private List<AlertDto> alerts;
    private CurrentShiftInfo currentShift;
    
    public int getTotalProduction() { return totalProduction; } public void setTotalProduction(int value) { totalProduction = value; }
    public int getSpringCount() { return springCount; } public void setSpringCount(int value) { springCount = value; }
    public int getHypnosCount() { return hypnosCount; } public void setHypnosCount(int value) { hypnosCount = value; }
    public int getShiftTarget() { return shiftTarget; } public void setShiftTarget(int value) { shiftTarget = value; }
    public int getTargetPercentage() { return targetPercentage; } public void setTargetPercentage(int value) { targetPercentage = value; }
    public int getEfficiency() { return efficiency; } public void setEfficiency(int value) { efficiency = value; }
    public long getDowntimeMinutes() { return downtimeMinutes; } public void setDowntimeMinutes(long value) { downtimeMinutes = value; }
    public Map<String, SizeBreakdown> getSizeBreakdown() { return sizeBreakdown; } public void setSizeBreakdown(Map<String, SizeBreakdown> value) { sizeBreakdown = value; }
    public List<RecentProduction> getRecentItems() { return recentItems; } public void setRecentItems(List<RecentProduction> value) { recentItems = value; }
    public HourlyProduction getHourlyData() { return hourlyData; } public void setHourlyData(HourlyProduction value) { hourlyData = value; }
    public List<AlertDto> getAlerts() { return alerts; } public void setAlerts(List<AlertDto> value) { alerts = value; }
    public CurrentShiftInfo getCurrentShift() { return currentShift; } public void setCurrentShift(CurrentShiftInfo value) { currentShift = value; }
    
    // Inner classes for additional data
    public static class AlertDto {
        private String alertType;
        private String message;
        private String severity;
        private String status;
        private String createdTimestamp;
        
        public AlertDto() {}
        public AlertDto(String alertType, String message, String severity, String status, String createdTimestamp) {
            this.alertType = alertType;
            this.message = message;
            this.severity = severity;
            this.status = status;
            this.createdTimestamp = createdTimestamp;
        }
        
        public String getAlertType() { return alertType; } public void setAlertType(String value) { alertType = value; }
        public String getMessage() { return message; } public void setMessage(String value) { message = value; }
        public String getSeverity() { return severity; } public void setSeverity(String value) { severity = value; }
        public String getStatus() { return status; } public void setStatus(String value) { status = value; }
        public String getCreatedTimestamp() { return createdTimestamp; } public void setCreatedTimestamp(String value) { createdTimestamp = value; }
    }
    
    public static class CurrentShiftInfo {
        private String shiftName;
        private String startTime;
        private String endTime;
        private double durationHours;
        private boolean isActive;
        
        public CurrentShiftInfo() {}
        public CurrentShiftInfo(String shiftName, String startTime, String endTime, double durationHours, boolean isActive) {
            this.shiftName = shiftName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationHours = durationHours;
            this.isActive = isActive;
        }
        
        public String getShiftName() { return shiftName; } public void setShiftName(String value) { shiftName = value; }
        public String getStartTime() { return startTime; } public void setStartTime(String value) { startTime = value; }
        public String getEndTime() { return endTime; } public void setEndTime(String value) { endTime = value; }
        public double getDurationHours() { return durationHours; } public void setDurationHours(double value) { durationHours = value; }
        public boolean isActive() { return isActive; } public void setActive(boolean value) { isActive = value; }
    }
}
