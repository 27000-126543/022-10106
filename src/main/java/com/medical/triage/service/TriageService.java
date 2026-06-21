package com.medical.triage.service;

import com.medical.triage.dto.response.TriageResultResponse;
import com.medical.triage.entity.Appointment;
import com.medical.triage.entity.ConsultantGroup;
import com.medical.triage.entity.Customer;
import com.medical.triage.entity.Department;
import com.medical.triage.entity.Store;
import com.medical.triage.entity.TriageResult;
import com.medical.triage.enums.AppointmentStatus;
import com.medical.triage.enums.RiskLevel;
import com.medical.triage.enums.TriageStatus;
import com.medical.triage.engine.TriageEngine;
import com.medical.triage.event.TriageCompletedEvent;
import com.medical.triage.repository.AppointmentRepository;
import com.medical.triage.repository.ConsultantGroupRepository;
import com.medical.triage.repository.CustomerRepository;
import com.medical.triage.repository.DepartmentRepository;
import com.medical.triage.repository.StoreRepository;
import com.medical.triage.repository.TriageResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriageService {

    private final TriageEngine triageEngine;
    private final TriageResultRepository triageResultRepository;
    private final GuideTaskService guideTaskService;
    private final StatusFlowService statusFlowService;
    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final DepartmentRepository departmentRepository;
    private final ConsultantGroupRepository consultantGroupRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TriageResultResponse triageAppointment(Long appointmentId) {
        log.info("执行分诊, appointmentId: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("已取消的预约无法分诊");
        }

        if (triageResultRepository.existsByAppointmentId(appointmentId)) {
            log.warn("该预约已分诊，将覆盖原有分诊结果, appointmentId: {}", appointmentId);
            triageResultRepository.findByAppointmentId(appointmentId)
                    .ifPresent(triageResultRepository::delete);
        }

        TriageResult triageResult = triageEngine.executeTriage(appointmentId);
        triageResult = triageResultRepository.save(triageResult);

        boolean needDoctor = triageResult.getRiskLevel() == RiskLevel.HIGH
                || triageResult.getRiskLevel() == RiskLevel.EXTREME;

        guideTaskService.createGuideTasks(triageResult, needDoctor);

        statusFlowService.updateAppointmentStatus(
                appointmentId, AppointmentStatus.IN_CONSULTATION,
                null, "系统", "分诊引擎", "自动分诊完成");

        eventPublisher.publishEvent(new TriageCompletedEvent(
                this, triageResult.getId(), appointmentId,
                appointment.getCustomerId(), appointment.getStoreId()));

        log.info("分诊完成, appointmentId: {}, triageResultId: {}", appointmentId, triageResult.getId());

        return buildTriageResultResponse(triageResult, needDoctor);
    }

    @Transactional(readOnly = true)
    public TriageResultResponse getTriageResult(Long appointmentId) {
        log.info("获取分诊结果, appointmentId: {}", appointmentId);

        TriageResult triageResult = triageResultRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("该预约没有分诊结果: " + appointmentId));

        boolean needDoctor = triageResult.getRiskLevel() == RiskLevel.HIGH
                || triageResult.getRiskLevel() == RiskLevel.EXTREME;

        return buildTriageResultResponse(triageResult, needDoctor);
    }

    @Transactional
    public TriageResultResponse reassignTriage(Long appointmentId, Long newDepartmentId,
                                               Long newConsultantGroupId, String reason) {
        log.info("人工改派分诊结果, appointmentId: {}, newDepartmentId: {}, newConsultantGroupId: {}",
                appointmentId, newDepartmentId, newConsultantGroupId);

        TriageResult triageResult = triageResultRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("该预约没有分诊结果: " + appointmentId));

        if (triageResult.getStatus() == TriageStatus.COMPLETED) {
            throw new IllegalStateException("已完成的分诊结果无法改派");
        }

        triageResult.setDepartmentId(newDepartmentId);
        triageResult.setConsultantGroupId(newConsultantGroupId);
        triageResult.setIsReassigned(true);
        triageResult.setReassignedReason(reason);
        triageResult.setStatus(TriageStatus.REASSIGNED);
        triageResult = triageResultRepository.save(triageResult);

        boolean needDoctor = triageResult.getRiskLevel() == RiskLevel.HIGH
                || triageResult.getRiskLevel() == RiskLevel.EXTREME;

        log.info("分诊结果改派成功, appointmentId: {}", appointmentId);

        return buildTriageResultResponse(triageResult, needDoctor);
    }

    private TriageResultResponse buildTriageResultResponse(TriageResult triageResult, boolean needDoctor) {
        Customer customer = customerRepository.findById(triageResult.getCustomerId()).orElse(null);
        Store store = storeRepository.findById(triageResult.getStoreId()).orElse(null);
        Department department = departmentRepository.findById(triageResult.getDepartmentId()).orElse(null);
        ConsultantGroup consultantGroup = consultantGroupRepository.findById(triageResult.getConsultantGroupId()).orElse(null);

        return TriageResultResponse.builder()
                .id(triageResult.getId())
                .appointmentId(triageResult.getAppointmentId())
                .customerName(customer != null ? customer.getName() : null)
                .storeName(store != null ? store.getName() : null)
                .departmentName(department != null ? department.getName() : null)
                .consultantGroupName(consultantGroup != null ? consultantGroup.getName() : null)
                .consultationType(triageResult.getConsultationType())
                .riskLevel(triageResult.getRiskLevel())
                .needDoctorAssessment(needDoctor)
                .triageTime(triageResult.getTriageTime())
                .isReassigned(triageResult.getIsReassigned())
                .reassignedReason(triageResult.getReassignedReason())
                .build();
    }
}
