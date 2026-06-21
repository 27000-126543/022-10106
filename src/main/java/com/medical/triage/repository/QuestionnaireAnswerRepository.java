package com.medical.triage.repository;

import com.medical.triage.entity.QuestionnaireAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionnaireAnswerRepository extends JpaRepository<QuestionnaireAnswer, Long> {

    List<QuestionnaireAnswer> findByAppointmentId(Long appointmentId);

    List<QuestionnaireAnswer> findByCustomerId(Long customerId);

    List<QuestionnaireAnswer> findByAppointmentIdAndQuestionId(Long appointmentId, Long questionId);

    List<QuestionnaireAnswer> findByCustomerIdAndQuestionId(Long customerId, Long questionId);

    void deleteByAppointmentId(Long appointmentId);

    long countByAppointmentId(Long appointmentId);

    boolean existsByAppointmentIdAndAnswerTextContaining(Long appointmentId, String keyword);

    boolean existsByCustomerIdAndAnswerTextContaining(Long customerId, String keyword);
}
