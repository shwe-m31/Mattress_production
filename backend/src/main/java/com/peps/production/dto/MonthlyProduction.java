package com.peps.production.dto;

public class MonthlyProduction {
    private String month;
    private int spring;
    private int hypnos;
    private int total;
    private int target;
    private int king;
    private int queen;
    private int doubleSize;
    private int single;
    
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    
    public int getSpring() { return spring; }
    public void setSpring(int spring) { this.spring = spring; }
    
    public int getHypnos() { return hypnos; }
    public void setHypnos(int hypnos) { this.hypnos = hypnos; }
    
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    
    public int getTarget() { return target; }
    public void setTarget(int target) { this.target = target; }
    
    public int getKing() { return king; }
    public void setKing(int king) { this.king = king; }
    
    public int getQueen() { return queen; }
    public void setQueen(int queen) { this.queen = queen; }
    
    public int getDouble() { return doubleSize; }
    public void setDouble(int doubleSize) { this.doubleSize = doubleSize; }
    
    public int getSingle() { return single; }
    public void setSingle(int single) { this.single = single; }
}