package com.medical.triage.listener;

import com.medical.triage.entity.ConsultationResult;
import com.medical.triage.event.ConsultationCompletedEvent;
import com.medical.triage.repository.ConsultationResultRepository;
import com.medical.triage.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationEventListener {

    private final MessageService messageService;
    private final ConsultationResultRepository consultationResultRepository;

    @Async
    @EventListener
    public void onConsultationCompleted(ConsultationCompletedEvent event) {
        log.info("异步处理面诊完成事件, appointmentId: {}", event.getAppointmentId());
        try {
            messageService.sendResultNotice(event.getAppointmentId());
            log.info("面诊结果通知发送完成, appointmentId: {}", event.getAppointmentId());

            ConsultationResult result = consultationResultRepository.findById(event.getConsultationResultId()).orElse(null);
            if (result != null) {
                messageService.notifyInternalSystem(event.getAppointmentId(), result);
                log.info("内部系统同步完成, appointmentId: {}", event.getAppointmentId());
            }
        } catch (Exception e) {
            log.error("面诊完成后处理失败, appointmentId: {}", event.getAppointmentId(), e);
        }
    }
}
