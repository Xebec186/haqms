package com.haqms.controller;

import com.haqms.dto.request.CreateProviderRequest;
import com.haqms.dto.request.CreateSystemUserRequest;
import com.haqms.dto.request.UpdateProviderRequest;
import com.haqms.dto.request.UpdateUserStatusRequest;
import com.haqms.dto.response.*;
import com.haqms.service.AdminService;
import com.haqms.service.HealthcareProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService              adminService;
    private final HealthcareProviderService providerService;

    // ── User Management ───────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/users?page=0&size=20
     * Returns a paginated list of all system users.
     * Optional: ?role=PATIENT to filter by role name.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<SystemUserResponse>>> listUsers(
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {

        Page<SystemUserResponse> page = adminService.listUsers(role, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Users retrieved"));
    }

    /**
     * GET /api/v1/admin/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<SystemUserResponse>> getUserById(
            @PathVariable Long id) {

        SystemUserResponse response = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User retrieved"));
    }

    /**
     * PATCH /api/v1/admin/users/{id}/status
     * Activates or deactivates a user account (is_active flag).
     * Does not delete — aligns with the ON DELETE RESTRICT constraint in the schema.
     */
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<SystemUserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        SystemUserResponse response = adminService.updateUserStatus(id, request.getIsActive());
        return ResponseEntity.ok(ApiResponse.success(response, "User status updated"));
    }

    // ── Analytics ─────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/analytics/summary
     * Returns today's counts: total appointments, by status, active queues,
     * patients waiting. Designed to populate the admin dashboard cards.
     */
    @GetMapping("/analytics/summary")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getSummary() {

        AnalyticsSummaryResponse response = adminService.getDailySummary(LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success(response, "Summary retrieved"));
    }

    /**
     * GET /api/v1/admin/analytics/departments?from=2025-01-01&to=2025-01-31
     * Returns appointment counts grouped by department for the given date range.
     * Both params default to today if omitted.
     */
    @GetMapping("/analytics/departments")
    public ResponseEntity<ApiResponse<List<DepartmentAnalyticsResponse>>> getDepartmentAnalytics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end   = to   != null ? to   : LocalDate.now();
        List<DepartmentAnalyticsResponse> response =
                adminService.getDepartmentAnalytics(start, end);
        return ResponseEntity.ok(ApiResponse.success(response, "Analytics retrieved"));
    }

    // ── Provider Management ───────────────────────────────────────────────

    /**
     * GET /api/v1/admin/providers?departmentId=
     * Returns all providers, optionally filtered by department.
     */
    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<ProviderResponse>>> listProviders(
            @RequestParam(required = false) Long departmentId) {

        List<ProviderResponse> list = providerService.findAll(departmentId);
        return ResponseEntity.ok(ApiResponse.success(list, "Providers retrieved"));
    }

    @GetMapping("/providers/{id}")
    public ResponseEntity<ApiResponse<ProviderResponse>> getProviderById(
            Long providerId) {

        ProviderResponse provider = providerService.findById(providerId);
        return ResponseEntity.ok(ApiResponse.success(provider, "Provider retrieved"));
    }


    /**
     * POST /api/v1/admin/providers
     * Creates a new healthcare provider and optionally a linked PROVIDER
     * system user account in one transaction.
     */
    @PostMapping("/providers")
    public ResponseEntity<ApiResponse<ProviderResponse>> createProvider(
            @Valid @RequestBody CreateProviderRequest request) {

        ProviderResponse response = providerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Provider created"));
    }

    /**
     * PATCH /api/v1/admin/providers/{id}
     * Updates provider contact details, specialisation, or active status.
     * License number is immutable after creation.
     */
    @PatchMapping("/providers/{id}")
    public ResponseEntity<ApiResponse<ProviderResponse>> updateProvider(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProviderRequest request) {

        ProviderResponse response = providerService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Provider updated"));
    }

    /**
     * POST /api/v1/admin/users
     * Creates a new system user with any role.
     * Only ADMIN can create other ADMIN accounts.
     */
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<SystemUserResponse>> createUser(
            @Valid @RequestBody CreateSystemUserRequest request) {

        SystemUserResponse response = adminService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User account created successfully"));
    }
}
