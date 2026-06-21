package com.medical.triage.repository;

import com.medical.triage.entity.ConsultantGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultantGroupRepository extends JpaRepository<ConsultantGroup, Long> {

    List<ConsultantGroup> findByStoreId(Long storeId);

    List<ConsultantGroup> findByStoreIdAndStatus(Long storeId, Integer status);

    List<ConsultantGroup> findByDepartmentId(Long departmentId);

    List<ConsultantGroup> findByStoreIdAndDepartmentId(Long storeId, Long departmentId);

    Optional<ConsultantGroup> findByStoreIdAndName(Long storeId, String name);

    List<ConsultantGroup> findByStatus(Integer status);
}
