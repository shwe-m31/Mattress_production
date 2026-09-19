package com.peps.production.acquisition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * External production data source abstraction.
 * Serves as the extension point for future PLC, OPC-UA, or Modbus TCP drivers.
 * In this prototype, it provides a clean interface without fabricating fake hardware signals.
 */
@Component
public class ExternalProductionDataSource implements ProductionDataSource {
    private static final Logger logger = LoggerFactory.getLogger(ExternalProductionDataSource.class);

    @Override
    public String getSourceMode() {
        return "DATA_SOURCE";
    }

    @Override
    public List<ProductionEvent> fetchNextProductionEvents() {
        logger.debug("External Data Source mode active: awaiting live external PLC/HMI telemetry");
        // In prototype, external industrial connection is ready for future integration drivers
        return Collections.emptyList();
    }
}
