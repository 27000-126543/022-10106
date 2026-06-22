package com.medical.triage.repository;

import com.medical.triage.entity.TriageRuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TriageRuleVersionRepository extends JpaRepository<TriageRuleVersion, Long> {

    List<TriageRuleVersion> findByRuleIdOrderByVersionDesc(Long ruleId);

    Optional<TriageRuleVersion> findByRuleIdAndVersion(Long ruleId, Integer version);
}
