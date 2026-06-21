package com.medical.triage.controller;

import com.medical.triage.dto.request.CustomerSyncRequest;
import com.medical.triage.dto.response.ApiResponse;
import com.medical.triage.entity.Customer;
import com.medical.triage.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/sync")
    public ApiResponse<Customer> syncCustomer(@Valid @RequestBody CustomerSyncRequest request) {
        Customer customer = customerService.syncCustomer(request);
        return ApiResponse.success("顾客资料同步成功", customer);
    }

    @GetMapping("/{id}")
    public ApiResponse<Customer> getCustomer(@PathVariable Long id) {
        Customer customer = customerService.getCustomer(id);
        return ApiResponse.success(customer);
    }

    @GetMapping("/phone/{phone}")
    public ApiResponse<Customer> getCustomerByPhone(@PathVariable String phone) {
        Customer customer = customerService.getCustomerByPhone(phone);
        return ApiResponse.success(customer);
    }
}
