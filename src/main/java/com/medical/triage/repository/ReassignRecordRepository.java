package com.medical.triage.repository;

import com.medical.triage.entity.ReassignRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReassignRecordRepository extends JpaRepository<ReassignRecord, Long> {

    List<ReassignRecord> findByGuideTaskIdOrderByCreatedAtDesc(Long guideTaskId);

    List<ReassignRecord> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    List<ReassignRecord> findByOperatorId(Long operatorId);

    List<ReassignRecord> findByOldAssigneeId(Long oldAssigneeId);

    List<ReassignRecord> findByNewAssigneeId(Long newAssigneeId);

    List<ReassignRecord> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<ReassignRecord> findByNewDepartmentId(Long newDepartmentId);

    List<ReassignRecord> findByOldDepartmentIdAndCreatedAtBetween(
            Long oldDepartmentId, LocalDateTime startTime, LocalDateTime endTime);

    long countByNewAssigneeIdAndCreatedAtBetween(Long newAssigneeId, LocalDateTime startTime, LocalDateTime endTime);

    long countByNewDepartmentIdAndCreatedAtBetween(Long newDepartmentId, LocalDateTime startTime, LocalDateTime endTime);

    void deleteByGuideTaskId(Long guideTaskId);

    void deleteByAppointmentId(Long appointmentId);
}
