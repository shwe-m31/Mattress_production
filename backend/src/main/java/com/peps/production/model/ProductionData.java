package com.peps.production.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_data")
public class ProductionData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ProductType productType;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private MattressSize size;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false)
    private LocalDateTime completionTime;
    @Column(nullable = false)
    private LocalDateTime productionTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ProductionStatus status;

    protected ProductionData() { }
    public ProductionData(ProductType productType, MattressSize size, int quantity, LocalDateTime completionTime) {
        this.productType = productType; this.size = size; this.quantity = quantity;
        this.completionTime = completionTime; this.productionTime = completionTime;
        this.status = ProductionStatus.COMPLETED;
    }
    public Long getId() { return id; }
    public ProductType getProductType() { return productType; }
    public MattressSize getSize() { return size; }
    public int getQuantity() { return quantity; }
    public LocalDateTime getCompletionTime() { return completionTime; }
    public ProductionStatus getStatus() { return status; }
}
