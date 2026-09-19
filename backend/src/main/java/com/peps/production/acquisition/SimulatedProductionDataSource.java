package com.peps.production.acquisition;

import com.peps.production.model.MattressSize;
import com.peps.production.model.ProductType;
import com.peps.production.model.ProductionStatus;
import com.peps.production.model.ProductionTarget;
import com.peps.production.model.ShiftConfiguration;
import com.peps.production.repository.ProductionTargetRepository;
import com.peps.production.service.DowntimeSimulationService;
import com.peps.production.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Simulated production data source implementation.
 * Produces discrete production completion events according to configured targets and shift schedules.
 */
@Component
public class SimulatedProductionDataSource implements ProductionDataSource {
    private static final Logger logger = LoggerFactory.getLogger(SimulatedProductionDataSource.class);

    private final ProductionTargetRepository targetRepository;
    private final SettingsService settingsService;
    private final DowntimeSimulationService downtimeSimulationService;

    // Variety definitions
    private static final String[] SPRING_VARIETIES = {"Bonnell", "Pocket", "Offset", "Continuous"};
    private static final String[] HYPNOS_VARIETIES = {"Comfort", "Ortho", "Pillow Top", "Euro Top"};

    // Line definitions
    private static final String[] SPRING_LINES = {"SPRING-01", "SPRING-02"};
    private static final String[] HYPNOS_LINES = {"HYPNOS-01", "HYPNOS-02"};

    private static final double MIN_CYCLE_TIME = 3.5;
    private static final double MAX_CYCLE_TIME = 5.5;

    public SimulatedProductionDataSource(ProductionTargetRepository targetRepository,
                                         SettingsService settingsService,
                                         DowntimeSimulationService downtimeSimulationService) {
        this.targetRepository = targetRepository;
        this.settingsService = settingsService;
        this.downtimeSimulationService = downtimeSimulationService;
    }

    @Override
    public String getSourceMode() {
        return "SIMULATED";
    }

    @Override
    public List<ProductionEvent> fetchNextProductionEvents() {
        List<ProductionEvent> events = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. Check if current time is within an active shift
        Optional<ShiftConfiguration> currentShift = settingsService.getCurrentShift();
        if (currentShift.isEmpty()) {
            logger.debug("Outside active shift hours, no simulated events generated");
            return events;
        }

        // 2. Fetch all configured targets
        List<ProductionTarget> targets = targetRepository.findAll();
        int totalHourlyTarget = targets.stream().mapToInt(ProductionTarget::getHourlyTarget).sum();
        if (totalHourlyTarget <= 0) {
            logger.debug("Configured hourly target is 0, no events generated");
            return events;
        }

        // 3. Check downtime state
        if (downtimeSimulationService.isCurrentlyInDowntime()) {
            logger.debug("Currently in downtime stoppage, production paused");
            return events;
        }

        // Check if we should trigger a new downtime event
        if (downtimeSimulationService.shouldSimulateDowntime(now)) {
            downtimeSimulationService.startDowntime(now);
            logger.info("Simulated downtime initiated at {}", now);
            return events;
        }

        // 4. Calculate events to produce in this 1-minute step:
        // rate = totalHourlyTarget / 60
        double eventsPerMinute = (double) totalHourlyTarget / 60.0;
        int count = (int) Math.floor(eventsPerMinute);
        double remainder = eventsPerMinute - count;

        long seed = now.atZone(ZoneId.systemDefault()).toEpochSecond();
        Random random = new Random(seed);

        if (random.nextDouble() < remainder) {
            count++;
        }

        for (int i = 0; i < count; i++) {
            ProductionEvent event = generateTargetWeightedEvent(targets, now, random, i);
            if (event != null) {
                events.add(event);
            }
        }

        return events;
    }

    /**
     * Pick product type and size weighted according to the 8 configured targets.
     */
    private ProductionEvent generateTargetWeightedEvent(List<ProductionTarget> targets, LocalDateTime now, Random random, int eventOffset) {
        int totalTarget = targets.stream().mapToInt(ProductionTarget::getHourlyTarget).sum();
        if (totalTarget <= 0) {
            return null;
        }

        int targetRoll = random.nextInt(totalTarget);
        int runningSum = 0;
        ProductType selectedType = ProductType.SPRING;
        MattressSize selectedSize = MattressSize.QUEEN;

        for (ProductionTarget target : targets) {
            runningSum += target.getHourlyTarget();
            if (targetRoll < runningSum) {
                selectedType = target.getProductType();
                selectedSize = target.getSize();
                break;
            }
        }

        String variety;
        String line;
        if (selectedType == ProductType.SPRING) {
            variety = SPRING_VARIETIES[random.nextInt(SPRING_VARIETIES.length)];
            line = SPRING_LINES[random.nextInt(SPRING_LINES.length)];
        } else {
            variety = HYPNOS_VARIETIES[random.nextInt(HYPNOS_VARIETIES.length)];
            line = HYPNOS_LINES[random.nextInt(HYPNOS_LINES.length)];
        }

        double cycleTime = MIN_CYCLE_TIME + random.nextDouble() * (MAX_CYCLE_TIME - MIN_CYCLE_TIME);
        LocalDateTime completionTime = now.plusSeconds(eventOffset * 5L);
        LocalDateTime startTime = completionTime.minusMinutes((long) cycleTime);

        return new ProductionEvent(
                selectedType,
                selectedSize,
                variety,
                line,
                startTime,
                completionTime,
                cycleTime,
                ProductionStatus.COMPLETED,
                "SIMULATED"
        );
    }
}
