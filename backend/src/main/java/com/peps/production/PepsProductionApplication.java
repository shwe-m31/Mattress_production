package com.peps.production;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PepsProductionApplication {
    public static void main(String[] args) {
        SpringApplication.run(PepsProductionApplication.class, args);
    }
}
