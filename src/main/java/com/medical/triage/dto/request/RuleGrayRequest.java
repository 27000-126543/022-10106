package com.medical.triage.dto.request;

import com.medical.triage.enums.AppointmentSource;
import com.medical.triage.enums.ConsultationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleGrayRequest {

    @NotNull(message = "规则ID不能为空")
    private Long ruleId;

    private List<AppointmentSource> graySources;

    private List<ConsultationType> grayProjects;

    private Integer grayPercentage;

    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    @NotNull(message = "操作人名称不能为空")
    private String operatorName;
}
