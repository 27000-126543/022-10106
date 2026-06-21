package com.medical.triage.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TriageCompletedEvent extends ApplicationEvent {

    private final Long triageResultId;
    private final Long appointmentId;
    private final Long customerId;
    private final Long storeId;

    public TriageCompletedEvent(Object source, Long triageResultId, Long appointmentId,
                                 Long customerId, Long storeId) {
        super(source);
        this.triageResultId = triageResultId;
        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.storeId = storeId;
    }
}
