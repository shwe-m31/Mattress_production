package com.peps.production;

import com.peps.production.acquisition.DataAcquisitionService;
import com.peps.production.acquisition.ProductionEvent;
import com.peps.production.acquisition.SimulatedProductionDataSource;
import com.peps.production.dto.DashboardResponse;
import com.peps.production.model.*;
import com.peps.production.repository.ProductionRepository;
import com.peps.production.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "simulation.enabled=true",
    "simulation.history-days=5"
})
@Transactional
class ProductionDataFoundationTest {

    @Autowired
    private ProductionRepository repository;

    @Autowired
    private HistoricalSeedingService historicalSeedingService;

    @Autowired
    private SimulatedProductionDataSource simulatedDataSource;

    @Autowired
    private DataAcquisitionService dataAcquisitionService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        settingsService.initializeDefaultSettings();
    }

    @Test
    void testHistoricalInitializationOnEmptyDatabase() {
        assertEquals(0, repository.count(), "Database should be empty initially");
        assertTrue(historicalSeedingService.needsSeeding(), "Should need seeding on empty database");

        historicalSeedingService.seedHistoricalData();
        assertTrue(repository.count() > 0, "Historical data should be created");

        // Verify all historical events have completionTime in past
        List<ProductionData> allEvents = repository.findAll();
        for (ProductionData event : allEvents) {
            assertTrue(event.getCompletionTime().isBefore(LocalDateTime.now().plusMinutes(1)));
        }
    }

    @Test
    void testNoDuplicateHistoricalDataOnRestart() {
        historicalSeedingService.seedHistoricalData();
        long firstCount = repository.count();

        // Attempt second seeding
        historicalSeedingService.seedHistoricalData();
        long secondCount = repository.count();

        assertEquals(firstCount, secondCount, "Historical data should not be duplicated on restart");
    }

    @Test
    void testDataAcquisitionSingleMattressEvent() {
        ProductionEvent event = new ProductionEvent(
                ProductType.SPRING,
                MattressSize.QUEEN,
                "Pocket",
                "SPRING-01",
                LocalDateTime.now().minusMinutes(4),
                LocalDateTime.now(),
                4.0,
                ProductionStatus.COMPLETED,
                "SIMULATED"
        );

        ProductionData saved = dataAcquisitionService.ingestEvent(event);
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(ProductType.SPRING, saved.getProductType());
        assertEquals(MattressSize.QUEEN, saved.getSize());

        assertEquals(1, repository.count());
    }

    @Test
    void testDashboardCalculations() {
        historicalSeedingService.seedHistoricalData();
        DashboardResponse dashboard = dashboardService.getDashboard();

        assertNotNull(dashboard);
        assertTrue(dashboard.getShiftTarget() > 0, "Shift target should be dynamically computed");
        assertNotNull(dashboard.getSizeBreakdown());
        assertNotNull(dashboard.getHourlyData());
    }
}