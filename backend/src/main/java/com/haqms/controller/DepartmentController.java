package com.haqms.controller;

import com.haqms.dto.request.CreateDepartmentRequest;
import com.haqms.dto.request.CreateScheduleRequest;
import com.haqms.dto.request.UpdateDepartmentRequest;
import com.haqms.dto.request.UpdateScheduleRequest;
import com.haqms.dto.response.ApiResponse;
import com.haqms.dto.response.DepartmentResponse;
import com.haqms.dto.response.ProviderResponse;
import com.haqms.dto.response.ScheduleResponse;
import com.haqms.service.DepartmentService;
import com.haqms.service.ProviderScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService      departmentService;
    private final ProviderScheduleService scheduleService;

    // ── Departments ───────────────────────────────────────────────────────

    /**
     * GET /api/v1/departments
     * Returns all active departments. Used by the booking form to populate
     * the department drop-down.
     */
    @GetMapping("/api/v1/departments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAllDepartments() {
        return ResponseEntity.ok(
                ApiResponse.success(departmentService.findAllActive(), "Departments retrieved"));
    }

    /**
     * GET /api/v1/departments/{id}
     */
    @GetMapping("/api/v1/departments/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(departmentService.findById(id), "Department retrieved"));
    }

    /**
     * POST /api/v1/departments
     * Creates a new department. ADMIN only.
     */
    @PostMapping("/api/v1/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentResponse response = departmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Department created"));
    }

    /**
     * PATCH /api/v1/departments/{id}
     * Updates department name, description, or location. ADMIN only.
     */
    @PatchMapping("/api/v1/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        DepartmentResponse response = departmentService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Department updated"));
    }

    // ── Providers (nested under department) ───────────────────────────────

    /**
     * GET /api/v1/departments/{id}/providers
     * Returns all active healthcare providers in the given department.
     * Used by the booking form to populate the provider drop-down.
     */
    @GetMapping("/api/v1/departments/{id}/providers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ProviderResponse>>> getProvidersByDepartment(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        departmentService.findActiveProvidersByDepartment(id),
                        "Providers retrieved"));
    }

    // ── Provider Schedules ────────────────────────────────────────────────

    /**
     * GET /api/v1/providers/{id}/schedules
     * Returns available (is_available = true, future) schedules for a provider.
     * Used by the booking form to populate the time-slot drop-down.
     */
    @GetMapping("/api/v1/providers/{id}/schedules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getProviderSchedules(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        scheduleService.findAvailableByProvider(id),
                        "Schedules retrieved"));
    }

    /**
     * POST /api/v1/providers/{id}/schedules
     * Creates a new working schedule for a provider. ADMIN only.
     */
    @PostMapping("/api/v1/providers/{id}/schedules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @PathVariable Long id,
            @Valid @RequestBody CreateScheduleRequest request) {
        request.setProviderId(id);
        ScheduleResponse response = scheduleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Schedule created"));
    }

    /**
     * PATCH /api/v1/providers/{id}/schedules/{sid}
     * Updates max_slots or is_available on an existing schedule. ADMIN only.
     */
    @PatchMapping("/api/v1/providers/{id}/schedules/{sid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
            @PathVariable Long id,
            @PathVariable Long sid,
            @Valid @RequestBody UpdateScheduleRequest request) {
        ScheduleResponse response = scheduleService.update(sid, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Schedule updated"));
    }
}
