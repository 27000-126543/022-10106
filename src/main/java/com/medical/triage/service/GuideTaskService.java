package com.medical.triage.service;

import com.medical.triage.dto.request.GuideTaskReassignRequest;
import com.medical.triage.dto.response.GuideTaskResponse;
import com.medical.triage.entity.Department;
import com.medical.triage.entity.GuideTask;
import com.medical.triage.entity.ReassignRecord;
import com.medical.triage.entity.TriageResult;
import com.medical.triage.enums.GuideTaskStatus;
import com.medical.triage.enums.GuideTaskType;
import com.medical.triage.event.GuideTaskStatusChangedEvent;
import com.medical.triage.repository.DepartmentRepository;
import com.medical.triage.repository.GuideTaskRepository;
import com.medical.triage.repository.ReassignRecordRepository;
import com.medical.triage.repository.StatusRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideTaskService {

    private final GuideTaskRepository guideTaskRepository;
    private final ReassignRecordRepository reassignRecordRepository;
    private final StatusRecordRepository statusRecordRepository;
    private final DepartmentRepository departmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public List<GuideTask> createGuideTasks(TriageResult triageResult, boolean needDoctor) {
        log.info("创建导诊任务, appointmentId: {}, needDoctor: {}",
                triageResult.getAppointmentId(), needDoctor);

        List<GuideTask> tasks = new ArrayList<>();

        GuideTask receptionTask = createTask(triageResult, GuideTaskType.RECEPTION,
                triageResult.getDepartmentId(), null, null,
                triageResult.getTriageTime().plusMinutes(30));
        tasks.add(receptionTask);

        GuideTask consultationTask = createTask(triageResult, GuideTaskType.CONSULTATION,
                triageResult.getDepartmentId(), triageResult.getConsultantGroupId(), null,
                triageResult.getTriageTime().plusMinutes(60));
        tasks.add(consultationTask);

        if (needDoctor) {
            GuideTask doctorAssessmentTask = createTask(triageResult, GuideTaskType.DOCTOR_ASSESSMENT,
                    triageResult.getDepartmentId(), null, null,
                    triageResult.getTriageTime().plusMinutes(90));
            tasks.add(doctorAssessmentTask);
        }

        tasks = guideTaskRepository.saveAll(tasks);
        log.info("导诊任务创建成功, appointmentId: {}, 任务数量: {}",
                triageResult.getAppointmentId(), tasks.size());

        return tasks;
    }

    @Transactional
    public GuideTaskResponse reassignTask(GuideTaskReassignRequest request) {
        log.info("人工改派任务, taskId: {}, newAssigneeId: {}",
                request.getTaskId(), request.getNewAssigneeId());

        GuideTask task = guideTaskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + request.getTaskId()));

        GuideTaskStatus fromStatus = task.getStatus();

        ReassignRecord reassignRecord = ReassignRecord.builder()
                .guideTaskId(task.getId())
                .appointmentId(task.getAppointmentId())
                .operatorId(request.getOperatorId())
                .operatorName(request.getOperatorName())
                .oldAssigneeId(task.getAssigneeId())
                .newAssigneeId(request.getNewAssigneeId())
                .oldDepartmentId(task.getDepartmentId())
                .newDepartmentId(request.getNewDepartmentId())
                .reason(request.getReason())
                .build();
        reassignRecordRepository.save(reassignRecord);

        task.setAssigneeId(request.getNewAssigneeId());
        task.setAssigneeName(request.getNewAssigneeName());
        task.setDepartmentId(request.getNewDepartmentId());
        task.setStatus(GuideTaskStatus.REASSIGNED);
        task = guideTaskRepository.save(task);

        eventPublisher.publishEvent(new GuideTaskStatusChangedEvent(
                this, task.getId(), task.getAppointmentId(), task.getCustomerId(),
                fromStatus, GuideTaskStatus.REASSIGNED));

        log.info("任务改派成功, taskId: {}", task.getId());
        return buildTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public GuideTaskResponse getTask(Long taskId) {
        GuideTask task = guideTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));
        return buildTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public List<GuideTaskResponse> getTasksByAppointment(Long appointmentId) {
        log.info("根据预约查询任务, appointmentId: {}", appointmentId);
        List<GuideTask> tasks = guideTaskRepository.findByAppointmentId(appointmentId);
        return tasks.stream()
                .map(this::buildTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<GuideTaskResponse> getTasksByStore(Long storeId, GuideTaskStatus status, Pageable pageable) {
        log.info("分页查询门店任务, storeId: {}, status: {}", storeId, status);

        Page<GuideTask> taskPage = guideTaskRepository.findAll(
                (root, query, cb) -> {
                    var predicates = cb.conjunction();
                    if (storeId != null) {
                        predicates = cb.and(predicates, cb.equal(root.get("storeId"), storeId));
                    }
                    if (status != null) {
                        predicates = cb.and(predicates, cb.equal(root.get("status"), status));
                    }
                    return predicates;
                },
                pageable
        );

        return taskPage.map(this::buildTaskResponse);
    }

    private GuideTask createTask(TriageResult triageResult, GuideTaskType taskType,
                                 Long departmentId, Long consultantGroupId,
                                 Long assigneeId, LocalDateTime dueTime) {
        return GuideTask.builder()
                .appointmentId(triageResult.getAppointmentId())
                .customerId(triageResult.getCustomerId())
                .storeId(triageResult.getStoreId())
                .departmentId(departmentId)
                .consultantGroupId(consultantGroupId)
                .taskType(taskType)
                .status(GuideTaskStatus.PENDING)
                .assigneeId(assigneeId)
                .dueTime(dueTime)
                .build();
    }

    private GuideTaskResponse buildTaskResponse(GuideTask task) {
        Department department = departmentRepository.findById(task.getDepartmentId()).orElse(null);

        return GuideTaskResponse.builder()
                .id(task.getId())
                .appointmentId(task.getAppointmentId())
                .taskType(task.getTaskType())
                .status(task.getStatus())
                .assigneeName(task.getAssigneeName())
                .departmentName(department != null ? department.getName() : null)
                .dueTime(task.getDueTime())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
