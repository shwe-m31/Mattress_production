package com.peps.production.repository;

import com.peps.production.model.MattressSize;
import com.peps.production.model.ProductType;
import com.peps.production.model.ProductionTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionTargetRepository extends JpaRepository<ProductionTarget, Long> {
    Optional<ProductionTarget> findByProductTypeAndSize(ProductType productType, MattressSize size);
    List<ProductionTarget> findByProductType(ProductType productType);
    List<ProductionTarget> findAll();
}