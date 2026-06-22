package com.medical.triage.dto.response;

import com.medical.triage.enums.AppointmentSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelStats {

    private AppointmentSource source;

    private Integer count;

    private BigDecimal percentage;
}
