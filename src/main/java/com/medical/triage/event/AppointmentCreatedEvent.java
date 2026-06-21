package com.medical.triage.event;

import com.medical.triage.enums.ConsultationType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AppointmentCreatedEvent extends ApplicationEvent {

    private final Long appointmentId;
    private final Long customerId;
    private final Long storeId;
    private final ConsultationType consultationType;

    public AppointmentCreatedEvent(Object source, Long appointmentId, Long customerId,
                                    Long storeId, ConsultationType consultationType) {
        super(source);
        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.storeId = storeId;
        this.consultationType = consultationType;
    }
}
