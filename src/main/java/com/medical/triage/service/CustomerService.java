package com.medical.triage.service;

import com.medical.triage.dto.request.CustomerSyncRequest;
import com.medical.triage.entity.Customer;
import com.medical.triage.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer syncCustomer(CustomerSyncRequest request) {
        log.info("开始同步顾客资料, 手机号: {}", request.getPhone());

        return customerRepository.findByPhone(request.getPhone())
                .map(existingCustomer -> {
                    existingCustomer.setName(request.getName());
                    existingCustomer.setGender(request.getGender());
                    existingCustomer.setAge(request.getAge());
                    existingCustomer.setIdCard(request.getIdCard());
                    existingCustomer.setAddress(request.getAddress());
                    existingCustomer.setSource(request.getSource());
                    existingCustomer.setMemberLevel(request.getMemberLevel());
                    Customer updated = customerRepository.save(existingCustomer);
                    log.info("顾客资料更新成功, customerId: {}", updated.getId());
                    return updated;
                })
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .name(request.getName())
                            .gender(request.getGender())
                            .age(request.getAge())
                            .phone(request.getPhone())
                            .idCard(request.getIdCard())
                            .address(request.getAddress())
                            .source(request.getSource())
                            .memberLevel(request.getMemberLevel())
                            .build();
                    Customer created = customerRepository.save(newCustomer);
                    log.info("顾客资料创建成功, customerId: {}", created.getId());
                    return created;
                });
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("顾客不存在: " + id));
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByPhone(String phone) {
        return customerRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("手机号对应的顾客不存在: " + phone));
    }
}
