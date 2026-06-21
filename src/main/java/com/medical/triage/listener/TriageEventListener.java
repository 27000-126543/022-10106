package com.medical.triage.listener;

import com.medical.triage.entity.TriageResult;
import com.medical.triage.enums.RiskLevel;
import com.medical.triage.event.TriageCompletedEvent;
import com.medical.triage.repository.TriageResultRepository;
import com.medical.triage.service.GuideTaskService;
import com.medical.triage.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TriageEventListener {

    private final MessageService messageService;
    private final GuideTaskService guideTaskService;
    private final TriageResultRepository triageResultRepository;

    @Async
    @EventListener
    public void onTriageCompleted(TriageCompletedEvent event) {
        log.info("异步处理分诊完成事件, appointmentId: {}", event.getAppointmentId());
        try {
            messageService.sendWaitReminder(event.getAppointmentId());
            log.info("分诊完成通知发送完成, appointmentId: {}", event.getAppointmentId());

            TriageResult triageResult = triageResultRepository.findById(event.getTriageResultId()).orElse(null);
            if (triageResult != null) {
                boolean needDoctor = triageResult.getRiskLevel() == RiskLevel.HIGH
                        || triageResult.getRiskLevel() == RiskLevel.EXTREME;
                guideTaskService.createGuideTasks(triageResult, needDoctor);
                log.info("分诊完成后导诊任务创建完成, appointmentId: {}", event.getAppointmentId());
            }
        } catch (Exception e) {
            log.error("分诊完成后处理失败, appointmentId: {}", event.getAppointmentId(), e);
        }
    }
}
