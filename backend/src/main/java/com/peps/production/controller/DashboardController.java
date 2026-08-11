package com.peps.production.controller;

import com.peps.production.dto.DashboardResponse;
import com.peps.production.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }
    @GetMapping("/dashboard") public ResponseEntity<DashboardResponse> dashboard() { return ResponseEntity.ok(dashboardService.getDashboard()); }
}
