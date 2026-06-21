package com.medical.triage.listener;

import com.medical.triage.event.AppointmentCreatedEvent;
import com.medical.triage.event.CustomerArrivedEvent;
import com.medical.triage.service.GuideTaskService;
import com.medical.triage.service.MessageService;
import com.medical.triage.service.TriageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventListener {

    private final TriageService triageService;
    private final MessageService messageService;
    private final GuideTaskService guideTaskService;

    @Async
    @EventListener
    public void onAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("异步处理预约创建事件, appointmentId: {}", event.getAppointmentId());
        try {
            triageService.triageAppointment(event.getAppointmentId());
            log.info("预约创建后分诊执行完成, appointmentId: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("预约创建后分诊执行失败, appointmentId: {}", event.getAppointmentId(), e);
        }
    }

    @Async
    @EventListener
    public void onCustomerArrived(CustomerArrivedEvent event) {
        log.info("异步处理顾客到店事件, appointmentId: {}", event.getAppointmentId());
        try {
            messageService.sendArrivalNotice(event.getAppointmentId());
            log.info("顾客到店通知发送完成, appointmentId: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("顾客到店通知发送失败, appointmentId: {}", event.getAppointmentId(), e);
        }
    }
}
