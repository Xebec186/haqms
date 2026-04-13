package com.haqms.service.impl;

import com.haqms.dto.request.CreateAppointmentRequest;
import com.haqms.dto.response.AppointmentResponse;
import com.haqms.entity.*;
import com.haqms.enums.AppointmentPriority;
import com.haqms.enums.AppointmentStatus;
import com.haqms.exception.ConflictException;
import com.haqms.exception.ResourceNotFoundException;
import com.haqms.exception.ValidationException;
import com.haqms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppointmentServiceImpl.
 *
 * All database dependencies are mocked with Mockito.
 * Tests focus on the five-step validation sequence in createAppointment()
 * and the business rules in updateStatus(), updatePriority(), and delete().
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentServiceImpl")
class AppointmentServiceImplTest {

    @Mock private AppointmentRepository       appointmentRepository;
    @Mock private PatientRepository           patientRepository;
    @Mock private HealthcareProviderRepository providerRepository;
    @Mock private DepartmentRepository        departmentRepository;
    @Mock private ProviderScheduleRepository  scheduleRepository;

    @InjectMocks
    private AppointmentServiceImpl service;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private Patient          patient;
    private HealthcareProvider provider;
    private Department       department;
    private ProviderSchedule schedule;
    private SystemUser       patientUser;
    private SystemUser       adminUser;
    private Role             patientRole;
    private Role             adminRole;

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(7);
    private static final LocalTime VALID_TIME   = LocalTime.of(9, 0);

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setDepartmentId(1L);
        department.setName("General Outpatient");

        provider = new HealthcareProvider();
        provider.setProviderId(10L);
        provider.setFirstName("Kofi");
        provider.setLastName("Mensah");
        provider.setDepartment(department);

        patient = new Patient();
        patient.setPatientId(100L);
        patient.setFirstName("Kwame");
        patient.setLastName("Asante");

