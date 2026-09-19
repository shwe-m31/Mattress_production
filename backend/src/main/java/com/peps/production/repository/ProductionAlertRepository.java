package com.peps.production.repository;

import com.peps.production.model.ProductionAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductionAlertRepository extends JpaRepository<ProductionAlert, Long> {
    List<ProductionAlert> findByStatusOrderByCreatedTimestampDesc(String status);
    List<ProductionAlert> findByCreatedTimestampAfterOrderByCreatedTimestampDesc(LocalDateTime timestamp);
    List<ProductionAlert> findByShiftAndStatus(String shift, String status);
}