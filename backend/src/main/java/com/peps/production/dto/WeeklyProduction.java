package com.peps.production.dto;

public class WeeklyProduction {
    private String week;
    private int spring;
    private int hypnos;
    private int total;
    private int target;
    private int efficiency;
    
    public String getWeek() { return week; }
    public void setWeek(String week) { this.week = week; }
    
    public int getSpring() { return spring; }
    public void setSpring(int spring) { this.spring = spring; }
    
    public int getHypnos() { return hypnos; }
    public void setHypnos(int hypnos) { this.hypnos = hypnos; }
    
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    
    public int getTarget() { return target; }
    public void setTarget(int target) { this.target = target; }
    
    public int getEfficiency() { return efficiency; }
    public void setEfficiency(int efficiency) { this.efficiency = efficiency; }
}