        schedule = new ProviderSchedule();
        schedule.setScheduleId(5L);
        schedule.setProvider(provider);
        schedule.setScheduleDate(FUTURE_DATE);
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(17, 0));
        schedule.setMaxSlots(20);
        schedule.setIsAvailable(true);

        patientRole = new Role();
        patientRole.setRoleId(3);
        patientRole.setRoleName("PATIENT");

        adminRole = new Role();
        adminRole.setRoleId(1);
        adminRole.setRoleName("ADMIN");

        patientUser = new SystemUser();
        patientUser.setUserId(200L);
        patientUser.setRole(patientRole);
        patientUser.setPatient(patient);

        adminUser = new SystemUser();
        adminUser.setUserId(1L);
        adminUser.setRole(adminRole);
    }

    // ── Helper: build a valid request ─────────────────────────────────────────
    private CreateAppointmentRequest validRequest() {
        CreateAppointmentRequest r = new CreateAppointmentRequest();
        r.setPatientId(100L);
        r.setProviderId(10L);
        r.setDepartmentId(1L);
        r.setScheduleId(5L);
        r.setReason("Routine check-up");
        r.setPriority(AppointmentPriority.REGULAR);
        // null bookedByUserId = patient self-booking
        return r;
    }

    private void stubAllEntities() {
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));
        when(appointmentRepository.countActiveByScheduleId(5L)).thenReturn(0L);
    }

    private Appointment savedAppointment(AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setAppointmentId(1L);
        a.setPatient(patient);
        a.setProvider(provider);
        a.setDepartment(department);
        a.setSchedule(schedule);
        a.setAppointmentDate(FUTURE_DATE);
        a.setStatus(status);
        a.setAppointmentPriority(AppointmentPriority.REGULAR);
        a.setReason("Routine check-up");
        return a;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // createAppointment()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createAppointment()")
    class CreateAppointment {

        @Test
        @DisplayName("TC-AS-001: Valid request creates and returns appointment")
        void validRequest_createsAppointment() {
            stubAllEntities();
            Appointment saved = savedAppointment(AppointmentStatus.SCHEDULED);
            when(appointmentRepository.save(any())).thenReturn(saved);

            AppointmentResponse response = service.createAppointment(validRequest());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
            assertThat(response.getAppointmentPriority()).isEqualTo(AppointmentPriority.REGULAR);

            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository).save(captor.capture());
            Appointment persisted = captor.getValue();
            assertThat(persisted.getPatient()).isSameAs(patient);
            assertThat(persisted.getProvider()).isSameAs(provider);
        }

        @Test
        @DisplayName("TC-AS-002: Patient not found throws ResourceNotFoundException")
        void patientNotFound_throwsResourceNotFound() {
            when(patientRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createAppointment(validRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Patient not found");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-AS-003: Provider not found throws ResourceNotFoundException")
        void providerNotFound_throwsResourceNotFound() {
            when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
            when(providerRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createAppointment(validRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Provider not found");
        }

        @Test
        @DisplayName("TC-AS-004: Schedule belongs to different provider throws ValidationException")
        void scheduleMismatch_throwsValidation() {
            HealthcareProvider otherProvider = new HealthcareProvider();
            otherProvider.setProviderId(99L);
            schedule.setProvider(otherProvider);   // schedule belongs to provider 99, not 10

            when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
            when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

            assertThatThrownBy(() -> service.createAppointment(validRequest()))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("does not belong to provider");
        }


        @Test
        @DisplayName("TC-AS-007: Schedule at maximum capacity throws ConflictException")
        void scheduleAtCapacity_throwsConflict() {
            schedule.setMaxSlots(10);

            when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
            when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));
            when(appointmentRepository.countActiveByScheduleId(5L)).thenReturn(10L); // at max

            assertThatThrownBy(() -> service.createAppointment(validRequest()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("No available slots remaining");
        }

        @Test
        @DisplayName("TC-AS-008: Duplicate provider/date/time throws ConflictException")
        void doubleBooking_throwsConflict() {
            when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
            when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));
            when(appointmentRepository.countActiveByScheduleId(5L)).thenReturn(0L);

            assertThatThrownBy(() -> service.createAppointment(validRequest()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already has an appointment");
        }

        @Test
        @DisplayName("TC-AS-009: Non-REGULAR priority with bookedByUserId set throws ValidationException")
        void nonRegularPriority_withBookedByUser_throwsValidation() {
            CreateAppointmentRequest req = validRequest();
            req.setPriority(AppointmentPriority.EMERGENCY);
            req.setBookedByUserId(200L); // simulates patient self-booking with EMERGENCY

            when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
            when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));
            when(appointmentRepository.countActiveByScheduleId(5L)).thenReturn(0L);

            assertThatThrownBy(() -> service.createAppointment(req))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Patients may only book REGULAR priority");
        }

        @Test
        @DisplayName("TC-AS-010: Null priority defaults to REGULAR")
        void nullPriority_defaultsToRegular() {
            CreateAppointmentRequest req = validRequest();
            req.setPriority(null);

            stubAllEntities();
            Appointment saved = savedAppointment(AppointmentStatus.SCHEDULED);
            when(appointmentRepository.save(any())).thenReturn(saved);

            service.createAppointment(req);

            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository).save(captor.capture());
            assertThat(captor.getValue().getAppointmentPriority())
                    .isEqualTo(AppointmentPriority.REGULAR);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getById()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("TC-AS-011: Appointment found returns response")
        void found_returnsResponse() {
            Appointment a = savedAppointment(AppointmentStatus.SCHEDULED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

            AppointmentResponse response = service.getById(1L, adminUser);

            assertThat(response.getAppointmentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("TC-AS-012: Appointment not found throws ResourceNotFoundException")
        void notFound_throwsResourceNotFound() {
            when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(999L, adminUser))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Appointment not found");
        }

        @Test
        @DisplayName("TC-AS-013: PATIENT accessing another patient's appointment throws ValidationException")
        void patientAccessOtherAppointment_throwsValidation() {
            Patient otherPatient = new Patient();
            otherPatient.setPatientId(999L);
            Appointment a = savedAppointment(AppointmentStatus.SCHEDULED);
            a.setPatient(otherPatient);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

            // patientUser is linked to patientId=100, appointment belongs to patientId=999
            assertThatThrownBy(() -> service.getById(1L, patientUser))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("does not belong to you");
        }

        @Test
        @DisplayName("TC-AS-014: PATIENT accessing own appointment succeeds")
        void patientAccessOwnAppointment_succeeds() {
            Appointment a = savedAppointment(AppointmentStatus.SCHEDULED);
            // appointment.patient = patientId 100, patientUser.patient = patientId 100
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

            assertThatNoException().isThrownBy(() -> service.getById(1L, patientUser));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getByPatientId()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getByPatientId()")
    class GetByPatientId {

        @Test
        @DisplayName("TC-AS-015: Null patientId throws ValidationException")
        void nullPatientId_throwsValidation() {
            assertThatThrownBy(() -> service.getByPatientId(null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Patient ID could not be resolved");
        }

        @Test
        @DisplayName("TC-AS-016: Returns mapped list for valid patientId")
        void validPatientId_returnsList() {
            Appointment a = savedAppointment(AppointmentStatus.SCHEDULED);
            when(appointmentRepository
                    .findByPatientPatientIdOrderByAppointmentDate(100L))
                    .thenReturn(List.of(a));

            List<AppointmentResponse> result = service.getByPatientId(100L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("TC-AS-017: Returns empty list when patient has no appointments")
        void noAppointments_returnsEmptyList() {
            when(appointmentRepository
                    .findByPatientPatientIdOrderByAppointmentDate(100L))
                    .thenReturn(Collections.emptyList());

            List<AppointmentResponse> result = service.getByPatientId(100L);

            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // updateStatus()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("TC-AS-018: Valid status transition SCHEDULED → CONFIRMED succeeds")
        void validTransition_scheduledToConfirmed() {
            Appointment a = savedAppointment(AppointmentStatus.SCHEDULED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));
            when(appointmentRepository.save(any())).thenReturn(a);

            AppointmentResponse response = service.updateStatus(
                    1L, AppointmentStatus.CONFIRMED, null, adminUser);

            assertThat(response.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        }

        @Test
        @DisplayName("TC-AS-019: Updating COMPLETED appointment throws ValidationException")
        void completedAppointment_throwsValidation() {
            Appointment a = savedAppointment(AppointmentStatus.COMPLETED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

            assertThatThrownBy(() -> service.updateStatus(
                    1L, AppointmentStatus.CANCELLED, "reason", patientUser))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("completed");
        }

        @Test
        @DisplayName("TC-AS-020: Updating CANCELLED appointment throws ValidationException")
        void cancelledAppointment_throwsValidation() {
            Appointment a = savedAppointment(AppointmentStatus.CANCELLED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

            assertThatThrownBy(() -> service.updateStatus(
                    1L, AppointmentStatus.CONFIRMED, null, adminUser))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("cancelled");
        }

        @Test
        @DisplayName("TC-AS-021: Cancellation without reason throws ValidationException")
        void cancelWithoutReason_throwsValidation() {
            Appointment a = savedAppointment(AppointmentStatus.SCHEDULED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

            assertThatThrownBy(() -> service.updateStatus(
                    1L, AppointmentStatus.CANCELLED, null, patientUser))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("cancellation reason is required");
        }

        @Test
        @DisplayName("TC-AS-022: Cancellation with blank reason throws ValidationException")
        void cancelWithBlankReason_throwsValidation() {
            Appointment a = savedAppointment(AppointmentStatus.SCHEDULED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

            assertThatThrownBy(() -> service.updateStatus(
                    1L, AppointmentStatus.CANCELLED, "   ", patientUser))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("cancellation reason is required");
        }

        @Test
        @DisplayName("TC-AS-023: Valid cancellation stores reason and updates status")
        void validCancellation_storesReasonAndStatus() {
            Appointment a = savedAppointment(AppointmentStatus.SCHEDULED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));
            when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AppointmentResponse response = service.updateStatus(
                    1L, AppointmentStatus.CANCELLED, "Unable to attend", patientUser);

            assertThat(response.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(a.getCancellationReason()).isEqualTo("Unable to attend");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // updatePriority()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatePriority()")
    class UpdatePriority {

        @Test
        @DisplayName("TC-AS-024: Priority updated from REGULAR to EMERGENCY")
        void priorityEscalation_toEmergency() {
            Appointment a = savedAppointment(AppointmentStatus.SCHEDULED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));
            when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AppointmentResponse response = service.updatePriority(1L, AppointmentPriority.EMERGENCY);

            assertThat(response.getAppointmentPriority()).isEqualTo(AppointmentPriority.EMERGENCY);
        }

        @Test
        @DisplayName("TC-AS-025: Cannot change priority of COMPLETED appointment")
        void completedAppointment_throwsValidation() {
            Appointment a = savedAppointment(AppointmentStatus.COMPLETED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

            assertThatThrownBy(() -> service.updatePriority(1L, AppointmentPriority.URGENT))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("terminal");
        }

        @Test
        @DisplayName("TC-AS-026: Cannot change priority of CANCELLED appointment")
        void cancelledAppointment_throwsValidation() {
            Appointment a = savedAppointment(AppointmentStatus.CANCELLED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

            assertThatThrownBy(() -> service.updatePriority(1L, AppointmentPriority.URGENT))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("terminal");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // delete()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("TC-AS-027: Deleting CANCELLED appointment succeeds")
        void cancelledAppointment_deletedSuccessfully() {
            Appointment a = savedAppointment(AppointmentStatus.CANCELLED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

            assertThatNoException().isThrownBy(() -> service.delete(1L));
            verify(appointmentRepository).delete(a);
        }

        @Test
        @DisplayName("TC-AS-028: Deleting non-CANCELLED appointment throws ValidationException")
        void nonCancelledAppointment_throwsValidation() {
            Appointment a = savedAppointment(AppointmentStatus.SCHEDULED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(a));

            assertThatThrownBy(() -> service.delete(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Only CANCELLED appointments may be deleted");

            verify(appointmentRepository, never()).delete(any());
        }
    }
}
