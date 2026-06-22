package com.medical.triage.repository;

import com.medical.triage.entity.TriageExplanation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TriageExplanationRepository extends JpaRepository<TriageExplanation, Long> {

    Optional<TriageExplanation> findByTriageResultId(Long triageResultId);

    Optional<TriageExplanation> findByAppointmentId(Long appointmentId);
}
