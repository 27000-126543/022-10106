package com.medical.triage.service;

import com.medical.triage.dto.request.QuestionnaireAnswerSubmitRequest;
import com.medical.triage.entity.Questionnaire;
import com.medical.triage.entity.QuestionnaireAnswer;
import com.medical.triage.entity.QuestionnaireQuestion;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.event.QuestionnaireSubmittedEvent;
import com.medical.triage.repository.QuestionnaireAnswerRepository;
import com.medical.triage.repository.QuestionnaireQuestionRepository;
import com.medical.triage.repository.QuestionnaireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionnaireService {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireQuestionRepository questionRepository;
    private final QuestionnaireAnswerRepository answerRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<QuestionnaireQuestion> getQuestions(Long storeId, ConsultationType consultationType) {
        log.info("获取问卷题目, storeId: {}, consultationType: {}", storeId, consultationType);

        Questionnaire questionnaire = questionnaireRepository
                .findByStoreIdAndTargetConsultationTypeAndIsActiveTrue(storeId, consultationType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "门店 " + storeId + " 没有 " + consultationType + " 类型的活跃问卷"));

        return questionRepository.findByQuestionnaireIdOrderBySortOrderAsc(questionnaire.getId());
    }

    @Transactional
    public void submitAnswers(QuestionnaireAnswerSubmitRequest request) {
        log.info("提交问卷答案, appointmentId: {}, 答案数量: {}",
                request.getAppointmentId(), request.getAnswers().size());

        List<QuestionnaireAnswer> existingAnswers = answerRepository.findByAppointmentId(request.getAppointmentId());
        if (!existingAnswers.isEmpty()) {
            log.warn("该预约已有问卷答案，将覆盖原有答案, appointmentId: {}", request.getAppointmentId());
            answerRepository.deleteByAppointmentId(request.getAppointmentId());
        }

        List<QuestionnaireAnswer> answers = new ArrayList<>();
        for (QuestionnaireAnswerSubmitRequest.AnswerItem item : request.getAnswers()) {
            QuestionnaireAnswer answer = QuestionnaireAnswer.builder()
                    .appointmentId(request.getAppointmentId())
                    .customerId(request.getCustomerId())
                    .questionId(item.getQuestionId())
                    .answerText(item.getAnswerText())
                    .build();
            answers.add(answer);
        }

        answerRepository.saveAll(answers);
        log.info("问卷答案提交成功, appointmentId: {}", request.getAppointmentId());

        eventPublisher.publishEvent(new QuestionnaireSubmittedEvent(
                this, request.getAppointmentId(), request.getCustomerId(), null));
    }

    @Transactional(readOnly = true)
    public List<QuestionnaireAnswer> getAnswersByAppointment(Long appointmentId) {
        log.info("获取预约的问卷答案, appointmentId: {}", appointmentId);
        return answerRepository.findByAppointmentId(appointmentId);
    }
}
