package com.medical.triage.listener;

import com.medical.triage.engine.RiskCheckEngine;
import com.medical.triage.event.QuestionnaireSubmittedEvent;
import com.medical.triage.service.TriageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionnaireEventListener {

    private final TriageService triageService;
    private final RiskCheckEngine riskCheckEngine;

    @Async
    @EventListener
    public void onQuestionnaireSubmitted(QuestionnaireSubmittedEvent event) {
        log.info("异步处理问卷提交事件, appointmentId: {}", event.getAppointmentId());
        try {
            triageService.triageAppointment(event.getAppointmentId());
            log.info("问卷提交后重新分诊执行完成, appointmentId: {}", event.getAppointmentId());

            riskCheckEngine.checkRisk(event.getStoreId(), event.getAppointmentId(), null);
            log.info("问卷提交后风险校验执行完成, appointmentId: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("问卷提交后处理失败, appointmentId: {}", event.getAppointmentId(), e);
        }
    }
}
