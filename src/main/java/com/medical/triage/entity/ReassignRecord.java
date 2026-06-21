package com.medical.triage.entity;

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
@Table(name = "reassign_record")
public class ReassignRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long guideTaskId;

    private Long appointmentId;

    private Long operatorId;

    private String operatorName;

    private Long oldAssigneeId;

    private Long newAssigneeId;

    private Long oldDepartmentId;

    private Long newDepartmentId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
