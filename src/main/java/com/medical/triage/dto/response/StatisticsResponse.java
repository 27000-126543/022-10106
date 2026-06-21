package com.medical.triage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {

    private Long storeId;

    private String storeName;

    private Long totalCount;

    private Long surgeryCount;

    private Long nonSurgeryCount;

    private Long injectionCount;

    private Long skinCareCount;

    private Long highRiskCount;

    private Long doctorAssessmentCount;

    private String dateRange;
}
