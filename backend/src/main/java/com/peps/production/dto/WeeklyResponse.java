package com.peps.production.dto;

import java.util.List;

public class WeeklyResponse {
    private List<WeeklyProduction> weeklySummary;
    private List<ProductFulfilmentDto> productFulfilment;

    public WeeklyResponse() {}

    public WeeklyResponse(List<WeeklyProduction> weeklySummary, List<ProductFulfilmentDto> productFulfilment) {
        this.weeklySummary = weeklySummary;
        this.productFulfilment = productFulfilment;
    }

    public List<WeeklyProduction> getWeeklySummary() { return weeklySummary; }
    public void setWeeklySummary(List<WeeklyProduction> weeklySummary) { this.weeklySummary = weeklySummary; }

    public List<ProductFulfilmentDto> getProductFulfilment() { return productFulfilment; }
    public void setProductFulfilment(List<ProductFulfilmentDto> productFulfilment) { this.productFulfilment = productFulfilment; }
}
