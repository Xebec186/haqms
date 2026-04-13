package com.haqms.service.impl;

import com.haqms.dto.response.QueueEntryResponse;
import com.haqms.dto.response.QueueResponse;
import com.haqms.entity.*;
import com.haqms.enums.*;
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
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QueueServiceImpl.
 *
 * Covers check-in validation, duplicate check-in prevention, priority-ordered
 * call-next logic, state transition enforcement, wait time calculation,
 * and queue lifecycle management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QueueServiceImpl")
class QueueServiceImplTest {

    @Mock private QueueRepository       queueRepository;
    @Mock private QueueEntryRepository  queueEntryRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private DepartmentRepository  departmentRepository;

    @InjectMocks
    private QueueServiceImpl service;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private Patient          patient;
    private Department       department;
    private Appointment      appointment;
    private Queue            queue;
    private QueueEntry       entry;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setDepartmentId(1L);
        department.setName("General Outpatient");

        patient = new Patient();
        patient.setPatientId(100L);
        patient.setFirstName("Kwame");
        patient.setLastName("Asante");

        HealthcareProvider provider = new HealthcareProvider();
        provider.setProviderId(10L);
        provider.setDepartment(department);

        appointment = new Appointment();
        appointment.setAppointmentId(1L);
        appointment.setPatient(patient);
        appointment.setProvider(provider);
        appointment.setDepartment(department);
        appointment.setAppointmentDate(LocalDate.now());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setAppointmentPriority(AppointmentPriority.REGULAR);

        queue = new Queue();
        queue.setQueueId(1L);
        queue.setDepartment(department);
        queue.setQueueDate(LocalDate.now());
        queue.setStatus(QueueStatus.OPEN);
        queue.setCurrentPosition(0);
        queue.setTotalRegistered(0);

        entry = new QueueEntry();
        entry.setEntryId(1L);
        entry.setQueue(queue);
        entry.setAppointment(appointment);
        entry.setPatient(patient);
        entry.setQueuePosition(1);
        entry.setStatus(QueueEntryStatus.WAITING);
        entry.setCheckedInAt(LocalDateTime.now().minusMinutes(30));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // checkIn()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("checkIn()")
    class CheckIn {

        @Test
        @DisplayName("TC-QS-001: Valid SCHEDULED appointment creates WAITING entry at position 1")
        void validScheduled_createsWaitingEntry() {
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
            when(queueEntryRepository.findByAppointmentAppointmentId(1L)).thenReturn(Optional.empty());
            when(queueRepository.findByProviderProviderIdAndQueueDate(1L, LocalDate.now()))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findMaxPositionInQueue(1L)).thenReturn(Optional.of(0));
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(queueRepository.save(any())).thenReturn(queue);
            when(appointmentRepository.save(any())).thenReturn(appointment);
            // Wait time calculations
            when(queueEntryRepository.countWaitingByQueueAndPriority(anyLong(), any())).thenReturn(0L);
            when(queueEntryRepository.countWaitingByQueuePriorityAndPositionLessThan(
                    anyLong(), any(), anyInt())).thenReturn(0L);

            QueueEntryResponse response = service.checkIn(1L);

            assertThat(response.getQueuePosition()).isEqualTo(1);
            assertThat(response.getStatus()).isEqualTo(QueueEntryStatus.WAITING);
            assertThat(response.getQueueId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("TC-QS-002: Valid CONFIRMED appointment can check in")
        void confirmedAppointment_checksInSuccessfully() {
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
            when(queueEntryRepository.findByAppointmentAppointmentId(1L)).thenReturn(Optional.empty());
            when(queueRepository.findByProviderProviderIdAndQueueDate(1L, LocalDate.now()))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findMaxPositionInQueue(1L)).thenReturn(Optional.empty());
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(queueRepository.save(any())).thenReturn(queue);
            when(appointmentRepository.save(any())).thenReturn(appointment);
            when(queueEntryRepository.countWaitingByQueueAndPriority(anyLong(), any())).thenReturn(0L);
            when(queueEntryRepository.countWaitingByQueuePriorityAndPositionLessThan(
                    anyLong(), any(), anyInt())).thenReturn(0L);

            assertThatNoException().isThrownBy(() -> service.checkIn(1L));
        }

