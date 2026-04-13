package com.haqms.dto.response;

import com.haqms.entity.SystemUser;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SystemUserResponse {

    private Long          userId;
    private String        username;
    private String        email;
    private String        roleName;
    private Boolean       isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private Long          patientId;   // null if not a PATIENT
    private Long          providerId;  // null if not a PROVIDER

    // password_hash is never included in responses

    public static SystemUserResponse from(SystemUser u) {
        return SystemUserResponse.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .email(u.getEmail())
                .roleName(u.getRole().getRoleName())
                .isActive(u.getIsActive())
                .lastLogin(u.getLastLogin())
                .createdAt(u.getCreatedAt())
                .patientId(u.getPatient()  != null ? u.getPatient().getPatientId()   : null)
                .providerId(u.getProvider() != null ? u.getProvider().getProviderId() : null)
                .build();
    }
}

