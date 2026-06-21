package com.medical.triage.repository;

import com.medical.triage.entity.Department;
import com.medical.triage.enums.DepartmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByStoreId(Long storeId);

    List<Department> findByStoreIdAndStatus(Long storeId, Integer status);

    List<Department> findByStoreIdAndDepartmentType(Long storeId, DepartmentType departmentType);

    Optional<Department> findByStoreIdAndName(Long storeId, String name);

    List<Department> findByDepartmentType(DepartmentType departmentType);

    List<Department> findByStatus(Integer status);
}
