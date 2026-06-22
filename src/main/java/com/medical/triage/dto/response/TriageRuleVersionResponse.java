package com.medical.triage.dto.response;

import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.DepartmentType;
import com.medical.triage.enums.RuleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageRuleVersionResponse {

    private Long id;

    private Long ruleId;

    private String ruleCode;

    private Integer version;

    private String name;

    private Long storeId;

    private ConsultationType consultationType;

    private DepartmentType departmentType;

    private List<String> keywords;

    private Integer priority;

    private Integer minAge;

    private Integer maxAge;

    private String gender;

    private Boolean isEnabled;

    private RuleStatus status;

    private String createdBy;

    private LocalDateTime createdAt;

    private String description;
}
