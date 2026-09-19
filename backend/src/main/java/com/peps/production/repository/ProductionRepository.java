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
    List<ProductionData> findByProductionDateAndStatus(LocalDate date, ProductionStatus status);
    List<ProductionData> findByShiftAndStatus(String shift, ProductionStatus status);
    
    /**
     * Count production events (one event = one mattress) for a specific time period
     */
    @Query("SELECT COUNT(p) FROM ProductionData p WHERE p.status = :status AND p.completionTime BETWEEN :start AND :end")
    long countByStatusAndCompletionTimeBetween(@Param("status") ProductionStatus status, 
                                                @Param("start") LocalDateTime start, 
                                                @Param("end") LocalDateTime end);
    
    /**
     * Count production events for a specific product type and time period
     */
    @Query("SELECT COUNT(p) FROM ProductionData p WHERE p.status = :status AND p.productType = :productType AND p.completionTime BETWEEN :start AND :end")
    long countByStatusAndProductTypeAndCompletionTimeBetween(@Param("status") ProductionStatus status,
                                                               @Param("productType") ProductType productType,
                                                               @Param("start") LocalDateTime start,
                                                               @Param("end") LocalDateTime end);
    
    /**
     * Count production events by size for a specific time period
     */
    @Query("SELECT p.size, COUNT(p) FROM ProductionData p WHERE p.status = :status AND p.completionTime BETWEEN :start AND :end GROUP BY p.size")
    List<Object[]> countBySizeAndCompletionTimeBetween(@Param("status") ProductionStatus status,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);
    
    /**
     * Check if any production data exists for a specific date
     * Used for historical seeding validation
     */
    @Query("SELECT COUNT(p) > 0 FROM ProductionData p WHERE p.completionTime BETWEEN :start AND :end")
    boolean existsByCompletionTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * Get count of production records for a specific date
     */
    @Query("SELECT COUNT(p) FROM ProductionData p WHERE p.completionTime BETWEEN :start AND :end")
    long countByCompletionTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * Get the latest completion time for a specific date
     * Used for synchronization validation
     */
    @Query("SELECT MAX(p.completionTime) FROM ProductionData p WHERE p.completionTime BETWEEN :start AND :end")
    LocalDateTime findMaxCompletionTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
