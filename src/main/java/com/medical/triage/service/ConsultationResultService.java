package com.medical.triage.service;

import com.medical.triage.dto.request.ConsultationResultSubmitRequest;
import com.medical.triage.entity.Appointment;
import com.medical.triage.entity.ConsultationResult;
import com.medical.triage.enums.AppointmentStatus;
import com.medical.triage.event.ConsultationCompletedEvent;
import com.medical.triage.repository.AppointmentRepository;
import com.medical.triage.repository.ConsultationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationResultService {

    private final ConsultationResultRepository consultationResultRepository;
    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void submitResult(ConsultationResultSubmitRequest request) {
        log.info("提交面诊结果, appointmentId: {}", request.getAppointmentId());

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + request.getAppointmentId()));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("已取消的预约无法提交面诊结果");
        }

        if (appointment.getCustomerId() == null) {
            throw new IllegalStateException("预约缺少客户信息");
        }

        consultationResultRepository.findByAppointmentId(request.getAppointmentId())
                .ifPresent(existingResult -> {
                    log.warn("该预约已有面诊结果，将覆盖原有结果, appointmentId: {}", request.getAppointmentId());
                    consultationResultRepository.delete(existingResult);
                });

        ConsultationResult result = ConsultationResult.builder()
                .appointmentId(request.getAppointmentId())
                .customerId(appointment.getCustomerId())
                .consultantId(request.getConsultantId())
                .consultantName(request.getConsultantName())
                .doctorId(request.getDoctorId())
                .doctorName(request.getDoctorName())
                .diagnosis(request.getDiagnosis())
                .suggestion(request.getSuggestion())
                .treatmentPlan(request.getTreatmentPlan())
                .followUpDate(request.getFollowUpDate() != null ? request.getFollowUpDate().atStartOfDay() : null)
                .build();

        result = consultationResultRepository.save(result);
        log.info("面诊结果提交成功, resultId: {}, appointmentId: {}", result.getId(), request.getAppointmentId());

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        eventPublisher.publishEvent(new ConsultationCompletedEvent(
                this, appointment.getId(), appointment.getCustomerId(), result.getId()));

        log.info("面诊完成事件已发布, appointmentId: {}", request.getAppointmentId());
    }

    @Transactional(readOnly = true)
    public ConsultationResult getResultByAppointment(Long appointmentId) {
        log.info("查询面诊结果, appointmentId: {}", appointmentId);

        return consultationResultRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("该预约没有面诊结果: " + appointmentId));
    }
}
