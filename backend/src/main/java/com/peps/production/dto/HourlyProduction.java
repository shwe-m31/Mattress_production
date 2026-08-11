package com.peps.production.dto;

public class HourlyProduction {
    private final int[] spring = new int[12];
    private final int[] hypnos = new int[12];
    public int[] getSpring() { return spring; }
    public int[] getHypnos() { return hypnos; }
}
