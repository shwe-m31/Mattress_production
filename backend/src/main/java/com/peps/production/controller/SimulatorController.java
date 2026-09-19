package com.peps.production.controller;

import com.peps.production.dto.SimulatorStatusResponse;
import com.peps.production.repository.ProductionRepository;
import com.peps.production.service.ContinuousSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {
    private final ProductionRepository repository;
    private final ContinuousSimulationService simulationService;

    public SimulatorController(ProductionRepository repository,
                               ContinuousSimulationService simulationService) {
        this.repository = repository;
        this.simulationService = simulationService;
    }

    @GetMapping("/status")
    public ResponseEntity<SimulatorStatusResponse> getSimulatorStatus() {
        SimulatorStatusResponse status = new SimulatorStatusResponse();
        status.setActive(simulationService.isRunning());
        status.setLastEventTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        status.setEventsGenerated(repository.count());
        status.setStatus(simulationService.isRunning() ? "Continuous Simulation Active" : "Paused");
        return ResponseEntity.ok(status);
    }
}