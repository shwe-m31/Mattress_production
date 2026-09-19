package com.peps.production.acquisition;

import java.util.List;

/**
 * Abstraction layer for production data sources.
 * In Simulated Mode, SimulatedProductionDataSource generates events based on configured targets.
 * In Data Source Mode, ExternalProductionDataSource can connect to industrial interfaces (OPC-UA, Modbus, etc.).
 */
public interface ProductionDataSource {
    /**
     * Poll or produce the next batch of completed mattress production events.
     * Returns empty list if no new event occurred or during downtime/off-shift.
     */
    List<ProductionEvent> fetchNextProductionEvents();
    
    /**
     * Returns the name / mode identifier of this data source ("SIMULATED" or "DATA_SOURCE").
     */
    String getSourceMode();
}
