package com.medical.triage.repository;

import com.medical.triage.entity.QuestionnaireQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionnaireQuestionRepository extends JpaRepository<QuestionnaireQuestion, Long> {

    List<QuestionnaireQuestion> findByQuestionnaireId(Long questionnaireId);

    List<QuestionnaireQuestion> findByQuestionnaireIdOrderBySortOrderAsc(Long questionnaireId);

    List<QuestionnaireQuestion> findByQuestionnaireIdAndIsRequired(Long questionnaireId, Boolean isRequired);

    void deleteByQuestionnaireId(Long questionnaireId);

    long countByQuestionnaireId(Long questionnaireId);

    boolean existsByQuestionnaireIdAndRiskKeywordsContaining(Long questionnaireId, String keyword);
}
