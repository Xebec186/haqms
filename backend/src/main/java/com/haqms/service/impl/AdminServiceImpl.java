package com.haqms.service.impl;

import com.haqms.dto.request.CreateSystemUserRequest;
import com.haqms.dto.response.AnalyticsSummaryResponse;
import com.haqms.dto.response.DepartmentAnalyticsResponse;
import com.haqms.dto.response.SystemUserResponse;
import com.haqms.entity.HealthcareProvider;
import com.haqms.entity.Patient;
import com.haqms.entity.Role;
import com.haqms.entity.SystemUser;
import com.haqms.enums.AppointmentPriority;
import com.haqms.enums.AppointmentStatus;
import com.haqms.enums.QueueEntryStatus;
import com.haqms.enums.QueueStatus;
import com.haqms.exception.ConflictException;
import com.haqms.exception.ResourceNotFoundException;
import com.haqms.repository.*;
import com.haqms.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final SystemUserRepository   userRepository;
    private final AppointmentRepository  appointmentRepository;
    private final QueueRepository        queueRepository;
    private final QueueEntryRepository   queueEntryRepository;
    private final DepartmentRepository   departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final HealthcareProviderRepository providerRepository;
    private final PatientRepository patientRepository;

    // ── User management ───────────────────────────────────────────────────────

    /**
     * Returns a paginated list of system users.
     * If roleName is provided, filters to that role only (case-insensitive).
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SystemUserResponse> listUsers(String roleName, Pageable pageable) {
        if (roleName != null && !roleName.isBlank()) {
            return userRepository
                    .findByRoleRoleNameIgnoreCase(roleName, pageable)
                    .map(SystemUserResponse::from);
        }
        return userRepository.findAll(pageable).map(SystemUserResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemUserResponse getUserById(Long userId) {
        return SystemUserResponse.from(requireUser(userId));
    }

    /**
     * Activates or deactivates a system user account (is_active flag).
     * Deactivation is preferred over deletion to respect referential integrity
     * (booked_by_user_id foreign key on appointments).
     */
    @Override
    @Transactional
    public SystemUserResponse updateUserStatus(Long userId, Boolean isActive) {
        SystemUser user = requireUser(userId);
        user.setIsActive(isActive);
        log.info("User {} is_active set to {}", userId, isActive);
        return SystemUserResponse.from(userRepository.save(user));
    }


    @Override
    @Transactional
    public SystemUserResponse createUser(CreateSystemUserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException(
                    "Username '" + request.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(
                    "Email '" + request.getEmail() + "' is already registered.");
        }

        Role role = roleRepository.findByRoleName(request.getRoleName().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role not found: " + request.getRoleName()));

        SystemUser user = SystemUser.builder()
                .role(role)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .isActive(true)
                .build();

        // Link to provider record if role is PROVIDER
        if (request.getProviderId() != null) {
            HealthcareProvider provider = providerRepository
                    .findById(request.getProviderId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Provider not found: " + request.getProviderId()));
            user.setProvider(provider);
        }

        // Link to patient record if role is PATIENT
        if (request.getPatientId() != null) {
            Patient patient = patientRepository
                    .findById(request.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Patient not found: " + request.getPatientId()));
            user.setPatient(patient);
        }

        log.info("Admin created new {} account: {}", role.getRoleName(), request.getUsername());
        return SystemUserResponse.from(userRepository.save(user));
    }

    // ── Analytics ─────────────────────────────────────────────────────────────

    /**
     * Produces a summary of appointment and queue statistics for a given date.
     * Designed to populate the admin dashboard cards (total, by-status, waiting counts).
     */
    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getDailySummary(LocalDate date) {

        long total      = appointmentRepository.countByAppointmentDate(date);
        long scheduled  = appointmentRepository.countByAppointmentDateAndStatus(date, AppointmentStatus.SCHEDULED);
        long confirmed  = appointmentRepository.countByAppointmentDateAndStatus(date, AppointmentStatus.CONFIRMED);
        long completed  = appointmentRepository.countByAppointmentDateAndStatus(date, AppointmentStatus.COMPLETED);
        long cancelled  = appointmentRepository.countByAppointmentDateAndStatus(date, AppointmentStatus.CANCELLED);
        long noShow     = appointmentRepository.countByAppointmentDateAndStatus(date, AppointmentStatus.NO_SHOW);

        long activeQueues = queueRepository.countByQueueDateAndStatus(date, QueueStatus.OPEN);

        long waiting   = queueEntryRepository.countByQueueQueueDateAndStatus(date, QueueEntryStatus.WAITING);
        long emergency = queueEntryRepository.countWaitingByDateAndPriority(date, AppointmentPriority.EMERGENCY);
        long urgent    = queueEntryRepository.countWaitingByDateAndPriority(date, AppointmentPriority.URGENT);

        return AnalyticsSummaryResponse.builder()
                .date(date)
                .totalAppointments(total)
                .scheduled(scheduled)
                .confirmed(confirmed)
                .completed(completed)
                .cancelled(cancelled)
                .noShow(noShow)
                .activeQueues(activeQueues)
                .patientsWaiting(waiting)
                .emergencyWaiting(emergency)
                .urgentWaiting(urgent)
                .build();
    }

    /**
     * Returns per-department appointment counts and average wait times
     * for the given date range, ordered by total appointments descending.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DepartmentAnalyticsResponse> getDepartmentAnalytics(LocalDate from, LocalDate to) {
        return departmentRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(dept -> {
                    Long deptId = dept.getDepartmentId();
                    long totalAppts = appointmentRepository
                            .countByDepartmentDepartmentIdAndAppointmentDateBetween(deptId, from, to);
                    long completedAppts = appointmentRepository
                            .countByDepartmentDepartmentIdAndAppointmentDateBetweenAndStatus(
                                    deptId, from, to, AppointmentStatus.COMPLETED);
                    long cancelledAppts = appointmentRepository
                            .countByDepartmentDepartmentIdAndAppointmentDateBetweenAndStatus(
                                    deptId, from, to, AppointmentStatus.CANCELLED);
                    Double avgWait = queueEntryRepository
                            .findAverageWaitMinutesByDepartmentAndDateRange(deptId, from, to);

                    return new DepartmentAnalyticsResponse(
                            deptId,
                            dept.getName(),
                            totalAppts,
                            completedAppts,
                            cancelledAppts,
                            avgWait != null ? Math.round(avgWait * 100.0) / 100.0 : 0.0
                    );
                })
                .sorted((a, b) -> Long.compare(b.getTotalAppointments(), a.getTotalAppointments()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getAnalyticsSummary(LocalDate date) {

        // Fetch appointment and queue-related statistics
        long totalAppointments = appointmentRepository.countByAppointmentDate(date);
        long scheduled = appointmentRepository.countByAppointmentDateAndStatus(date, AppointmentStatus.SCHEDULED);
        long completed = appointmentRepository.countByAppointmentDateAndStatus(date, AppointmentStatus.COMPLETED);
        long cancelled = appointmentRepository.countByAppointmentDateAndStatus(date, AppointmentStatus.CANCELLED);
        long noShow = appointmentRepository.countByAppointmentDateAndStatus(date, AppointmentStatus.NO_SHOW);
        long activeQueues = queueRepository.countByQueueDateAndStatus(date, QueueStatus.OPEN);
        long patientsWaiting = queueEntryRepository.countByQueueQueueDateAndStatus(date, QueueEntryStatus.WAITING);
        long emergencyWaiting = queueEntryRepository.countWaitingByDateAndPriority(date, AppointmentPriority.EMERGENCY);
        long urgentWaiting = queueEntryRepository.countWaitingByDateAndPriority(date, AppointmentPriority.URGENT);

        // Build and return the analytics summary response
        return AnalyticsSummaryResponse.builder()
                .date(date)
                .totalAppointments(totalAppointments)
                .scheduled(scheduled)
                .completed(completed)
                .cancelled(cancelled)
                .noShow(noShow)
                .activeQueues(activeQueues)
                .patientsWaiting(patientsWaiting)
                .emergencyWaiting(emergencyWaiting)
                .urgentWaiting(urgentWaiting)
                .build();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private SystemUser requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
