package com.peps.production.controller;

import com.peps.production.dto.SimulatorStatusResponse;
import com.peps.production.model.ProductionData;
import com.peps.production.model.ProductType;
import com.peps.production.repository.ProductionRepository;
import com.peps.production.service.ProductionSimulatorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {
    private final ProductionRepository repository;
    private final ProductionSimulatorService simulatorService;
    private final AtomicLong eventsGenerated = new AtomicLong(0);
    private LocalDateTime lastEventTime;
    
    public SimulatorController(ProductionRepository repository, 
                               ProductionSimulatorService simulatorService) {
        this.repository = repository;
        this.simulatorService = simulatorService;
    }
    
    @GetMapping("/status")
    public ResponseEntity<SimulatorStatusResponse> getSimulatorStatus() {
        SimulatorStatusResponse status = new SimulatorStatusResponse();
        status.setActive(true);
        status.setLastEventTime(lastEventTime != null ? 
            lastEventTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "No events yet");
        status.setEventsGenerated(eventsGenerated.get());
        status.setStatus("Running - Generating production events");
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/event")
    public ResponseEntity<ProductionData> triggerSimulatorEvent() {
        ProductionData event = generateDetailedEvent(LocalDateTime.now());
        repository.save(event);
        
        eventsGenerated.incrementAndGet();
        lastEventTime = LocalDateTime.now();
        
        return ResponseEntity.ok(event);
    }
    
    private ProductionData generateDetailedEvent(LocalDateTime time) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ProductType type = random.nextInt(100) < 58 ? ProductType.SPRING : ProductType.HYPNOS;
        
        String[] springVarieties = {"Bonnell", "Pocket", "Offset", "Continuous"};
        String[] hypnosVarieties = {"Comfort", "Ortho", "Pillow Top", "Euro Top"};
        String[] sizes = {"SINGLE", "DOUBLE", "QUEEN", "KING"};
        String[] lines = {"SPRING-01", "SPRING-02", "HYPNOS-01", "HYNPOS-02"};
        String[] statuses = {"COMPLETED", "COMPLETED", "COMPLETED", "IN_PROGRESS", "DELAYED"};
        
        String variety = type == ProductType.SPRING ? 
            springVarieties[random.nextInt(springVarieties.length)] : 
            hypnosVarieties[random.nextInt(hypnosVarieties.length)];
        
        int quantity = random.nextInt(1, 5); // 1-4 units per event
        LocalDateTime startTime = time.minusMinutes(random.nextInt(3, 6));
        
        // Use the simpler constructor and set additional fields
        ProductionData event = new ProductionData(
            type, 
            com.peps.production.model.MattressSize.valueOf(sizes[random.nextInt(sizes.length)]),
            quantity,
            time
        );
        
        event.setVariety(variety);
        event.setProductionLine(lines[random.nextInt(lines.length)]);
        event.setStartTime(startTime);
        event.setCycleTime(random.nextDouble(3.5, 5.5));
        event.setStatus(com.peps.production.model.ProductionStatus.valueOf(statuses[random.nextInt(statuses.length)]));
        event.setProductionTime(time);
        
        return event;
    }
}