package com.medical.triage.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class QuestionnaireSubmittedEvent extends ApplicationEvent {

    private final Long appointmentId;
    private final Long customerId;
    private final Long storeId;

    public QuestionnaireSubmittedEvent(Object source, Long appointmentId, Long customerId, Long storeId) {
        super(source);
        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.storeId = storeId;
    }
}
