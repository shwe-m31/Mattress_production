package com.peps.production.repository;

import com.peps.production.model.ShiftConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftConfigurationRepository extends JpaRepository<ShiftConfiguration, Long> {
    Optional<ShiftConfiguration> findByShiftName(String shiftName);
    List<ShiftConfiguration> findByActiveTrueOrderByStartTime();
    List<ShiftConfiguration> findAll();
}