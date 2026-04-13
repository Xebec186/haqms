package com.haqms.controller;

import com.haqms.dto.request.CreatePatientRequest;
import com.haqms.dto.request.UpdatePatientRequest;
import com.haqms.dto.response.ApiResponse;
import com.haqms.dto.response.PatientResponse;
import com.haqms.entity.SystemUser;
import com.haqms.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    /**
     * POST /api/v1/patients
     * Staff-side patient registration. Patients self-register via /auth/register.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(
            @Valid @RequestBody CreatePatientRequest request) {

        PatientResponse response = patientService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Patient registered successfully"));
    }

    /**
     * GET /api/v1/patients/me
     * Returns the authenticated patient's own profile.
     * The patient_id is resolved from the JWT via the linked SystemUser.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PatientResponse>> getMyProfile(
            @AuthenticationPrincipal SystemUser currentUser) {

        PatientResponse response = patientService.findByUserId(currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Profile retrieved"));
    }

    /**
     * GET /api/v1/patients/{id}
     * Retrieves any patient by patient_id.
     * Accessible to staff and providers; patients access own record via /me.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(
            @PathVariable Long id) {

        PatientResponse response = patientService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Patient retrieved"));
    }

    /**
     * PATCH /api/v1/patients/{id}
     * Partial update — contact details only (phone, email, address).
     * Clinical data is immutable after registration.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePatientRequest request) {

        PatientResponse response = patientService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Patient updated"));
    }

    /**
     * GET /api/v1/patients?page=0&size=20&sort=lastName
     * Paginated patient list. Supports Spring Data Pageable.
     * Optional query param: ?search=kwame searches last/first name.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> listPatients(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {

        Page<PatientResponse> page = patientService.search(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Patients retrieved"));
    }
}

