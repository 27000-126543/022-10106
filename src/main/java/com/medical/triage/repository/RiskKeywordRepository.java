package com.medical.triage.repository;

import com.medical.triage.entity.RiskKeyword;
import com.medical.triage.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskKeywordRepository extends JpaRepository<RiskKeyword, Long> {

    Optional<RiskKeyword> findByKeyword(String keyword);

    List<RiskKeyword> findByRiskLevel(RiskLevel riskLevel);

    List<RiskKeyword> findByNeedDoctorAssessment(Boolean needDoctorAssessment);

    List<RiskKeyword> findByRiskLevelAndNeedDoctorAssessment(RiskLevel riskLevel, Boolean needDoctorAssessment);

    @Query("SELECT r FROM RiskKeyword r WHERE :text LIKE CONCAT('%', r.keyword, '%')")
    List<RiskKeyword> findMatchingKeywords(@Param("text") String text);

    @Query("SELECT r FROM RiskKeyword r WHERE r.riskLevel = :riskLevel AND :text LIKE CONCAT('%', r.keyword, '%')")
    List<RiskKeyword> findMatchingKeywordsByRiskLevel(
            @Param("text") String text,
            @Param("riskLevel") RiskLevel riskLevel);

    boolean existsByKeyword(String keyword);

    long countByRiskLevel(RiskLevel riskLevel);
}
