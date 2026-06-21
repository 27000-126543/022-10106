package com.medical.triage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class TriageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TriageServiceApplication.class, args);
    }
}
