package com.peps.production;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class PepsProductionApplication {
    public static void main(String[] args) {
        // Set default timezone to Asia/Kolkata for consistent time handling
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(PepsProductionApplication.class, args);
    }
}
