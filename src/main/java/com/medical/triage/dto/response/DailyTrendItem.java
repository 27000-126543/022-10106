package com.medical.triage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTrendItem {

    private LocalDate date;

    private Integer totalCount;

    private Integer surgeryCount;

    private Integer highRiskCount;

    private Integer doctorAssessmentCount;
}
