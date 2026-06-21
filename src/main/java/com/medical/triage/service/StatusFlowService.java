package com.medical.triage.service;

import com.medical.triage.dto.request.StatusUpdateRequest;
import com.medical.triage.entity.Appointment;
import com.medical.triage.entity.GuideTask;
import com.medical.triage.entity.StatusRecord;
import com.medical.triage.enums.AppointmentStatus;
import com.medical.triage.repository.AppointmentRepository;
import com.medical.triage.repository.GuideTaskRepository;
import com.medical.triage.repository.StatusRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusFlowService {

    private final StatusRecordRepository statusRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final GuideTaskRepository guideTaskRepository;

    @Transactional
    public void recordStatusChange(StatusUpdateRequest request) {
        log.info("记录状态变更, appointmentId: {}, toStatus: {}",
                request.getAppointmentId(), request.getToStatus());

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + request.getAppointmentId()));

        String fromStatus = appointment.getStatus() != null ? appointment.getStatus().name() : null;

        StatusRecord record = StatusRecord.builder()
                .appointmentId(request.getAppointmentId())
                .guideTaskId(request.getGuideTaskId())
                .operatorId(request.getOperatorId())
                .operatorName(request.getOperatorName())
                .operatorRole(request.getOperatorRole())
                .fromStatus(fromStatus)
                .toStatus(request.getToStatus().name())
                .remark(request.getRemark())
                .build();

        statusRecordRepository.save(record);

        appointment.setStatus(request.getToStatus());
        appointmentRepository.save(appointment);

        if (request.getGuideTaskId() != null) {
            GuideTask task = guideTaskRepository.findById(request.getGuideTaskId()).orElse(null);
            if (task != null) {
                log.debug("关联导诊任务状态更新, taskId: {}", request.getGuideTaskId());
            }
        }

        log.info("状态变更记录成功, appointmentId: {}, from: {}, to: {}",
                request.getAppointmentId(), fromStatus, request.getToStatus());
    }

    @Transactional(readOnly = true)
    public List<StatusRecord> getStatusHistory(Long appointmentId) {
        log.info("获取状态历史, appointmentId: {}", appointmentId);
        return statusRecordRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId);
    }

    @Transactional
    public void updateAppointmentStatus(Long appointmentId, AppointmentStatus status,
                                        Long operatorId, String operatorName,
                                        String operatorRole, String remark) {
        log.info("更新预约状态, appointmentId: {}, status: {}", appointmentId, status);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + appointmentId));

        String fromStatus = appointment.getStatus() != null ? appointment.getStatus().name() : null;

        if (appointment.getStatus() == status) {
            log.warn("预约状态未变更, appointmentId: {}, status: {}", appointmentId, status);
            return;
        }

        StatusRecord record = StatusRecord.builder()
                .appointmentId(appointmentId)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .operatorRole(operatorRole)
                .fromStatus(fromStatus)
                .toStatus(status.name())
                .remark(remark)
                .build();
        statusRecordRepository.save(record);

        appointment.setStatus(status);
        appointmentRepository.save(appointment);

        log.info("预约状态更新成功, appointmentId: {}, from: {}, to: {}",
                appointmentId, fromStatus, status);
    }
}
