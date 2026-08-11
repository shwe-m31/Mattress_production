package com.peps.production.dto;

import java.util.*;

public class DashboardResponse {
    private int totalProduction;
    private int springCount;
    private int hypnosCount;
    private int efficiency;
    private Map<String, SizeBreakdown> sizeBreakdown;
    private List<RecentProduction> recentItems;
    private HourlyProduction hourlyData;
    public int getTotalProduction() { return totalProduction; } public void setTotalProduction(int value) { totalProduction = value; }
    public int getSpringCount() { return springCount; } public void setSpringCount(int value) { springCount = value; }
    public int getHypnosCount() { return hypnosCount; } public void setHypnosCount(int value) { hypnosCount = value; }
    public int getEfficiency() { return efficiency; } public void setEfficiency(int value) { efficiency = value; }
    public Map<String, SizeBreakdown> getSizeBreakdown() { return sizeBreakdown; } public void setSizeBreakdown(Map<String, SizeBreakdown> value) { sizeBreakdown = value; }
    public List<RecentProduction> getRecentItems() { return recentItems; } public void setRecentItems(List<RecentProduction> value) { recentItems = value; }
    public HourlyProduction getHourlyData() { return hourlyData; } public void setHourlyData(HourlyProduction value) { hourlyData = value; }
}
