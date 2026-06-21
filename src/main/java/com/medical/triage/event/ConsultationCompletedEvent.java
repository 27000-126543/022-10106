package com.medical.triage.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ConsultationCompletedEvent extends ApplicationEvent {

    private final Long appointmentId;
    private final Long customerId;
    private final Long consultationResultId;

    public ConsultationCompletedEvent(Object source, Long appointmentId, Long customerId,
                                       Long consultationResultId) {
        super(source);
        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.consultationResultId = consultationResultId;
    }
}
