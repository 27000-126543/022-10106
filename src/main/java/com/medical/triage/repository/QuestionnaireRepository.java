package com.medical.triage.repository;

import com.medical.triage.entity.Questionnaire;
import com.medical.triage.enums.ConsultationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {

    List<Questionnaire> findByStoreId(Long storeId);

    List<Questionnaire> findByStoreIdAndIsActive(Long storeId, Boolean isActive);

    Optional<Questionnaire> findByStoreIdAndTargetConsultationTypeAndIsActiveTrue(
            Long storeId, ConsultationType targetConsultationType);

    List<Questionnaire> findByTargetConsultationType(ConsultationType targetConsultationType);

    List<Questionnaire> findByIsActive(Boolean isActive);

    Optional<Questionnaire> findByStoreIdAndVersionAndIsActiveTrue(Long storeId, String version);

    boolean existsByStoreIdAndVersion(Long storeId, String version);
}
