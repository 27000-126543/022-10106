package com.medical.triage.dto.response;

import com.medical.triage.enums.GuideTaskStatus;
import com.medical.triage.enums.GuideTaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideTaskResponse {

    private Long id;

    private Long appointmentId;

    private String customerName;

    private GuideTaskType taskType;

    private GuideTaskStatus status;

    private String assigneeName;

    private String departmentName;

    private LocalDateTime dueTime;

    private LocalDateTime createdAt;
}
