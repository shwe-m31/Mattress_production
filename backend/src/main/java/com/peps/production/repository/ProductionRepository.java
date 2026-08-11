package com.peps.production.repository;

import com.peps.production.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductionRepository extends JpaRepository<ProductionData, Long> {
    List<ProductionData> findByStatusAndCompletionTimeBetween(ProductionStatus status, LocalDateTime start, LocalDateTime end);
    List<ProductionData> findTop10ByStatusOrderByCompletionTimeDesc(ProductionStatus status);
}
