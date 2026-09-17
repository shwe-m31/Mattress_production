package com.peps.production.repository;

import com.peps.production.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductionRepository extends JpaRepository<ProductionData, Long> {
    List<ProductionData> findByStatusAndCompletionTimeBetween(ProductionStatus status, LocalDateTime start, LocalDateTime end);
    List<ProductionData> findTop10ByStatusOrderByCompletionTimeDesc(ProductionStatus status);
    
    /**
     * Count total production quantity for a specific time period
     */
    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM ProductionData p WHERE p.status = :status AND p.completionTime BETWEEN :start AND :end")
    int sumQuantityByStatusAndCompletionTimeBetween(@Param("status") ProductionStatus status, 
                                                     @Param("start") LocalDateTime start, 
                                                     @Param("end") LocalDateTime end);
    
    /**
     * Count total production quantity for a specific product type and time period
     */
    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM ProductionData p WHERE p.status = :status AND p.productType = :productType AND p.completionTime BETWEEN :start AND :end")
    int sumQuantityByStatusAndProductTypeAndCompletionTimeBetween(@Param("status") ProductionStatus status,
                                                                    @Param("productType") ProductType productType,
                                                                    @Param("start") LocalDateTime start,
                                                                    @Param("end") LocalDateTime end);
}
