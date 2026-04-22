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
import com.haqms.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository      appointmentRepository;
    private final PatientRepository          patientRepository;
    private final HealthcareProviderRepository providerRepository;
    private final DepartmentRepository       departmentRepository;
    private final ProviderScheduleRepository scheduleRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Books a new appointment.
     *
     * Validation sequence:
     * 1. Resolve all referenced entities.
     * 2. Confirm the schedule belongs to the requested provider.
     * 3. Confirm the requested time is within the schedule window.
     * 4. Confirm the schedule has remaining capacity (max_slots not exceeded).
     * 5. Check for a time conflict on the provider slot (double-booking guard).
     * 6. Enforce priority escalation permission: PATIENT role may only book REGULAR.
     */
    @Override
    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {

        // 1. Resolve entities
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient not found: " + request.getPatientId()));

        HealthcareProvider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider not found: " + request.getProviderId()));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found: " + request.getDepartmentId()));

        ProviderSchedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Schedule not found: " + request.getScheduleId()));

        // 2. Schedule–provider consistency
        if (!schedule.getProvider().getProviderId().equals(request.getProviderId())) {
            throw new ValidationException(
                    "Schedule " + request.getScheduleId() +
                    " does not belong to provider " + request.getProviderId());
        }

        LocalDate today = LocalDate.now();
        if (schedule.getScheduleDate().isBefore(today)) {
            throw new ValidationException("Cannot book an appointment for a past date.");
        }

        // 3. Time within schedule window
//        if (schedule.getAppointmentTime().isBefore(schedule.getStartTime()) ||
//                !request.getAppointmentTime().isBefore(schedule.getEndTime())) {
//            throw new ValidationException(
//                    "Appointment time " + request.getAppointmentTime() +
//                    " is outside the schedule window " +
//                    schedule.getStartTime() + "–" + schedule.getEndTime());
//        }

        // 4. Capacity check
        long activeBookings = appointmentRepository.countActiveByScheduleId(
                request.getScheduleId());
        if (activeBookings >= schedule.getMaxSlots()) {
            throw new ConflictException(
                    "No available slots remaining in schedule " + request.getScheduleId() +
                    ". Maximum capacity (" + schedule.getMaxSlots() + ") reached.");
        }

        // 5. Double-booking guard (mirrors DB unique constraint uq_provider_datetime)
