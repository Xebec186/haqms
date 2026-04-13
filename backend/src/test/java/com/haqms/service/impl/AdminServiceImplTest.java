package com.haqms.service.impl;

import com.haqms.dto.response.AnalyticsSummaryResponse;
import com.haqms.dto.response.SystemUserResponse;
import com.haqms.entity.*;
import com.haqms.enums.*;
import com.haqms.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminServiceImpl.
 *
 * Covers user management (list, get, activate/deactivate)
 * and analytics summary (appointment counts, queue waiting counts).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminServiceImpl")
class AdminServiceImplTest {

    @Mock private SystemUserRepository   userRepository;
    @Mock private AppointmentRepository  appointmentRepository;
    @Mock private QueueRepository        queueRepository;
    @Mock private QueueEntryRepository   queueEntryRepository;
    @Mock private DepartmentRepository   departmentRepository;

    @InjectMocks
    private AdminServiceImpl service;

    private SystemUser adminUser;
    private SystemUser patientUser;
    private Role       adminRole;
    private Role       patientRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setRoleId(1);
        adminRole.setRoleName("ADMIN");

        patientRole = new Role();
        patientRole.setRoleId(3);
        patientRole.setRoleName("PATIENT");

        adminUser = new SystemUser();
        adminUser.setUserId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@haqms.gh");
        adminUser.setRole(adminRole);
        adminUser.setIsActive(true);

        patientUser = new SystemUser();
        patientUser.setUserId(2L);
        patientUser.setUsername("patient_00001");
        patientUser.setEmail("patient1@mail.gh");
        patientUser.setRole(patientRole);
        patientUser.setIsActive(true);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // listUsers()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listUsers()")
    class ListUsers {

        @Test
        @DisplayName("TC-ADM-001: Returns all users when no role filter provided")
        void noRoleFilter_returnsAll() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<SystemUser> page = new PageImpl<>(List.of(adminUser, patientUser));
            when(userRepository.findAll(pageable)).thenReturn(page);

            Page<SystemUserResponse> result = service.listUsers(null, pageable);

            assertThat(result.getContent()).hasSize(2);
            verify(userRepository).findAll(pageable);
            verify(userRepository, never()).findByRoleRoleNameIgnoreCase(any(), any());
        }

        @Test
        @DisplayName("TC-ADM-002: Filters by role when role name provided")
        void withRoleFilter_returnsFiltered() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<SystemUser> page = new PageImpl<>(List.of(patientUser));
            when(userRepository.findByRoleRoleNameIgnoreCase("PATIENT", pageable)).thenReturn(page);

            Page<SystemUserResponse> result = service.listUsers("PATIENT", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getRoleName()).isEqualTo("PATIENT");
            verify(userRepository, never()).findAll((Pageable) any());
        }

        @Test
        @DisplayName("TC-ADM-003: Blank role filter returns all users (treated as null)")
        void blankRoleFilter_returnsAll() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<SystemUser> page = new PageImpl<>(List.of(adminUser, patientUser));
            when(userRepository.findAll(pageable)).thenReturn(page);

            Page<SystemUserResponse> result = service.listUsers("   ", pageable);

            assertThat(result.getContent()).hasSize(2);
            verify(userRepository).findAll(pageable);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getUserById()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("TC-ADM-004: Existing user returned with correct fields")
        void existingUser_returned() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

            SystemUserResponse response = service.getUserById(1L);

            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getUsername()).isEqualTo("admin");
            assertThat(response.getRoleName()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("TC-ADM-005: Non-existent user throws ResourceNotFoundException")
        void nonExistentUser_throwsResourceNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUserById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // updateUserStatus()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateUserStatus()")
    class UpdateUserStatus {

        @Test
        @DisplayName("TC-ADM-006: Active user deactivated — isActive set to false")
        void activeUser_deactivated() {
            patientUser.setIsActive(true);
            when(userRepository.findById(2L)).thenReturn(Optional.of(patientUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SystemUserResponse response = service.updateUserStatus(2L, false);

            ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getIsActive()).isFalse();
        }

        @Test
        @DisplayName("TC-ADM-007: Inactive user activated — isActive set to true")
        void inactiveUser_activated() {
            patientUser.setIsActive(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(patientUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateUserStatus(2L, true);

            ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getIsActive()).isTrue();
        }

        @Test
        @DisplayName("TC-ADM-008: Non-existent user throws ResourceNotFoundException")
        void nonExistentUser_throwsResourceNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateUserStatus(999L, false))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getAnalyticsSummary()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAnalyticsSummary()")
    class GetAnalyticsSummary {

        @Test
        @DisplayName("TC-ADM-009: Summary aggregates all appointment status counts correctly")
        void summary_aggregatesCorrectly() {
            LocalDate today = LocalDate.now();

            when(appointmentRepository.countByAppointmentDate(today)).thenReturn(50L);
            when(appointmentRepository.countByAppointmentDateAndStatus(today, AppointmentStatus.COMPLETED))
                    .thenReturn(30L);
            when(appointmentRepository.countByAppointmentDateAndStatus(today, AppointmentStatus.CANCELLED))
                    .thenReturn(5L);
            when(appointmentRepository.countByAppointmentDateAndStatus(today, AppointmentStatus.NO_SHOW))
                    .thenReturn(3L);
            when(appointmentRepository.countByAppointmentDateAndStatus(today, AppointmentStatus.SCHEDULED))
                    .thenReturn(8L);
            when(queueRepository.countByQueueDateAndStatus(today, QueueStatus.OPEN)).thenReturn(4L);
            when(queueEntryRepository.countByQueueQueueDateAndStatus(today, QueueEntryStatus.WAITING))
                    .thenReturn(12L);
            when(queueEntryRepository.countWaitingByDateAndPriority(today, AppointmentPriority.EMERGENCY))
                    .thenReturn(2L);
            when(queueEntryRepository.countWaitingByDateAndPriority(today, AppointmentPriority.URGENT))
                    .thenReturn(4L);

            AnalyticsSummaryResponse response = service.getAnalyticsSummary(today);

            assertThat(response.getTotalAppointments()).isEqualTo(50L);
            assertThat(response.getCompleted()).isEqualTo(30L);
            assertThat(response.getCancelled()).isEqualTo(5L);
            assertThat(response.getNoShow()).isEqualTo(3L);
            assertThat(response.getScheduled()).isEqualTo(8L);
            assertThat(response.getActiveQueues()).isEqualTo(4L);
            assertThat(response.getPatientsWaiting()).isEqualTo(12L);
            assertThat(response.getEmergencyWaiting()).isEqualTo(2L);
            assertThat(response.getUrgentWaiting()).isEqualTo(4L);
        }

        @Test
        @DisplayName("TC-ADM-010: Summary with no appointments returns all zeros")
        void emptyDay_returnsZeros() {
            LocalDate today = LocalDate.now();
            when(appointmentRepository.countByAppointmentDate(today)).thenReturn(0L);
            when(appointmentRepository.countByAppointmentDateAndStatus(any(), any())).thenReturn(0L);
            when(queueRepository.countByQueueDateAndStatus(any(), any())).thenReturn(0L);
            when(queueEntryRepository.countByQueueQueueDateAndStatus(any(), any())).thenReturn(0L);
            when(queueEntryRepository.countWaitingByDateAndPriority(any(), any())).thenReturn(0L);

            AnalyticsSummaryResponse response = service.getAnalyticsSummary(today);

            assertThat(response.getTotalAppointments()).isZero();
            assertThat(response.getPatientsWaiting()).isZero();
            assertThat(response.getEmergencyWaiting()).isZero();
        }
    }
}
