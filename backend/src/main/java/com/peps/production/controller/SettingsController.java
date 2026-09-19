package com.peps.production.controller;

import com.peps.production.dto.Settings;
import com.peps.production.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SettingsController {
    private final SettingsService settingsService;
    
    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
    
    @GetMapping("/settings")
    public ResponseEntity<Settings> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }
    
    @PutMapping("/settings")
    public ResponseEntity<Settings> updateSettings(@RequestBody Settings settings) {
        return ResponseEntity.ok(settingsService.updateSettings(settings));
    }
}