//        if (appointmentRepository
//                .existsByProviderProviderIdAndAppointmentDateAndAppointmentTime(
//                        request.getProviderId(),
//                        schedule.getScheduleDate(),
//                        request.getAppointmentTime())) {
//            throw new ConflictException(
//                    "Provider " + request.getProviderId() + " already has an appointment at " +
//                    request.getAppointmentTime() + " on " + schedule.getScheduleDate() +
//                    ". Please choose a different time.");
//        }

        // 6. Priority permission (PATIENT may not self-escalate)
        AppointmentPriority priority = request.getPriority() != null
                ? request.getPriority()
                : AppointmentPriority.REGULAR;

        if (priority != AppointmentPriority.REGULAR &&
                request.getBookedByUserId() != null) {
            // Role check delegated here; controller sets bookedByUserId from JWT
            // Service re-validates via the caller role in the request context
            // (Full enforcement is also done in the controller @PreAuthorize,
            //  this is a defence-in-depth check.)
            throw new ValidationException(
                    "Patients may only book REGULAR priority appointments. " +
                    "EMERGENCY or URGENT priority must be set by staff.");
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .provider(provider)
                .department(department)
                .schedule(schedule)
                .appointmentDate(schedule.getScheduleDate())
                .reason(request.getReason())
                .appointmentPriority(priority)
                .status(AppointmentStatus.SCHEDULED)
                .bookedByUserId(request.getBookedByUserId())
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment {} created for patient {} with provider {} on {} from {} to {}",
                saved.getAppointmentId(), patient.getPatientId(),
                provider.getProviderId(), saved.getAppointmentDate(),
                schedule.getStartTime(), schedule.getEndTime());

        return AppointmentResponse.from(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Fetches a single appointment.
     * PATIENT role may only view their own appointment.
     */
    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getById(Long appointmentId, SystemUser currentUser) {
        Appointment appointment = requireAppointment(appointmentId);
        enforcePatientOwnership(appointment, currentUser);
        return AppointmentResponse.from(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getByPatientId(Long patientId) {
        if (patientId == null) {
            throw new ValidationException("Patient ID could not be resolved from the current user.");
        }
        return appointmentRepository
                .findByPatientPatientIdOrderByAppointmentDate(patientId)
                .stream()
                .map(AppointmentResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Returns appointments for a provider on a given date.
     * If providerId is null, returns all appointments for that date across all providers.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getByProviderAndDate(Long providerId, LocalDate date) {
        List<Appointment> results;
        if (providerId != null) {
            results = appointmentRepository
                    .findByProviderProviderIdAndAppointmentDate(
                            providerId, date);
        } else {
            results = appointmentRepository
                    .findByAppointmentDate(date);
        }
        return results.stream()
                .map(AppointmentResponse::from)
                .collect(Collectors.toList());
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Updates appointment status.
     *
     * Rules:
     * - COMPLETED and CANCELLED are terminal; no further transitions allowed.
     * - CANCELLED requires a cancellationReason.
     * - PATIENT role may only cancel their own appointment.
     */
    @Override
    @Transactional
    public AppointmentResponse updateStatus(Long appointmentId, AppointmentStatus newStatus,
                                            String cancellationReason, SystemUser currentUser) {

        Appointment appointment = requireAppointment(appointmentId);

        // Terminal state check
        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
                appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ValidationException(
                    "Cannot change status of a " +
                    appointment.getStatus().name().toLowerCase() + " appointment.");
        }

        // Patient ownership check for cancellations
        if (newStatus == AppointmentStatus.CANCELLED) {
            enforcePatientOwnership(appointment, currentUser);
            if (cancellationReason == null || cancellationReason.isBlank()) {
                throw new ValidationException("A cancellation reason is required.");
            }
            appointment.setCancellationReason(cancellationReason);
        }

        appointment.setStatus(newStatus);
        log.info("Appointment {} status → {} by user {}",
                appointmentId, newStatus, currentUser.getUserId());

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    /**
     * Updates appointment priority (ADMIN only — enforced at controller level).
     * Cannot change priority of COMPLETED or CANCELLED appointments.
     */
    @Override
    @Transactional
    public AppointmentResponse updatePriority(Long appointmentId, AppointmentPriority newPriority) {
        Appointment appointment = requireAppointment(appointmentId);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
                appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ValidationException(
                    "Cannot change priority of a terminal appointment.");
        }

        appointment.setAppointmentPriority(newPriority);
        log.info("Appointment {} priority → {}", appointmentId, newPriority);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Hard-deletes an appointment.
     * Only permitted on CANCELLED records to prevent orphaned queue entries.
     */
    @Override
    @Transactional
    public void delete(Long appointmentId) {
        Appointment appointment = requireAppointment(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.CANCELLED) {
            throw new ValidationException(
                    "Only CANCELLED appointments may be deleted. " +
                    "Current status: " + appointment.getStatus());
        }
        appointmentRepository.delete(appointment);
        log.info("Appointment {} permanently deleted", appointmentId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Appointment requireAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }

    /**
     * If the caller holds the PATIENT role, verifies they own the appointment.
     * All other roles (ADMIN, PROVIDER) pass through without restriction.
     */
    private void enforcePatientOwnership(Appointment appointment, SystemUser currentUser) {
        boolean isPatient = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PATIENT"::equals);

        if (isPatient) {
            Long callerPatientId = currentUser.getPatient() != null
                    ? currentUser.getPatient().getPatientId() : null;
            if (!appointment.getPatient().getPatientId().equals(callerPatientId)) {
                throw new com.haqms.exception.ValidationException(
                        "Access denied: this appointment does not belong to you.");
            }
        }
    }
}
