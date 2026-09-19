package com.peps.production.service;

import com.peps.production.model.MattressSize;
import com.peps.production.model.ProductType;
import com.peps.production.model.ProductionData;
import com.peps.production.model.ProductionStatus;
import com.peps.production.repository.ProductionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Historical Seeding Service
 * Deterministically initializes past production records if the database is empty.
 * This guarantees that Weekly and Monthly analytics have rich, persistent data
 * without waiting 30 real days, and ensures that restarts never duplicate historical data.
 */
@Service
public class HistoricalSeedingService {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalSeedingService.class);

    private final ProductionRepository repository;

    @Value("${simulation.history-days:30}")
    private int historyDays;

    private static final String[] SPRING_VARIETIES = {"Bonnell", "Pocket", "Offset", "Continuous"};
    private static final String[] HYPNOS_VARIETIES = {"Comfort", "Ortho", "Pillow Top", "Euro Top"};
    private static final String[] SPRING_LINES = {"SPRING-01", "SPRING-02"};
    private static final String[] HYPNOS_LINES = {"HYPNOS-01", "HYPNOS-02"};

    private static final double MIN_CYCLE_TIME = 3.5;
    private static final double MAX_CYCLE_TIME = 5.5;

    public HistoricalSeedingService(ProductionRepository repository) {
        this.repository = repository;
    }

    public boolean needsSeeding() {
        long count = repository.count();
        if (count > 0) {
            logger.info("Database already contains {} production records, skipping historical seeding", count);
            return false;
        }
        return true;
    }

    @Transactional
    public void seedHistoricalData() {
        if (!needsSeeding()) {
            return;
        }

        logger.info("Initializing deterministic historical production dataset for {} days...", historyDays);

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(historyDays);

        List<ProductionData> batch = new ArrayList<>();
        int totalCreated = 0;

        for (LocalDate date = startDate; date.isBefore(today); date = date.plusDays(1)) {
            Random random = new Random(date.toEpochDay());
            
            // Base daily output target: ~480-600 units (8 hours * 60-75/hr)
            int dailyTarget = 450 + random.nextInt(120);
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                dailyTarget = (int) (dailyTarget * 0.4); // weekend maintenance
            }

            LocalDateTime currentTimestamp = date.atTime(LocalTime.of(6, 0));
            LocalDateTime dayEndTimestamp = date.atTime(LocalTime.of(22, 0));

            for (int i = 0; i < dailyTarget && currentTimestamp.isBefore(dayEndTimestamp); i++) {
                double cycle = MIN_CYCLE_TIME + random.nextDouble() * (MAX_CYCLE_TIME - MIN_CYCLE_TIME);
                currentTimestamp = currentTimestamp.plusMinutes((long) cycle).plusSeconds(random.nextInt(40));

                if (currentTimestamp.isAfter(dayEndTimestamp)) {
                    break;
                }

                ProductType type = random.nextDouble() < 0.58 ? ProductType.SPRING : ProductType.HYPNOS;
                MattressSize[] sizes = MattressSize.values();
                MattressSize size = sizes[random.nextInt(sizes.length)];

                String variety = (type == ProductType.SPRING) ?
                        SPRING_VARIETIES[random.nextInt(SPRING_VARIETIES.length)] :
                        HYPNOS_VARIETIES[random.nextInt(HYPNOS_VARIETIES.length)];

                String line = (type == ProductType.SPRING) ?
                        SPRING_LINES[random.nextInt(SPRING_LINES.length)] :
                        HYPNOS_LINES[random.nextInt(HYPNOS_LINES.length)];

                String shift = currentTimestamp.getHour() < 14 ? "Morning Shift" : "Evening Shift";

                ProductionData event = new ProductionData(
                        type,
                        variety,
                        size,
                        line,
                        currentTimestamp.minusMinutes((long) cycle),
                        currentTimestamp,
                        cycle,
                        ProductionStatus.COMPLETED,
                        "HISTORICAL",
                        shift,
                        "SIMULATED"
                );

                batch.add(event);
                if (batch.size() >= 500) {
                    repository.saveAll(batch);
                    totalCreated += batch.size();
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            repository.saveAll(batch);
            totalCreated += batch.size();
            batch.clear();
        }

        logger.info("Historical seeding completed successfully with {} production events", totalCreated);
    }
}