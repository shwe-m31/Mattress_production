package com.peps.production.controller;

import com.peps.production.dto.Settings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SettingsController {
    // In-memory settings storage (in production, this would be in database)
    private Settings currentSettings = new Settings();
    
    @GetMapping("/settings")
    public ResponseEntity<Settings> getSettings() {
        return ResponseEntity.ok(currentSettings);
    }
    
    @PutMapping("/settings")
    public ResponseEntity<Settings> updateSettings(@RequestBody Settings settings) {
        currentSettings.setRefreshInterval(settings.getRefreshInterval());
        currentSettings.setTvMode(settings.isTvMode());
        currentSettings.setAutoRotate(settings.isAutoRotate());
        currentSettings.setRotateInterval(settings.getRotateInterval());
        currentSettings.setSimulatorMode(settings.isSimulatorMode());
        currentSettings.setLastUpdated(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return ResponseEntity.ok(currentSettings);
    }
}