        @Test
        @DisplayName("TC-QS-003: CANCELLED appointment throws ValidationException")
        void cancelledAppointment_throwsValidation() {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> service.checkIn(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("SCHEDULED or CONFIRMED");
        }

        @Test
        @DisplayName("TC-QS-004: COMPLETED appointment throws ValidationException")
        void completedAppointment_throwsValidation() {
            appointment.setStatus(AppointmentStatus.COMPLETED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> service.checkIn(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("SCHEDULED or CONFIRMED");
        }

        @Test
        @DisplayName("TC-QS-005: Duplicate check-in for same appointment throws ConflictException")
        void duplicateCheckIn_throwsConflict() {
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
            when(queueEntryRepository.findByAppointmentAppointmentId(1L))
                    .thenReturn(Optional.of(entry));  // already checked in

            assertThatThrownBy(() -> service.checkIn(1L))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already checked in");
        }

        @Test
        @DisplayName("TC-QS-006: Check-in to CLOSED queue throws ValidationException")
        void closedQueue_throwsValidation() {
            queue.setStatus(QueueStatus.CLOSED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
            when(queueEntryRepository.findByAppointmentAppointmentId(1L)).thenReturn(Optional.empty());
            when(queueRepository.findByProviderProviderIdAndQueueDate(1L, LocalDate.now()))
                    .thenReturn(Optional.of(queue));

            assertThatThrownBy(() -> service.checkIn(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("closed");
        }

        @Test
        @DisplayName("TC-QS-007: New queue created when none exists for department today")
        void noExistingQueue_newQueueCreated() {
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
            when(queueEntryRepository.findByAppointmentAppointmentId(1L)).thenReturn(Optional.empty());
            when(queueRepository.findByProviderProviderIdAndQueueDate(1L, LocalDate.now()))
                    .thenReturn(Optional.empty());   // no queue yet
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(queueRepository.save(any())).thenReturn(queue);
            when(queueEntryRepository.findMaxPositionInQueue(anyLong())).thenReturn(Optional.empty());
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(appointmentRepository.save(any())).thenReturn(appointment);
            when(queueEntryRepository.countWaitingByQueueAndPriority(anyLong(), any())).thenReturn(0L);
            when(queueEntryRepository.countWaitingByQueuePriorityAndPositionLessThan(
                    anyLong(), any(), anyInt())).thenReturn(0L);

            assertThatNoException().isThrownBy(() -> service.checkIn(1L));
            verify(queueRepository, times(2)).save(any()); // once to create, once to update total
        }

        @Test
        @DisplayName("TC-QS-008: Sequential positions assigned correctly")
        void secondCheckIn_assignsNextPosition() {
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
            when(queueEntryRepository.findByAppointmentAppointmentId(1L)).thenReturn(Optional.empty());
            when(queueRepository.findByProviderProviderIdAndQueueDate(1L, LocalDate.now()))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findMaxPositionInQueue(1L)).thenReturn(Optional.of(3)); // 3 already in queue
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(queueRepository.save(any())).thenReturn(queue);
            when(appointmentRepository.save(any())).thenReturn(appointment);
            when(queueEntryRepository.countWaitingByQueueAndPriority(anyLong(), any())).thenReturn(0L);
            when(queueEntryRepository.countWaitingByQueuePriorityAndPositionLessThan(
                    anyLong(), any(), anyInt())).thenReturn(0L);

            QueueEntryResponse response = service.checkIn(1L);

            assertThat(response.getQueuePosition()).isEqualTo(4); // max was 3, next is 4
        }

        @Test
        @DisplayName("TC-QS-009: Appointment not found throws ResourceNotFoundException")
        void appointmentNotFound_throwsResourceNotFound() {
            when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.checkIn(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Appointment not found");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // callNext() — priority ordering
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("callNext()")
    class CallNext {

        @Test
        @DisplayName("TC-QS-010: EMERGENCY entry returned before REGULAR regardless of position")
        void emergencyBeforeRegular() {
            QueueEntry emergencyEntry = buildEntry(5, AppointmentPriority.EMERGENCY, QueueEntryStatus.WAITING);
            emergencyEntry.setEntryId(5L);

            when(queueRepository.findById(1L)).thenReturn(Optional.of(queue));
            when(queueEntryRepository.findWaitingByQueueOrderedByPriority(
                    eq(1L), any(PageRequest.class)))
                    .thenReturn(List.of(emergencyEntry)); // priority query returns EMERGENCY first
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(queueRepository.save(any())).thenReturn(queue);

            QueueEntryResponse response = service.callNext(1L);

            assertThat(response.getStatus()).isEqualTo(QueueEntryStatus.CALLED);
            assertThat(response.getQueuePosition()).isEqualTo(5);

            ArgumentCaptor<QueueEntry> captor = ArgumentCaptor.forClass(QueueEntry.class);
            verify(queueEntryRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(QueueEntryStatus.CALLED);
        }

        @Test
        @DisplayName("TC-QS-011: URGENT entry returned before REGULAR")
        void urgentBeforeRegular() {
            QueueEntry urgentEntry = buildEntry(3, AppointmentPriority.URGENT, QueueEntryStatus.WAITING);
            urgentEntry.setEntryId(3L);

            when(queueRepository.findById(1L)).thenReturn(Optional.of(queue));
            when(queueEntryRepository.findWaitingByQueueOrderedByPriority(
                    eq(1L), any(PageRequest.class)))
                    .thenReturn(List.of(urgentEntry));
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(queueRepository.save(any())).thenReturn(queue);

            QueueEntryResponse response = service.callNext(1L);

            assertThat(response.getStatus()).isEqualTo(QueueEntryStatus.CALLED);
        }

        @Test
        @DisplayName("TC-QS-012: Empty queue throws ResourceNotFoundException")
        void emptyQueue_throwsResourceNotFound() {
            when(queueRepository.findById(1L)).thenReturn(Optional.of(queue));
            when(queueEntryRepository.findWaitingByQueueOrderedByPriority(
                    eq(1L), any(PageRequest.class)))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.callNext(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No patients are currently waiting");
        }

        @Test
        @DisplayName("TC-QS-013: PAUSED queue throws ValidationException")
        void pausedQueue_throwsValidation() {
            queue.setStatus(QueueStatus.PAUSED);
            when(queueRepository.findById(1L)).thenReturn(Optional.of(queue));

            assertThatThrownBy(() -> service.callNext(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("not open");
        }

        @Test
        @DisplayName("TC-QS-014: CLOSED queue throws ValidationException")
        void closedQueue_throwsValidation() {
            queue.setStatus(QueueStatus.CLOSED);
            when(queueRepository.findById(1L)).thenReturn(Optional.of(queue));

            assertThatThrownBy(() -> service.callNext(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("not open");
        }

        @Test
        @DisplayName("TC-QS-015: Current position updated after call-next")
        void callNext_updatesCurrentPosition() {
            QueueEntry nextEntry = buildEntry(2, AppointmentPriority.REGULAR, QueueEntryStatus.WAITING);
            nextEntry.setEntryId(2L);

            when(queueRepository.findById(1L)).thenReturn(Optional.of(queue));
            when(queueEntryRepository.findWaitingByQueueOrderedByPriority(
                    eq(1L), any(PageRequest.class)))
                    .thenReturn(List.of(nextEntry));
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.callNext(1L);

            ArgumentCaptor<Queue> queueCaptor = ArgumentCaptor.forClass(Queue.class);
            verify(queueRepository).save(queueCaptor.capture());
            assertThat(queueCaptor.getValue().getCurrentPosition()).isEqualTo(2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // startServing()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("startServing()")
    class StartServing {

        @Test
        @DisplayName("TC-QS-016: CALLED entry transitions to SERVING")
        void calledEntry_transitionsToServing() {
            entry.setStatus(QueueEntryStatus.CALLED);
            when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QueueEntryResponse response = service.startServing(1L);

            assertThat(response.getStatus()).isEqualTo(QueueEntryStatus.SERVING);
        }

        @Test
        @DisplayName("TC-QS-017: WAITING entry cannot start serving — throws ValidationException")
        void waitingEntry_throwsValidation() {
            entry.setStatus(QueueEntryStatus.WAITING);
            when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

            assertThatThrownBy(() -> service.startServing(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid state transition");
        }

        @Test
        @DisplayName("TC-QS-018: servingStartedAt timestamp recorded")
        void startServing_recordsTimestamp() {
            entry.setStatus(QueueEntryStatus.CALLED);
            when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.startServing(1L);

            assertThat(entry.getServingStartedAt()).isNotNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // completeEntry()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("completeEntry()")
    class CompleteEntry {

        @Test
        @DisplayName("TC-QS-019: SERVING entry transitions to COMPLETED")
        void servingEntry_transitionsToCompleted() {
            entry.setStatus(QueueEntryStatus.SERVING);
            entry.setCheckedInAt(LocalDateTime.now().minusMinutes(45));
            entry.setServingStartedAt(LocalDateTime.now().minusMinutes(5));
            when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(appointmentRepository.save(any())).thenReturn(appointment);

            QueueEntryResponse response = service.completeEntry(1L);

            assertThat(response.getStatus()).isEqualTo(QueueEntryStatus.COMPLETED);
        }

        @Test
        @DisplayName("TC-QS-020: CALLED entry cannot complete — throws ValidationException")
        void calledEntry_throwsValidation() {
            entry.setStatus(QueueEntryStatus.CALLED);
            when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

            assertThatThrownBy(() -> service.completeEntry(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid state transition");
        }

        @Test
        @DisplayName("TC-QS-021: waitMinutes calculated from checkedInAt to servingStartedAt")
        void completeEntry_calculatesWaitMinutes() {
            entry.setStatus(QueueEntryStatus.SERVING);
            LocalDateTime checkedIn     = LocalDateTime.now().minusMinutes(40);
            LocalDateTime servingStarted = LocalDateTime.now().minusMinutes(5);
            entry.setCheckedInAt(checkedIn);
            entry.setServingStartedAt(servingStarted);

            when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(appointmentRepository.save(any())).thenReturn(appointment);

            service.completeEntry(1L);

            // 40 - 5 = 35 minutes wait
            assertThat(entry.getWaitMinutes()).isEqualTo(35);
        }

        @Test
        @DisplayName("TC-QS-022: Linked appointment marked COMPLETED")
        void completeEntry_marksAppointmentCompleted() {
            entry.setStatus(QueueEntryStatus.SERVING);
            entry.setCheckedInAt(LocalDateTime.now().minusMinutes(30));
            entry.setServingStartedAt(LocalDateTime.now().minusMinutes(5));

            when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.completeEntry(1L);

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // markMissed()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("markMissed()")
    class MarkMissed {

        @Test
        @DisplayName("TC-QS-023: CALLED entry transitions to MISSED")
        void calledEntry_transitionsToMissed() {
            entry.setStatus(QueueEntryStatus.CALLED);
            when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(appointmentRepository.save(any())).thenReturn(appointment);

            QueueEntryResponse response = service.markMissed(1L);

            assertThat(response.getStatus()).isEqualTo(QueueEntryStatus.MISSED);
        }

        @Test
        @DisplayName("TC-QS-024: WAITING entry cannot be marked missed — throws ValidationException")
        void waitingEntry_throwsValidation() {
            entry.setStatus(QueueEntryStatus.WAITING);
            when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

            assertThatThrownBy(() -> service.markMissed(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid state transition");
        }

        @Test
        @DisplayName("TC-QS-025: Linked appointment marked NO_SHOW when missed")
        void markMissed_marksAppointmentNoShow() {
            entry.setStatus(QueueEntryStatus.CALLED);
            when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.markMissed(1L);

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // updateQueueStatus()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateQueueStatus()")
    class UpdateQueueStatus {

        @Test
        @DisplayName("TC-QS-026: OPEN queue paused successfully")
        void openQueue_pausedSuccessfully() {
            when(queueRepository.findById(1L)).thenReturn(Optional.of(queue));
            when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QueueResponse response = service.updateQueueStatus(1L, QueueStatus.PAUSED);

            assertThat(response.getStatus()).isEqualTo(QueueStatus.PAUSED);
        }

        @Test
        @DisplayName("TC-QS-027: Closing queue records closedAt timestamp")
        void closeQueue_recordsClosedAt() {
            when(queueRepository.findById(1L)).thenReturn(Optional.of(queue));
            when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateQueueStatus(1L, QueueStatus.CLOSED);

            assertThat(queue.getClosedAt()).isNotNull();
        }

        @Test
        @DisplayName("TC-QS-028: Queue not found throws ResourceNotFoundException")
        void queueNotFound_throwsResourceNotFound() {
            when(queueRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateQueueStatus(999L, QueueStatus.CLOSED))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Queue not found");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Estimated wait time calculation
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Estimated wait time")
    class EstimatedWait {

        @Test
        @DisplayName("TC-QS-029: REGULAR patient behind 2 EMERGENCY and 3 URGENT = 75 min wait")
        void regularPatient_behindEmergencyAndUrgent_correctWait() {
            appointment.setAppointmentPriority(AppointmentPriority.REGULAR);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
            when(queueEntryRepository.findByAppointmentAppointmentId(1L)).thenReturn(Optional.empty());
            when(queueRepository.findByProviderProviderIdAndQueueDate(1L, LocalDate.now()))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findMaxPositionInQueue(1L)).thenReturn(Optional.of(5));
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(queueRepository.save(any())).thenReturn(queue);
            when(appointmentRepository.save(any())).thenReturn(appointment);

            // 2 EMERGENCY ahead, 3 URGENT ahead, 0 same priority ahead
            when(queueEntryRepository.countWaitingByQueueAndPriority(1L, AppointmentPriority.EMERGENCY))
                    .thenReturn(2L);
            when(queueEntryRepository.countWaitingByQueueAndPriority(1L, AppointmentPriority.URGENT))
                    .thenReturn(3L);
            when(queueEntryRepository.countWaitingByQueuePriorityAndPositionLessThan(
                    1L, AppointmentPriority.REGULAR, 6)).thenReturn(0L);

            QueueEntryResponse response = service.checkIn(1L);

            // (2 + 3 + 0) * 15 min = 75 min
            assertThat(response.getEstimatedWaitMinutes()).isEqualTo(75);
        }

        @Test
        @DisplayName("TC-QS-030: EMERGENCY patient only waits behind other EMERGENCY entries")
        void emergencyPatient_onlyWaitsBehindEmergency() {
            appointment.setAppointmentPriority(AppointmentPriority.EMERGENCY);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
            when(queueEntryRepository.findByAppointmentAppointmentId(1L)).thenReturn(Optional.empty());
            when(queueRepository.findByProviderProviderIdAndQueueDate(1L, LocalDate.now()))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findMaxPositionInQueue(1L)).thenReturn(Optional.of(10));
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(queueRepository.save(any())).thenReturn(queue);
            when(appointmentRepository.save(any())).thenReturn(appointment);

            // 1 EMERGENCY already ahead; URGENT count should NOT be included
            when(queueEntryRepository.countWaitingByQueueAndPriority(1L, AppointmentPriority.EMERGENCY))
                    .thenReturn(1L);
            when(queueEntryRepository.countWaitingByQueuePriorityAndPositionLessThan(
                    1L, AppointmentPriority.EMERGENCY, 11)).thenReturn(0L);

            QueueEntryResponse response = service.checkIn(1L);

            // Only 1 EMERGENCY ahead * 15 min = 15 min
            assertThat(response.getEstimatedWaitMinutes()).isEqualTo(15);
            // URGENT count should never be called for EMERGENCY patients
            verify(queueEntryRepository, never())
                    .countWaitingByQueueAndPriority(1L, AppointmentPriority.URGENT);
        }

        @Test
        @DisplayName("TC-QS-031: First patient in empty queue has 0 min estimated wait")
        void firstPatient_zeroWait() {
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
            when(queueEntryRepository.findByAppointmentAppointmentId(1L)).thenReturn(Optional.empty());
            when(queueRepository.findByProviderProviderIdAndQueueDate(1L, LocalDate.now()))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findMaxPositionInQueue(1L)).thenReturn(Optional.empty());
            when(queueEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(queueRepository.save(any())).thenReturn(queue);
            when(appointmentRepository.save(any())).thenReturn(appointment);
            when(queueEntryRepository.countWaitingByQueueAndPriority(anyLong(), any())).thenReturn(0L);
            when(queueEntryRepository.countWaitingByQueuePriorityAndPositionLessThan(
                    anyLong(), any(), anyInt())).thenReturn(0L);

            QueueEntryResponse response = service.checkIn(1L);

            assertThat(response.getEstimatedWaitMinutes()).isEqualTo(0);
        }
    }

    // ── Builder helper ─────────────────────────────────────────────────────────

    private QueueEntry buildEntry(int position, AppointmentPriority priority, QueueEntryStatus status) {
        Appointment appt = new Appointment();
        appt.setAppointmentId((long) position);
        appt.setAppointmentPriority(priority);
        appt.setPatient(patient);

        QueueEntry e = new QueueEntry();
        e.setEntryId((long) position);
        e.setQueue(queue);
        e.setAppointment(appt);
        e.setPatient(patient);
        e.setQueuePosition(position);
        e.setStatus(status);
        e.setCheckedInAt(LocalDateTime.now());
        return e;
    }
}
