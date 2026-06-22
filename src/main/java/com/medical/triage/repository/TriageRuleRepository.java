package com.medical.triage.repository;

import com.medical.triage.entity.TriageRule;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.DepartmentType;
import com.medical.triage.enums.RuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TriageRuleRepository extends JpaRepository<TriageRule, Long> {

    List<TriageRule> findByStoreIdAndIsEnabledTrue(Long storeId);

    List<TriageRule> findByStoreIdAndConsultationTypeAndIsEnabledTrue(
            Long storeId, ConsultationType consultationType);

    List<TriageRule> findByStoreIdAndDepartmentTypeAndIsEnabledTrue(
            Long storeId, DepartmentType departmentType);

    List<TriageRule> findByStoreIdAndIsEnabledTrueOrderByPriorityAsc(Long storeId);

    List<TriageRule> findByStoreIdAndConsultationTypeAndDepartmentTypeAndIsEnabledTrue(
            Long storeId, ConsultationType consultationType, DepartmentType departmentType);

    Optional<TriageRule> findByStoreIdAndNameAndIsEnabledTrue(Long storeId, String name);

    List<TriageRule> findByIsEnabledTrue();

    @Query("SELECT t FROM TriageRule t WHERE t.storeId = :storeId AND t.isEnabled = true " +
           "AND (t.minAge IS NULL OR t.minAge <= :age) " +
           "AND (t.maxAge IS NULL OR t.maxAge >= :age) " +
           "AND (t.gender IS NULL OR t.gender = :gender) " +
           "ORDER BY t.priority ASC")
    List<TriageRule> findMatchingRules(
            @Param("storeId") Long storeId,
            @Param("age") Integer age,
            @Param("gender") String gender);

    long countByStoreIdAndIsEnabledTrue(Long storeId);

    boolean existsByStoreIdAndNameAndIsEnabledTrue(Long storeId, String name);

    List<TriageRule> findByStoreIdAndStatusAndIsEnabledTrue(Long storeId, RuleStatus status);

    List<TriageRule> findByStoreIdAndIsOfficialTrueAndIsEnabledTrue(Long storeId);

    Optional<TriageRule> findByStoreIdAndRuleCodeAndIsOfficialTrue(Long storeId, String ruleCode);

    List<TriageRule> findByStoreIdAndStatusInAndIsEnabledTrue(Long storeId, List<RuleStatus> statuses);

    List<TriageRule> findByStoreIdAndStatus(Long storeId, RuleStatus status);

    List<TriageRule> findByRuleCodeOrderByVersionDesc(String ruleCode);
}
