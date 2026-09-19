package com.peps.production.acquisition;

import com.peps.production.model.*;
import com.peps.production.repository.ProductionRepository;
import com.peps.production.service.AlertService;
import com.peps.production.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Acquisition Service
 * The unified pipeline that receives incoming production events from either
 * Simulated or Data Source modes, validates, assigns metadata, persists to MySQL,
 * and feeds the analytics/alerting layer.
 */
@Service
public class DataAcquisitionService {
    private static final Logger logger = LoggerFactory.getLogger(DataAcquisitionService.class);

    private final ProductionRepository productionRepository;
    private final SettingsService settingsService;
    private final AlertService alertService;

    public DataAcquisitionService(ProductionRepository productionRepository,
                                  SettingsService settingsService,
                                  AlertService alertService) {
        this.productionRepository = productionRepository;
        this.settingsService = settingsService;
        this.alertService = alertService;
    }

    /**
     * Ingest and process a single production event
     */
    @Transactional
    public ProductionData ingestEvent(ProductionEvent event) {
        if (event == null) {
            return null;
        }

        // Validate product type
        if (event.getProductType() == null) {
            logger.warn("Discarding production event: missing ProductType");
            return null;
        }

        // Validate size
        if (event.getSize() == null) {
            logger.warn("Discarding production event: missing MattressSize");
            return null;
        }

        LocalDateTime completionTime = event.getCompletionTime() != null ? event.getCompletionTime() : LocalDateTime.now();
        LocalDateTime startTime = event.getStartTime() != null ? event.getStartTime() : completionTime.minusMinutes(4);
        Double cycleTime = event.getCycleTime() != null ? event.getCycleTime() : 4.0;
        ProductionStatus status = event.getStatus() != null ? event.getStatus() : ProductionStatus.COMPLETED;
        String variety = event.getVariety() != null ? event.getVariety() : "Standard";
        String line = event.getProductionLine() != null ? event.getProductionLine() : 
                      (event.getProductType() == ProductType.SPRING ? "SPRING-01" : "HYPNOS-01");
        String sourceMode = event.getSourceMode() != null ? event.getSourceMode() : "SIMULATED";

        // Determine current active shift
        Optional<ShiftConfiguration> currentShift = settingsService.getCurrentShift();
        String shiftName = currentShift.map(ShiftConfiguration::getShiftName).orElse("Standard Shift");

        ProductionData productionData = new ProductionData(
                event.getProductType(),
                variety,
                event.getSize(),
                line,
                startTime,
                completionTime,
                cycleTime,
                status,
                sourceMode,
                shiftName,
                sourceMode
        );

        ProductionData saved = productionRepository.save(productionData);
        logger.debug("Ingested production event #{}: {} {} on {} ({})",
                saved.getId(), saved.getProductType(), saved.getSize(), saved.getProductionLine(), saved.getCompletionTime());

        return saved;
    }

    /**
     * Ingest a batch of production events
     */
    @Transactional
    public List<ProductionData> ingestEvents(List<ProductionEvent> events) {
        List<ProductionData> savedList = new ArrayList<>();
        if (events == null || events.isEmpty()) {
            return savedList;
        }

        for (ProductionEvent event : events) {
            ProductionData saved = ingestEvent(event);
            if (saved != null) {
                savedList.add(saved);
            }
        }
        return savedList;
    }
}
