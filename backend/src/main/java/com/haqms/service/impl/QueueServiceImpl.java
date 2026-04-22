package com.haqms.service.impl;

import com.haqms.dto.response.QueueEntryResponse;
import com.haqms.dto.response.QueueResponse;
import com.haqms.entity.*;
import com.haqms.enums.AppointmentPriority;
import com.haqms.enums.AppointmentStatus;
import com.haqms.enums.QueueEntryStatus;
import com.haqms.enums.QueueStatus;
import com.haqms.exception.ConflictException;
import com.haqms.exception.ResourceNotFoundException;
import com.haqms.exception.ValidationException;
import com.haqms.repository.*;
import com.haqms.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueServiceImpl implements QueueService {

    private final QueueRepository         queueRepository;
    private final QueueEntryRepository    queueEntryRepository;
    private final AppointmentRepository   appointmentRepository;
    private final DepartmentRepository    departmentRepository;
    private final HealthcareProviderRepository providerRepository;

    /** Average minutes per consultation — used for estimated wait time calculation. */
    private static final int AVG_SERVICE_MINUTES = 15;

    // ── Check-in ──────────────────────────────────────────────────────────────

    /**
     * Checks a patient into the daily department queue.
     *
     * Steps:
     * 1. Validate the appointment is SCHEDULED or CONFIRMED.
     * 2. Prevent duplicate check-in.
     * 3. Get or create today's department queue.
     * 4. Assign the next sequential position.
     * 5. Compute priority-aware estimated wait time.
     * 6. Transition appointment status to CONFIRMED.
     */
    @Override
    @Transactional
    public QueueEntryResponse checkIn(Long appointmentId) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found: " + appointmentId));

        if (!appointment.getAppointmentDate().equals(LocalDate.now())) {
            throw new ValidationException("Check-in is only available on the scheduled date.");
        }

        // 1. Status validation
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED &&
                appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ValidationException(
                    "Only SCHEDULED or CONFIRMED appointments can check in. " +
                    "Current status: " + appointment.getStatus());
        }

        // 2. Duplicate check-in guard
        if (queueEntryRepository
                .findByAppointmentAppointmentId(appointmentId).isPresent()) {
            throw new ConflictException(
                    "Patient has already checked in for appointment " + appointmentId);
        }

        // 3. Get or create today's queue for this provider
        Queue queue = getOrCreateQueue(
                appointment.getProvider().getProviderId(),
                appointment.getAppointmentDate());

        if (queue.getStatus() == QueueStatus.CLOSED) {
            throw new ValidationException(
                    "The queue for this department is closed for today.");
        }

        // 4. Assign next position
        int nextPosition = queueEntryRepository
                .findMaxPositionInQueue(queue.getQueueId())
                .orElse(0) + 1;

        QueueEntry entry = QueueEntry.builder()
                .queue(queue)
                .appointment(appointment)
                .patient(appointment.getPatient())
                .queuePosition(nextPosition)
                .status(QueueEntryStatus.WAITING)
                .checkedInAt(LocalDateTime.now())
                .build();

        // Update queue total
        queue.setTotalRegistered(queue.getTotalRegistered() + 1);
        queueRepository.save(queue);

        // Transition appointment to CONFIRMED on check-in
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);

        QueueEntry saved = queueEntryRepository.save(entry);

        // 5. Priority-aware estimated wait
        int estimatedWait = calculateEstimatedWait(
                queue.getQueueId(),
                appointment.getAppointmentPriority(),
                nextPosition,
                queue.getCurrentPosition());

        log.info("Patient {} checked in at position {} (priority={}) in queue {}",
                appointment.getPatient().getPatientId(), nextPosition,
                appointment.getAppointmentPriority(), queue.getQueueId());

        return QueueEntryResponse.from(saved, estimatedWait);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public QueueResponse getQueueById(Long queueId) {
        return QueueResponse.from(requireQueue(queueId));
    }

    @Override
    @Transactional(readOnly = true)
    public QueueEntryResponse getQueueEntryByAppointmentId(Long appointmentId, SystemUser currentUser) {

        QueueEntry entry = queueEntryRepository
                .findByAppointment_AppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found"));

        // Enforce ownership
        boolean isPatient = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));

        if (isPatient) {
            Long patientId = currentUser.getPatient().getPatientId();

            if (!entry.getPatient().getPatientId().equals(patientId)) {
                throw new ValidationException("Access denied: not your queue entry");
            }
        }

        // ✅ Use existing data (no extra DB call)
        Appointment appointment = entry.getAppointment();
        Queue queue = entry.getQueue();

        int estimatedWait = calculateEstimatedWait(
                queue.getQueueId(),
                appointment.getAppointmentPriority(),
                entry.getQueuePosition(),
                queue.getCurrentPosition()
        );

        return QueueEntryResponse.from(entry, estimatedWait);
    }

    /**
     * Returns all entries for a queue sorted by priority then position.
     * EMERGENCY → URGENT → REGULAR; FIFO within each tier.
     */
    @Override
    @Transactional(readOnly = true)
    public List<QueueEntryResponse> getEntriesByQueue(Long queueId) {
        requireQueue(queueId);
        return queueEntryRepository
                .findAllByQueueOrderedByPriority(queueId)
                .stream()
                .map(e -> QueueEntryResponse.from(e, 0))
                .collect(Collectors.toList());
    }

    @Override
    public List<QueueEntryResponse> getEntriesByProvider(Long providerId) {
        return queueEntryRepository
                .findAllByProviderOrderedByPriority(providerId)
                .stream()
                .map(e -> QueueEntryResponse.from(e, 0))
                .collect(Collectors.toList());
    }

    // ── Queue progression ─────────────────────────────────────────────────────

    /**
     * Calls the next patient using priority ordering:
     * EMERGENCY first, then URGENT, then REGULAR; FIFO within each tier.
     * Transitions the selected entry from WAITING → CALLED.
     */
    @Override
    @Transactional
    public QueueEntryResponse callNext(Long queueId) {

        Queue queue = requireQueue(queueId);

        if (queue.getStatus() != QueueStatus.OPEN) {
            throw new ValidationException(
                    "Queue " + queueId + " is not open (current status: " + queue.getStatus() + ")");
        }

        List<QueueEntry> priorityWaiting = queueEntryRepository
                .findWaitingByQueueOrderedByPriority(queueId, PageRequest.of(0, 1));

        if (priorityWaiting.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No patients are currently waiting in queue " + queueId);
        }

        QueueEntry next = priorityWaiting.get(0);
        next.setStatus(QueueEntryStatus.CALLED);
        next.setCalledAt(LocalDateTime.now());

        queue.setCurrentPosition(next.getQueuePosition());
        queueRepository.save(queue);

        if (next.getAppointment().getAppointmentPriority() == AppointmentPriority.EMERGENCY) {
            log.warn("EMERGENCY patient called: patientId={}, appointmentId={}, queueId={}",
                    next.getPatient().getPatientId(),
                    next.getAppointment().getAppointmentId(),
                    queueId);
        } else {
            log.info("Called patient at position {} (priority={}) in queue {}",
                    next.getQueuePosition(),
                    next.getAppointment().getAppointmentPriority(),
                    queueId);
        }

        return QueueEntryResponse.from(queueEntryRepository.save(next), 0);
    }

    /**
     * Transitions a CALLED entry to SERVING (consultation started).
     * Records serving_started_at timestamp.
     */
    @Override
    @Transactional
    public QueueEntryResponse startServing(Long entryId) {
        QueueEntry entry = requireEntry(entryId);
        requireStatus(entry, QueueEntryStatus.CALLED);

        entry.setStatus(QueueEntryStatus.SERVING);
        entry.setServingStartedAt(LocalDateTime.now());

        log.info("Started serving entry {} (patient {})",
                entryId, entry.getPatient().getPatientId());
        return QueueEntryResponse.from(queueEntryRepository.save(entry), 0);
    }

    /**
     * Transitions a SERVING entry to COMPLETED.
     * Records completed_at, computes actual wait_minutes, and marks the linked
     * appointment as COMPLETED.
     */
    @Override
    @Transactional
    public QueueEntryResponse completeEntry(Long entryId) {
        QueueEntry entry = requireEntry(entryId);
        requireStatus(entry, QueueEntryStatus.SERVING);

        LocalDateTime now = LocalDateTime.now();
        entry.setStatus(QueueEntryStatus.COMPLETED);
        entry.setCompletedAt(now);

        // Compute actual wait time: check-in → serving started
        if (entry.getCheckedInAt() != null && entry.getServingStartedAt() != null) {
            long mins = ChronoUnit.MINUTES.between(
                    entry.getCheckedInAt(), entry.getServingStartedAt());
            entry.setWaitMinutes((int) mins);
        }

        // Complete the linked appointment
        Appointment appointment = entry.getAppointment();
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        log.info("Completed entry {} — wait time: {} min",
                entryId, entry.getWaitMinutes());
        return QueueEntryResponse.from(queueEntryRepository.save(entry), 0);
    }

    /**
     * Transitions a CALLED entry to MISSED (patient did not attend when called).
     * Marks the linked appointment as NO_SHOW.
     */
    @Override
    @Transactional
    public QueueEntryResponse markMissed(Long entryId) {
        QueueEntry entry = requireEntry(entryId);
        requireStatus(entry, QueueEntryStatus.CALLED);

        entry.setStatus(QueueEntryStatus.MISSED);

        Appointment appointment = entry.getAppointment();
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        appointmentRepository.save(appointment);

        log.info("Entry {} marked MISSED (patient {})",
                entryId, entry.getPatient().getPatientId());
        return QueueEntryResponse.from(queueEntryRepository.save(entry), 0);
    }

    /**
     * Opens, pauses, or closes a queue.
     * Records closed_at when transitioning to CLOSED.
     */
    @Override
    @Transactional
    public QueueResponse updateQueueStatus(Long queueId, QueueStatus newStatus) {
        Queue queue = requireQueue(queueId);
        queue.setStatus(newStatus);
        if (newStatus == QueueStatus.CLOSED && queue.getClosedAt() == null) {
            queue.setClosedAt(LocalDateTime.now());
        }
        if (newStatus == QueueStatus.OPEN && queue.getOpenedAt() == null) {
            queue.setOpenedAt(LocalDateTime.now());
        }
        log.info("Queue {} status → {}", queueId, newStatus);
        return QueueResponse.from(queueRepository.save(queue));
    }

    @Override
    @Transactional(readOnly = true)
    public QueueResponse getTodaysQueueByProvider(Long providerId) {
        return queueRepository
                .findByProviderProviderIdAndQueueDate(providerId, LocalDate.now())
                .map(QueueResponse::from)
                .orElse(null);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Retrieves the existing daily queue for a provider, or creates a new OPEN one.
     */
    private Queue getOrCreateQueue(Long providerId, LocalDate date) {
        return queueRepository
                .findByProviderProviderIdAndQueueDate(providerId, date)
                .orElseGet(() -> {
                    HealthcareProvider provider = providerRepository.findById(providerId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Provider not found: " + providerId));
                    Queue newQueue = Queue.builder()
                            .provider(provider)
                            .queueDate(date)
                            .status(QueueStatus.OPEN)
                            .currentPosition(0)
                            .totalRegistered(0)
                            .openedAt(LocalDateTime.now())
                            .build();
                    log.info("Created new queue for provider {} on {}", providerId, date);
                    return queueRepository.save(newQueue);
                });
    }

    /**
     * Calculates estimated wait time in minutes using priority-aware logic.
     *
     * A REGULAR patient waits behind:
     *   all EMERGENCY entries ahead + all URGENT entries ahead + same-priority entries ahead.
     * An URGENT patient waits behind:
     *   all EMERGENCY entries ahead + same-priority entries ahead.
     * An EMERGENCY patient only waits behind other EMERGENCY entries ahead.
     */
    private int calculateEstimatedWait(Long queueId,
                                       AppointmentPriority priority,
                                       int newPosition,
                                       int currentPosition) {
        long emergencyAhead = queueEntryRepository
                .countWaitingByQueueAndPriority(queueId, AppointmentPriority.EMERGENCY);

        long urgentAhead = 0;
        if (priority == AppointmentPriority.REGULAR) {
            urgentAhead = queueEntryRepository
                    .countWaitingByQueueAndPriority(queueId, AppointmentPriority.URGENT);
        }

        long sameAhead = queueEntryRepository
                .countWaitingByQueuePriorityAndPositionLessThan(queueId, priority, newPosition);

        // Exclude this patient's own entry from sameAhead
        // (position was just assigned so they are not yet in the WAITING count above,
        //  but guard for race conditions)
        long totalAhead = emergencyAhead + urgentAhead + sameAhead;

        return (int) Math.max(0, totalAhead * AVG_SERVICE_MINUTES);
    }

    private Queue requireQueue(Long id) {
        return queueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Queue not found: " + id));
    }

    private QueueEntry requireEntry(Long id) {
        return queueEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found: " + id));
    }

    private void requireStatus(QueueEntry entry, QueueEntryStatus expected) {
        if (entry.getStatus() != expected) {
            throw new ValidationException(
                    "Expected entry status " + expected +
                    " but found " + entry.getStatus() +
                    ". Invalid state transition.");
        }
    }
}
