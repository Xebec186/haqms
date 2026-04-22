package com.haqms.controller;

import com.haqms.dto.request.CreateAppointmentRequest;
import com.haqms.dto.request.UpdatePriorityRequest;
import com.haqms.dto.request.UpdateStatusRequest;
import com.haqms.dto.response.ApiResponse;
import com.haqms.dto.response.AppointmentResponse;
import com.haqms.entity.SystemUser;
import com.haqms.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * POST /api/v1/appointments
     * Books a new appointment. Conflict detection and slot validation
     * are enforced in AppointmentService.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            @AuthenticationPrincipal SystemUser currentUser) {

        // Attach the booking user ID for audit trail (booked_by_user_id column)
        request.setBookedByUserId(currentUser.getUserId());
        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Appointment booked successfully"));
    }

    /**
     * GET /api/v1/appointments/my
     * Returns all appointments for the authenticated patient,
     * ordered by date descending.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getMyAppointments(
            @AuthenticationPrincipal SystemUser currentUser) {

        Long patientId = currentUser.getPatient() != null
                ? currentUser.getPatient().getPatientId() : null;
        List<AppointmentResponse> list = appointmentService.getByPatientId(patientId);
        return ResponseEntity.ok(ApiResponse.success(list, "Appointments retrieved"));
    }

    /**
     * GET /api/v1/appointments/{id}
     * Retrieves a single appointment. Patients may only fetch their own.
     * Ownership check is enforced in the service layer.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER','PATIENT')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(
            @PathVariable Long id,
            @AuthenticationPrincipal SystemUser currentUser) {

        AppointmentResponse response = appointmentService.getById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Appointment retrieved"));
    }

    /**
     * GET /api/v1/appointments?providerId=&date=
     * Staff/provider query — filter appointments by provider and/or date.
     * Both params are optional; omitting both returns today's appointments.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> listAppointments(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<AppointmentResponse> list = appointmentService
                .getByProviderAndDate(providerId, queryDate);
        return ResponseEntity.ok(ApiResponse.success(list, "Appointments retrieved"));
    }

    /**
     * PATCH /api/v1/appointments/{id}/status
     * Updates appointment status (e.g. CONFIRMED, CANCELLED, NO_SHOW).
     * Terminal states (COMPLETED, CANCELLED) are rejected for further changes
     * by the service layer. Cancellation requires a reason.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER','PATIENT')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal SystemUser currentUser) {

        AppointmentResponse response = appointmentService.updateStatus(
                id, request.getStatus(), request.getCancellationReason(), currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Status updated"));
    }

    /**
     * PATCH /api/v1/appointments/{id}/priority
     * Escalates or downgrade appointment priority (EMERGENCY / URGENT / REGULAR).
     * Restricted to ADMIN — patients cannot self-escalate.
     */
    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updatePriority(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePriorityRequest request) {

        AppointmentResponse response = appointmentService
                .updatePriority(id, request.getPriority());
        return ResponseEntity.ok(ApiResponse.success(response, "Priority updated"));
    }

    /**
     * DELETE /api/v1/appointments/{id}
     * Hard-deletes an appointment. Only permitted on CANCELLED records
     * (enforced in service). Restricted to ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAppointment(
            @PathVariable Long id) {

        appointmentService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null, "Appointment deleted"));
    }
}
