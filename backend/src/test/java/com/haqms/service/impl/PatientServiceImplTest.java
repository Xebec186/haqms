package com.haqms.service.impl;

import com.haqms.dto.request.CreatePatientRequest;
import com.haqms.dto.request.UpdatePatientRequest;
import com.haqms.dto.response.PatientResponse;
import com.haqms.entity.Patient;
import com.haqms.entity.SystemUser;
import com.haqms.exception.ConflictException;
import com.haqms.exception.ResourceNotFoundException;
import com.haqms.repository.PatientRepository;
import com.haqms.repository.SystemUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PatientServiceImpl.
 *
 * Covers staff-side patient registration, lookups by ID and userId,
 * contact detail updates, and paginated patient search.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientServiceImpl")
class PatientServiceImplTest {

    @Mock private PatientRepository    patientRepository;
    @Mock private SystemUserRepository userRepository;

    @InjectMocks
    private PatientServiceImpl service;

    private Patient    patient;
    private SystemUser user;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setPatientId(100L);
        patient.setFirstName("Kwame");
        patient.setLastName("Asante");
        patient.setPhoneNumber("+233241234567");
        patient.setIsActive(true);

        user = new SystemUser();
        user.setUserId(200L);
        user.setPatient(patient);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // register() — staff-side
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("register() — staff-side")
    class Register {

        private CreatePatientRequest validRequest() {
            CreatePatientRequest r = new CreatePatientRequest();
            r.setFirstName("Ama");
            r.setLastName("Ofori");
            r.setGender("FEMALE");
            r.setDateOfBirth(java.time.LocalDate.of(1985, 3, 22));
            r.setPhoneNumber("+233551234567");
            r.setEmail("ama.ofori@mail.gh");
            return r;
        }

        @Test
        @DisplayName("TC-PS-001: Valid request creates and returns patient")
        void validRequest_createsPatient() {
            when(patientRepository.existsByPhoneNumber("+233551234567")).thenReturn(false);
            when(patientRepository.save(any())).thenReturn(patient);

            PatientResponse response = service.register(validRequest());

            assertThat(response).isNotNull();
            verify(patientRepository).save(any(Patient.class));
        }

        @Test
        @DisplayName("TC-PS-002: Duplicate phone number throws ConflictException")
        void duplicatePhone_throwsConflict() {
            when(patientRepository.existsByPhoneNumber("+233551234567")).thenReturn(true);

            assertThatThrownBy(() -> service.register(validRequest()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("phone number");

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-PS-003: Duplicate Ghana Card throws ConflictException")
        void duplicateGhanaCard_throwsConflict() {
            CreatePatientRequest req = validRequest();
            req.setGhanaCardNumber("GHA-987654321-0");
            when(patientRepository.existsByPhoneNumber(anyString())).thenReturn(false);
            when(patientRepository.existsByGhanaCardNumber("GHA-987654321-0")).thenReturn(true);

            assertThatThrownBy(() -> service.register(req))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Ghana Card");
        }

        @Test
        @DisplayName("TC-PS-004: No system user account created during staff-side registration")
        void staffRegistration_noSystemUserCreated() {
            when(patientRepository.existsByPhoneNumber(anyString())).thenReturn(false);
            when(patientRepository.save(any())).thenReturn(patient);

            service.register(validRequest());

            verify(userRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // findById()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("TC-PS-005: Existing patient returned")
        void existingPatient_returned() {
            when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));

            PatientResponse response = service.findById(100L);

            assertThat(response.getPatientId()).isEqualTo(100L);
            assertThat(response.getFirstName()).isEqualTo("Kwame");
        }

        @Test
        @DisplayName("TC-PS-006: Non-existent patient throws ResourceNotFoundException")
        void nonExistentPatient_throwsResourceNotFound() {
            when(patientRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // findByUserId()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByUserId()")
    class FindByUserId {

        @Test
        @DisplayName("TC-PS-007: Returns patient linked to system user")
        void linkedPatient_returned() {
            when(userRepository.findById(200L)).thenReturn(Optional.of(user));

            PatientResponse response = service.findByUserId(200L);

            assertThat(response.getPatientId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("TC-PS-008: User with no linked patient throws ResourceNotFoundException")
        void userWithNoPatient_throwsResourceNotFound() {
            SystemUser userWithNoPatient = new SystemUser();
            userWithNoPatient.setUserId(300L);
            userWithNoPatient.setPatient(null);

            when(userRepository.findById(300L)).thenReturn(Optional.of(userWithNoPatient));

            assertThatThrownBy(() -> service.findByUserId(300L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No patient profile linked");
        }

        @Test
        @DisplayName("TC-PS-009: Non-existent user throws ResourceNotFoundException")
        void nonExistentUser_throwsResourceNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByUserId(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // update()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("TC-PS-010: Phone number updated when no conflict exists")
        void phoneUpdate_noConflict_succeeds() {
            when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
            when(patientRepository.existsByPhoneNumber("+233249999999")).thenReturn(false);
            when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdatePatientRequest req = new UpdatePatientRequest();
            req.setPhoneNumber("+233249999999");

            PatientResponse response = service.update(100L, req);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("TC-PS-011: Updating to duplicate phone throws ConflictException")
        void duplicatePhoneUpdate_throwsConflict() {
            when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
            when(patientRepository.existsByPhoneNumber("+233249999999")).thenReturn(true);

            UpdatePatientRequest req = new UpdatePatientRequest();
            req.setPhoneNumber("+233249999999");

            assertThatThrownBy(() -> service.update(100L, req))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("phone number");
        }

        @Test
        @DisplayName("TC-PS-012: Updating non-existent patient throws ResourceNotFoundException")
        void nonExistentPatient_throwsResourceNotFound() {
            when(patientRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(999L, new UpdatePatientRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // listPatients()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listPatients()")
    class ListPatients {

        @Test
        @DisplayName("TC-PS-013: Returns paginated results when search term provided")
        void withSearchTerm_returnsPaginatedResults() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Patient> page = new PageImpl<>(List.of(patient));
            when(patientRepository.searchByNameOrPhone("Kwame", pageable)).thenReturn(page);

            Page<PatientResponse> result = service.search("Kwame", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getFirstName()).isEqualTo("Kwame");
        }

        @Test
        @DisplayName("TC-PS-014: Returns all patients when no search term provided")
        void noSearchTerm_returnsAll() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Patient> page = new PageImpl<>(List.of(patient));
            when(patientRepository.findAll(pageable)).thenReturn(page);

            Page<PatientResponse> result = service.search(null, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(patientRepository, never()).searchByNameOrPhone(any(), any());
        }
    }
}
