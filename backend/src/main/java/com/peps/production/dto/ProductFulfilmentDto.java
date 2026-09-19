package com.peps.production.dto;

public class ProductFulfilmentDto {
    private String product;
    private int planned;
    private int fulfilled;

    public ProductFulfilmentDto() {}

    public ProductFulfilmentDto(String product, int planned, int fulfilled) {
        this.product = product;
        this.planned = planned;
        this.fulfilled = fulfilled;
    }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public int getPlanned() { return planned; }
    public void setPlanned(int planned) { this.planned = planned; }

    public int getFulfilled() { return fulfilled; }
    public void setFulfilled(int fulfilled) { this.fulfilled = fulfilled; }
}
