package com.medical.triage.service;

import com.medical.triage.dto.request.AppointmentCreateRequest;
import com.medical.triage.dto.response.AppointmentResponse;
import com.medical.triage.entity.Appointment;
import com.medical.triage.entity.Customer;
import com.medical.triage.entity.Store;
import com.medical.triage.entity.TriageResult;
import com.medical.triage.enums.AppointmentStatus;
import com.medical.triage.event.AppointmentCreatedEvent;
import com.medical.triage.event.CustomerArrivedEvent;
import com.medical.triage.repository.AppointmentRepository;
import com.medical.triage.repository.CustomerRepository;
import com.medical.triage.repository.StoreRepository;
import com.medical.triage.repository.TriageResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;
    private final StoreRepository storeRepository;
    private final TriageResultRepository triageResultRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AppointmentResponse createAppointment(AppointmentCreateRequest request) {
        log.info("开始创建预约, 客户手机号: {}, 门店ID: {}", request.getPhone(), request.getStoreId());

        Customer customer = customerRepository.findByPhone(request.getPhone())
                .map(existingCustomer -> {
                    existingCustomer.setName(request.getCustomerName());
                    existingCustomer.setGender(request.getGender());
                    existingCustomer.setAge(request.getAge());
                    existingCustomer.setIdCard(request.getIdCard());
                    existingCustomer.setSource(request.getSource());
                    return customerRepository.save(existingCustomer);
                })
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .name(request.getCustomerName())
                            .gender(request.getGender())
                            .age(request.getAge())
                            .phone(request.getPhone())
                            .idCard(request.getIdCard())
                            .source(request.getSource())
                            .memberLevel(request.getMemberLevel())
                            .build();
                    return customerRepository.save(newCustomer);
                });

        Appointment appointment = Appointment.builder()
                .customerId(customer.getId())
                .storeId(request.getStoreId())
                .appointmentTime(request.getAppointmentTime())
                .source(request.getSource())
                .consultationType(request.getConsultationType())
                .status(AppointmentStatus.PENDING)
                .notes(request.getNotes())
                .isFirstVisit(request.getIsFirstVisit() != null ? request.getIsFirstVisit() : false)
                .build();

        appointment = appointmentRepository.save(appointment);
        log.info("预约创建成功, appointmentId: {}", appointment.getId());

        eventPublisher.publishEvent(new AppointmentCreatedEvent(
                this, appointment.getId(), customer.getId(),
                appointment.getStoreId(), appointment.getConsultationType()));

        return buildAppointmentResponse(appointment, customer);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + id));

        Customer customer = customerRepository.findById(appointment.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("顾客不存在: " + appointment.getCustomerId()));

        return buildAppointmentResponse(appointment, customer);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointments(Long storeId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        Page<Appointment> appointmentPage = appointmentRepository.findAll(
                (root, query, cb) -> {
                    var predicates = cb.conjunction();
                    if (storeId != null) {
                        predicates = cb.and(predicates, cb.equal(root.get("storeId"), storeId));
                    }
                    if (start != null) {
                        predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("appointmentTime"), start));
                    }
                    if (end != null) {
                        predicates = cb.and(predicates, cb.lessThan(root.get("appointmentTime"), end));
                    }
                    return predicates;
                },
                pageable
        );

        return appointmentPage.map(appointment -> {
            Customer customer = customerRepository.findById(appointment.getCustomerId()).orElse(null);
            return buildAppointmentResponse(appointment, customer);
        });
    }

    @Transactional
    public void confirmArrival(Long appointmentId) {
        log.info("确认顾客到店, appointmentId: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("已取消的预约无法确认到店");
        }

        appointment.setStatus(AppointmentStatus.ARRIVED);
        appointmentRepository.save(appointment);

        eventPublisher.publishEvent(new CustomerArrivedEvent(
                this, appointmentId, appointment.getCustomerId(),
                appointment.getStoreId(), LocalDateTime.now()));

        log.info("顾客到店确认成功, appointmentId: {}", appointmentId);
    }

    @Transactional
    public void cancelAppointment(Long appointmentId, String reason) {
        log.info("取消预约, appointmentId: {}, 原因: {}", appointmentId, reason);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("已完成的预约无法取消");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setNotes(appointment.getNotes() + " [取消原因: " + reason + "]");
        appointmentRepository.save(appointment);

        log.info("预约取消成功, appointmentId: {}", appointmentId);
    }

    private AppointmentResponse buildAppointmentResponse(Appointment appointment, Customer customer) {
        Store store = storeRepository.findById(appointment.getStoreId()).orElse(null);
        TriageResult triageResult = triageResultRepository.findByAppointmentId(appointment.getId()).orElse(null);

        return AppointmentResponse.builder()
                .id(appointment.getId())
                .customerId(appointment.getCustomerId())
                .customerName(customer != null ? customer.getName() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .storeId(appointment.getStoreId())
                .storeName(store != null ? store.getName() : null)
                .appointmentTime(appointment.getAppointmentTime())
                .consultationType(appointment.getConsultationType())
                .status(appointment.getStatus())
                .triageStatus(triageResult != null ? triageResult.getStatus() : null)
                .riskLevel(triageResult != null ? triageResult.getRiskLevel() : null)
                .createdAt(appointment.getCreatedAt())
                .build();
    }
}
