package com.peps.production.dto;

public class DailyProduction {
    private String date;
    private int spring;
    private int hypnos;
    private int total;
    private int king;
    private int queen;
    private int doubleSize;
    private int single;
    private int efficiency;
    private int downtime;
    
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public int getSpring() { return spring; }
    public void setSpring(int spring) { this.spring = spring; }
    
    public int getHypnos() { return hypnos; }
    public void setHypnos(int hypnos) { this.hypnos = hypnos; }
    
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    
    public int getKing() { return king; }
    public void setKing(int king) { this.king = king; }
    
    public int getQueen() { return queen; }
    public void setQueen(int queen) { this.queen = queen; }
    
    public int getDouble() { return doubleSize; }
    public void setDouble(int doubleSize) { this.doubleSize = doubleSize; }
    
    public int getSingle() { return single; }
    public void setSingle(int single) { this.single = single; }
    
    public int getEfficiency() { return efficiency; }
    public void setEfficiency(int efficiency) { this.efficiency = efficiency; }
    
    public int getDowntime() { return downtime; }
    public void setDowntime(int downtime) { this.downtime = downtime; }
}