package com.medical.triage.repository;

import com.medical.triage.entity.ConsultationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationResultRepository extends JpaRepository<ConsultationResult, Long> {

    Optional<ConsultationResult> findByAppointmentId(Long appointmentId);

    List<ConsultationResult> findByCustomerId(Long customerId);

    List<ConsultationResult> findByConsultantId(Long consultantId);

    List<ConsultationResult> findByDoctorId(Long doctorId);

    List<ConsultationResult> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<ConsultationResult> findByConsultantIdAndCreatedAtBetween(
            Long consultantId, LocalDateTime startTime, LocalDateTime endTime);

    List<ConsultationResult> findByDoctorIdAndCreatedAtBetween(
            Long doctorId, LocalDateTime startTime, LocalDateTime endTime);

    List<ConsultationResult> findByFollowUpDateBetween(LocalDateTime startTime, LocalDateTime endTime);

    long countByConsultantIdAndCreatedAtBetween(Long consultantId, LocalDateTime startTime, LocalDateTime endTime);

    long countByDoctorIdAndCreatedAtBetween(Long doctorId, LocalDateTime startTime, LocalDateTime endTime);

    boolean existsByAppointmentId(Long appointmentId);

    boolean existsByDiagnosisContaining(String keyword);
}
