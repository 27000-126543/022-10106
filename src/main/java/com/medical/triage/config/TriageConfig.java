package com.medical.triage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "triage")
public class TriageConfig {

    private Risk risk = new Risk();

    private Message message = new Message();

    @Data
    public static class Risk {
        private List<String> highRiskKeywords;
        private List<String> autoUpgradeToDoctorKeywords;
    }

    @Data
    public static class Message {
        private Integer waitReminderMinutes;
        private Boolean mockEnabled;
    }
}
