package com.peps.production.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_targets", indexes = {
    @Index(name = "idx_product_type_size", columnList = "product_type, size")
})
public class ProductionTarget {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ProductType productType;
    
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private MattressSize size;
    
    @Column(nullable = false)
    private int hourlyTarget;
    
    @Column(nullable = false)
    private LocalDateTime lastUpdated;
    
    public ProductionTarget() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    public ProductionTarget(ProductType productType, MattressSize size, int hourlyTarget) {
        this.productType = productType;
        this.size = size;
        this.hourlyTarget = hourlyTarget;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }
    
    public MattressSize getSize() { return size; }
    public void setSize(MattressSize size) { this.size = size; }
    
    public int getHourlyTarget() { return hourlyTarget; }
    public void setHourlyTarget(int hourlyTarget) { this.hourlyTarget = hourlyTarget; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}