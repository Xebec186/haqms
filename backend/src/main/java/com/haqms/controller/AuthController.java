package com.haqms.controller;

import com.haqms.dto.request.ChangePasswordRequest;
import com.haqms.dto.request.LoginRequest;
import com.haqms.dto.request.RegisterRequest;
import com.haqms.dto.response.ApiResponse;
import com.haqms.dto.response.AuthResponse;
import com.haqms.dto.response.PatientResponse;
import com.haqms.entity.SystemUser;
import com.haqms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/login
     * Validates credentials, returns a signed JWT with role and userId claims.
     * Public endpoint — no authentication required.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success(authResponse, "Login successful"));
    }

    /**
     * POST /api/v1/auth/register
     * Creates a Patient record and a linked PATIENT-role SystemUser in one transaction.
     * Public endpoint — no authentication required.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PatientResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        PatientResponse patientResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(patientResponse, "Registration successful. Please log in."));
    }

    /**
     * POST /api/v1/auth/change-password
     * Authenticated endpoint to change the current user's password.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal SystemUser currentUser) {

        authService.changePassword(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Password updated"));
    }
}
