package com.peps.production.service;

import com.peps.production.model.*;
import com.peps.production.repository.ProductionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProductionSimulatorService implements CommandLineRunner {
    private final ProductionRepository repository;
    @Value("${simulation.seed-hours:10}") private int seedHours;
    public ProductionSimulatorService(ProductionRepository repository) { this.repository = repository; }

    @Override public void run(String... args) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        if (repository.findByStatusAndCompletionTimeBetween(ProductionStatus.COMPLETED, start, start.plusDays(1)).isEmpty()) {
            seedToday();
        }
    }

    @Scheduled(fixedDelayString = "${simulation.interval:10000}", initialDelayString = "${simulation.initial-delay:10000}")
    public void generateProductionEvent() { repository.save(newEvent(LocalDateTime.now())); }

    private void seedToday() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        int endingHour = Math.max(9, Math.min(20, now.getHour()));
        int startingHour = Math.max(9, endingHour - seedHours + 1);
        for (int hour = startingHour; hour <= endingHour; hour++) {
            int events = ThreadLocalRandom.current().nextInt(3, 7);
            for (int event = 0; event < events; event++) {
                LocalDateTime time = today.atTime(hour, ThreadLocalRandom.current().nextInt(0, 60));
                if (time.isAfter(now)) time = now.minusMinutes(ThreadLocalRandom.current().nextInt(1, 10));
                repository.save(newEvent(time));
            }
        }
    }

    private ProductionData newEvent(LocalDateTime time) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ProductType type = random.nextInt(100) < 58 ? ProductType.SPRING : ProductType.HYPNOS;
        MattressSize[] sizes = MattressSize.values();
        return new ProductionData(type, sizes[random.nextInt(sizes.length)], 1, time);
    }
}
