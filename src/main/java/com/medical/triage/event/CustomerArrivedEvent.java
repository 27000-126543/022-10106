package com.medical.triage.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class CustomerArrivedEvent extends ApplicationEvent {

    private final Long appointmentId;
    private final Long customerId;
    private final Long storeId;
    private final LocalDateTime arrivedTime;

    public CustomerArrivedEvent(Object source, Long appointmentId, Long customerId,
                                 Long storeId, LocalDateTime arrivedTime) {
        super(source);
        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.storeId = storeId;
        this.arrivedTime = arrivedTime;
    }
}
