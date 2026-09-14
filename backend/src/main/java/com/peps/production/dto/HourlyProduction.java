package com.peps.production.dto;

public class HourlyProduction {
    private final int[] spring = new int[24]; // 24 hours
    private final int[] hypnos = new int[24]; // 24 hours
    public int[] getSpring() { return spring; }
    public int[] getHypnos() { return hypnos; }
}
