package com.medical.triage.event;

import com.medical.triage.enums.GuideTaskStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GuideTaskStatusChangedEvent extends ApplicationEvent {

    private final Long taskId;
    private final Long appointmentId;
    private final Long customerId;
    private final GuideTaskStatus fromStatus;
    private final GuideTaskStatus toStatus;

    public GuideTaskStatusChangedEvent(Object source, Long taskId, Long appointmentId, Long customerId,
                                        GuideTaskStatus fromStatus, GuideTaskStatus toStatus) {
        super(source);
        this.taskId = taskId;
        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }
}
