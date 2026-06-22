package com.medical.triage.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleRollbackRequest {

    @NotNull(message = "规则ID不能为空")
    private Long ruleId;

    @NotNull(message = "目标版本ID不能为空")
    private Long targetVersionId;

    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    @NotNull(message = "操作人名称不能为空")
    private String operatorName;
}
