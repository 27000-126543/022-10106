package com.medical.triage.dto.response;

import com.medical.triage.enums.ConsultationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStats {

    private ConsultationType consultationType;

    private Integer count;

    private BigDecimal percentage;
}
