package com.medical.triage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "questionnaire_question")
public class QuestionnaireQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long questionnaireId;

    @Column(columnDefinition = "TEXT")
    private String questionText;

    private String questionType;

    private Integer sortOrder;

    private Boolean isRequired;

    @ElementCollection
    private List<String> riskKeywords;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
