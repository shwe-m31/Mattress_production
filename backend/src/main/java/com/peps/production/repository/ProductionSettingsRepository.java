package com.peps.production.repository;

import com.peps.production.model.ProductionSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductionSettingsRepository extends JpaRepository<ProductionSettings, Long> {
    Optional<ProductionSettings> findBySettingsKey(String settingsKey);
}