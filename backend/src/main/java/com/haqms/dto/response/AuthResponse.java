package com.haqms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String role;
    private Long   userId;
    private Long   patientId;   // null if role is not PATIENT
    private Long   providerId;  // null if role is not PROVIDER
}
