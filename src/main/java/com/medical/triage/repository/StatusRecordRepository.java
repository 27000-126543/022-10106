package com.medical.triage.repository;

import com.medical.triage.entity.StatusRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatusRecordRepository extends JpaRepository<StatusRecord, Long> {

    List<StatusRecord> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    List<StatusRecord> findByGuideTaskIdOrderByCreatedAtDesc(Long guideTaskId);

    List<StatusRecord> findByOperatorIdOrderByCreatedAtDesc(Long operatorId);

    List<StatusRecord> findByAppointmentIdAndToStatus(Long appointmentId, String toStatus);

    List<StatusRecord> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<StatusRecord> findByAppointmentIdAndCreatedAtBetween(
            Long appointmentId, LocalDateTime startTime, LocalDateTime endTime);

    long countByAppointmentIdAndToStatus(Long appointmentId, String toStatus);

    long countByOperatorIdAndCreatedAtBetween(Long operatorId, LocalDateTime startTime, LocalDateTime endTime);

    void deleteByAppointmentId(Long appointmentId);

    void deleteByGuideTaskId(Long guideTaskId);
}
