package com.peps.production;

import com.peps.production.model.*;
import com.peps.production.repository.ProductionRepository;
import com.peps.production.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for production data foundation
 * Tests historical seeding, live synchronization, persistence, and aggregation
 */
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
    private ProductionSimulationService simulationService;

    @Autowired
    private ProductionTimeService timeService;

    @Autowired
    private SimulationClockService simulationClockService;

    @Autowired
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        repository.deleteAll();
        simulationClockService.reset();
    }

    @Test
    void testHistoricalInitializationOnEmptyDatabase() {
        // Enable simulation for this test
        simulationClockService.initialize();
        
        // Verify database is empty
        assertEquals(0, repository.count(), "Database should be empty initially");
        
        // Check if seeding is needed
        assertTrue(historicalSeedingService.needsSeeding(), "Should need seeding on empty database");
        
        // Seed historical data
        historicalSeedingService.seedHistoricalData();
        
        // Verify data was created
        assertTrue(repository.count() > 0, "Historical data should be created");
        
        // Verify all events have dataSource = "SIMULATOR"
        List<ProductionData> allEvents = repository.findAll();
        for (ProductionData event : allEvents) {
            assertEquals("SIMULATOR", event.getDataSource(), "All historical events should be marked as SIMULATOR");
        }
    }

    @Test
    void testNoDuplicateHistoricalDataOnRestart() {
        // Enable simulation
        simulationClockService.initialize();
        
        // First seeding
        historicalSeedingService.seedHistoricalData();
        long firstCount = repository.count();
        
        // Attempt second seeding (should not duplicate)
        historicalSeedingService.seedHistoricalData();
        long secondCount = repository.count();
        
        assertEquals(firstCount, secondCount, "Historical data should not be duplicated on restart");
    }

    @Test
    void testLiveSynchronizationGeneratesOnlyMissingEvents() {
        // Enable simulation
        simulationClockService.initialize();
        
        // Set specific simulation time during production window
        LocalDateTime testTime = LocalDate.now().atTime(10, 0); // 10:00 AM
        simulationClockService.setSimulationStartTime(testTime.minusHours(1));
        
        // Initial synchronization
        simulationService.synchronizeProduction();
        int firstCount = simulationService.getCurrentProductionCount();
        
        // Second synchronization (should not duplicate)
        simulationService.synchronizeProduction();
        int secondCount = simulationService.getCurrentProductionCount();
        
        assertEquals(firstCount, secondCount, "Live synchronization should not duplicate events");
    }

    @Test
    void testProductionWindowRespected() {
        // Test that production window methods work correctly
        LocalDateTime beforeWindow = LocalDate.now().atTime(8, 0); // 8:00 AM (before 9:00)
        LocalDateTime duringWindow = LocalDate.now().atTime(10, 0); // 10:00 AM
        LocalDateTime afterWindow = LocalDate.now().atTime(18, 0); // 6:00 PM (after 17:00)
        
        assertTrue(timeService.isBeforeProductionStart(beforeWindow), 
            "8:00 AM should be before production start");
        assertTrue(timeService.isWithinProductionWindow(duringWindow), 
            "10:00 AM should be within production window");
        assertTrue(timeService.isAfterProductionEnd(afterWindow), 
            "6:00 PM should be after production end");
    }

    @Test
    void testNoFutureProductionEvents() {
        // Enable simulation
        simulationClockService.initialize();
        
        // Set current time
        LocalDateTime currentTime = LocalDate.now().atTime(10, 0);
        simulationClockService.setSimulationStartTime(currentTime.minusHours(1));
        
        // Synchronize production
        simulationService.synchronizeProduction();
        
        // Verify no events have completion time in the future
        List<ProductionData> todayEvents = repository.findByStatusAndCompletionTimeBetween(
            ProductionStatus.COMPLETED, 
            LocalDate.now().atStartOfDay(), 
            currentTime
        );
        
        for (ProductionData event : todayEvents) {
            assertFalse(event.getCompletionTime().isAfter(currentTime), 
                "No events should have completion time in the future");
        }
    }

    @Test
    void testDailyAggregationFromProductionEvents() {
        // Enable simulation and seed data
        simulationClockService.initialize();
        historicalSeedingService.seedHistoricalData();
        
        // Get events for a specific day
        LocalDate testDate = LocalDate.now().minusDays(5);
        LocalDateTime dayStart = testDate.atStartOfDay();
        LocalDateTime dayEnd = testDate.atTime(23, 59, 59);
        
        List<ProductionData> dayEvents = repository.findByStatusAndCompletionTimeBetween(
            ProductionStatus.COMPLETED, dayStart, dayEnd);
        
        // Calculate sum manually
        int manualSum = dayEvents.stream().mapToInt(ProductionData::getQuantity).sum();
        
        // Get aggregated sum from repository
        int aggregatedSum = repository.sumQuantityByStatusAndCompletionTimeBetween(
            ProductionStatus.COMPLETED, dayStart, dayEnd);
        
        assertEquals(manualSum, aggregatedSum, 
            "Daily aggregation should equal sum of production events");
    }

    @Test
    void testProductTypeDistribution() {
        // Enable simulation and seed data
        simulationClockService.initialize();
        historicalSeedingService.seedHistoricalData();
        
        // Get all events
        List<ProductionData> allEvents = repository.findAll();
        
        // Count by product type
        long springCount = allEvents.stream()
            .filter(e -> e.getProductType() == ProductType.SPRING)
            .count();
        
        long hypnosCount = allEvents.stream()
            .filter(e -> e.getProductType() == ProductType.HYPNOS)
            .count();
        
        long total = springCount + hypnosCount;
        
        // Verify distribution is roughly 58% Spring, 42% Hypnos
        double springRatio = (double) springCount / total;
        assertTrue(springRatio > 0.5 && springRatio < 0.7, 
            "Spring distribution should be around 58%");
    }

    @Test
    void testDashboardDataConsistency() {
        // Enable simulation and seed data
        simulationClockService.initialize();
        historicalSeedingService.seedHistoricalData();
        
        // Get dashboard data
        var dashboard = dashboardService.getDashboard();
        
        // Verify data comes from database
        List<ProductionData> todayEvents = repository.findByStatusAndCompletionTimeBetween(
            ProductionStatus.COMPLETED, 
            timeService.getCurrentDate().atStartOfDay(), 
            timeService.getCurrentTime()
        );
        
        int dbTotal = todayEvents.stream().mapToInt(ProductionData::getQuantity).sum();
        
        assertEquals(dbTotal, dashboard.getTotalProduction(), 
            "Dashboard total should match database sum");
    }

    @Test
    void testEfficiencyCalculation() {
        // Enable simulation
        simulationClockService.initialize();
        
        // Set time during production window
        LocalDateTime testTime = LocalDate.now().atTime(12, 0); // 12:00 PM
        simulationClockService.setSimulationStartTime(testTime.minusHours(3));
        
        // Generate some production
        simulationService.synchronizeProduction();
        
        // Get dashboard data
        var dashboard = dashboardService.getDashboard();
        
        // Verify efficiency is between 0 and 100
        assertTrue(dashboard.getEfficiency() >= 0 && dashboard.getEfficiency() <= 100,
            "Efficiency should be between 0 and 100");
    }

    @Test
    void testSizeBreakdownConsistency() {
        // Enable simulation and seed data
        simulationClockService.initialize();
        historicalSeedingService.seedHistoricalData();
        
        // Get dashboard data
        var dashboard = dashboardService.getDashboard();
        
        // Calculate total from size breakdown
        int sizeBreakdownTotal = dashboard.getSizeBreakdown().values().stream()
            .mapToInt(size -> size.getSpring() + size.getHypnos())
            .sum();
        
        assertEquals(dashboard.getTotalProduction(), sizeBreakdownTotal,
            "Size breakdown total should match dashboard total");
    }

    @Test
    void testRecentProductionOrderedByTime() {
        // Enable simulation and seed data
        simulationClockService.initialize();
        historicalSeedingService.seedHistoricalData();
        
        // Get recent production
        List<ProductionData> recent = repository.findTop10ByStatusOrderByCompletionTimeDesc(
            ProductionStatus.COMPLETED);
        
        // Verify ordering
        for (int i = 0; i < recent.size() - 1; i++) {
            assertTrue(recent.get(i).getCompletionTime().isAfter(recent.get(i + 1).getCompletionTime()) ||
                   recent.get(i).getCompletionTime().isEqual(recent.get(i + 1).getCompletionTime()),
                "Recent production should be ordered by completion time descending");
        }
    }

    @Test
    void testSimulationClockFunctionality() {
        // Test simulation clock is working
        simulationClockService.initialize();
        
        LocalDateTime realTime = simulationClockService.getRealTime();
        LocalDateTime simulatedTime = simulationClockService.getCurrentSimulatedTime();
        
        assertNotNull(realTime, "Real time should not be null");
        assertNotNull(simulatedTime, "Simulated time should not be null");
        
        // If simulation is enabled, times should differ
        if (simulationClockService.isSimulationEnabled()) {
            assertNotEquals(realTime, simulatedTime, 
                "Simulated time should differ from real time when enabled");
        }
    }

    @Test
    void testDataSourceFieldPopulated() {
        // Enable simulation and seed data
        simulationClockService.initialize();
        historicalSeedingService.seedHistoricalData();
        
        // Generate live production
        simulationService.synchronizeProduction();
        
        // Verify all events have dataSource field
        List<ProductionData> allEvents = repository.findAll();
        for (ProductionData event : allEvents) {
            assertNotNull(event.getDataSource(), "All events should have dataSource field");
            assertEquals("SIMULATOR", event.getDataSource(), "All events should be marked as SIMULATOR");
        }
    }
}