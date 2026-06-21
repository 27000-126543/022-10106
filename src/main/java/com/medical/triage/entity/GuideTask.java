package com.medical.triage.entity;

import com.medical.triage.enums.GuideTaskStatus;
import com.medical.triage.enums.GuideTaskType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "guide_task")
public class GuideTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long appointmentId;

    private Long customerId;

    private Long storeId;

    private Long departmentId;

    private Long consultantGroupId;

    @Enumerated(EnumType.STRING)
    private GuideTaskType taskType;

    @Enumerated(EnumType.STRING)
    private GuideTaskStatus status;

    private Long assigneeId;

    private String assigneeName;

    private LocalDateTime dueTime;

    private LocalDateTime completedTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
