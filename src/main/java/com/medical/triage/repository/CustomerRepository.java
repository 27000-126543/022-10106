package com.medical.triage.repository;

import com.medical.triage.entity.Customer;
import com.medical.triage.enums.AppointmentSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByIdCard(String idCard);

    List<Customer> findByStoreId(Long storeId);

    List<Customer> findBySource(AppointmentSource source);

    List<Customer> findByMemberLevel(String memberLevel);

    boolean existsByPhone(String phone);

    boolean existsByIdCard(String idCard);
}
