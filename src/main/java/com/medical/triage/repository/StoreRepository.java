package com.medical.triage.repository;

import com.medical.triage.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByCode(String code);

    List<Store> findByStatus(Integer status);

    List<Store> findByCity(String city);

    List<Store> findByProvince(String province);

    boolean existsByCode(String code);
}
