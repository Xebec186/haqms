package com.haqms.service;

import com.haqms.dto.request.ChangePasswordRequest;
import com.haqms.dto.request.LoginRequest;
import com.haqms.dto.request.RegisterRequest;
import com.haqms.dto.response.AuthResponse;
import com.haqms.dto.response.PatientResponse;
import com.haqms.entity.SystemUser;

public interface AuthService {

    /**
     * Validates credentials and returns a signed JWT with role/userId claims.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Creates a Patient record and a linked PATIENT-role SystemUser in one transaction.
     */
    PatientResponse register(RegisterRequest request);

    /**
     * Change password for the authenticated user.
     */
    void changePassword(ChangePasswordRequest request, SystemUser currentUser);
}
