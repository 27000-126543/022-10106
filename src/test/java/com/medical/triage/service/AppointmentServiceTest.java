package com.medical.triage.service;

import com.medical.triage.dto.request.AppointmentCreateRequest;
import com.medical.triage.dto.response.AppointmentResponse;
import com.medical.triage.entity.Appointment;
import com.medical.triage.entity.Customer;
import com.medical.triage.entity.Store;
import com.medical.triage.enums.AppointmentSource;
import com.medical.triage.enums.AppointmentStatus;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.repository.AppointmentRepository;
import com.medical.triage.repository.CustomerRepository;
import com.medical.triage.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentService appointmentService;

    private Customer testCustomer;
    private Store testStore;
    private Appointment testAppointment;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(1L)
                .name("张三")
                .phone("13800138000")
                .build();

        testStore = Store.builder()
                .id(1L)
                .name("北京朝阳店")
                .build();

        testAppointment = Appointment.builder()
                .id(1L)
                .customerId(1L)
                .storeId(1L)
                .appointmentTime(LocalDateTime.now().plusDays(1))
                .source(AppointmentSource.OFFICIAL_WEBSITE)
                .consultationType(ConsultationType.SURGERY_CONSULTATION)
                .status(AppointmentStatus.PENDING)
                .notes("想做双眼皮")
                .isFirstVisit(true)
                .build();
    }

    @Test
    void testCreateAppointment_NewCustomer() {
        AppointmentCreateRequest request = AppointmentCreateRequest.builder()
                .customerName("张三")
                .phone("13800138000")
                .gender(1)
                .age(28)
                .storeId(1L)
                .appointmentTime(LocalDateTime.now().plusDays(1))
                .source(AppointmentSource.OFFICIAL_WEBSITE)
                .consultationType(ConsultationType.SURGERY_CONSULTATION)
                .notes("想做双眼皮")
                .isFirstVisit(true)
                .build();

        when(customerRepository.findByPhone("13800138000")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        AppointmentResponse response = appointmentService.createAppointment(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("张三", response.getCustomerName());
        assertEquals("北京朝阳店", response.getStoreName());
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void testCreateAppointment_ExistingCustomer() {
        AppointmentCreateRequest request = AppointmentCreateRequest.builder()
                .customerName("张三")
                .phone("13800138000")
                .gender(1)
                .age(28)
                .storeId(1L)
                .appointmentTime(LocalDateTime.now().plusDays(1))
                .source(AppointmentSource.MINI_PROGRAM)
                .consultationType(ConsultationType.SURGERY_CONSULTATION)
                .notes("想做双眼皮")
                .isFirstVisit(false)
                .build();

        when(customerRepository.findByPhone("13800138000")).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        AppointmentResponse response = appointmentService.createAppointment(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void testCreateAppointment_StoreNotFound() {
        AppointmentCreateRequest request = AppointmentCreateRequest.builder()
                .customerName("张三")
                .phone("13800138000")
                .gender(1)
                .age(28)
                .storeId(999L)
                .appointmentTime(LocalDateTime.now().plusDays(1))
                .source(AppointmentSource.OFFICIAL_WEBSITE)
                .consultationType(ConsultationType.SURGERY_CONSULTATION)
                .build();

        when(storeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> appointmentService.createAppointment(request));
    }

    @Test
    void testGetAppointment() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));

        AppointmentResponse response = appointmentService.getAppointment(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("张三", response.getCustomerName());
    }

    @Test
    void testGetAppointment_NotFound() {
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> appointmentService.getAppointment(999L));
    }

    @Test
    void testConfirmArrival() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        assertDoesNotThrow(() -> appointmentService.confirmArrival(1L));
        assertEquals(AppointmentStatus.ARRIVED, testAppointment.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void testConfirmArrival_InvalidStatus() {
        testAppointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));

        assertThrows(IllegalStateException.class, () -> appointmentService.confirmArrival(1L));
    }

    @Test
    void testCancelAppointment() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        assertDoesNotThrow(() -> appointmentService.cancelAppointment(1L, "用户原因取消"));
        assertEquals(AppointmentStatus.CANCELLED, testAppointment.getStatus());
    }

    @Test
    void testGetAppointments() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(7);

        when(appointmentRepository.findByStoreIdAndAppointmentTimeBetween(1L, start, end, pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(testAppointment), pageable, 1));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));

        Page<AppointmentResponse> result = appointmentService.getAppointments(1L, start, end, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("张三", result.getContent().get(0).getCustomerName());
    }
}
