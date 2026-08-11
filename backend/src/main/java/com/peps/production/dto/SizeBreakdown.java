package com.peps.production.dto;

public class SizeBreakdown {
    private int spring;
    private int hypnos;
    public int getSpring() { return spring; }
    public int getHypnos() { return hypnos; }
    public void addSpring(int quantity) { spring += quantity; }
    public void addHypnos(int quantity) { hypnos += quantity; }
}
