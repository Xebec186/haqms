package com.haqms.dto.response;

import com.haqms.entity.SystemUser;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {

    private Long userId;
    private String username;
    private String email;
    private String roleName;
    private Boolean isActive;
    
    // Additional profile info
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address; // only for patient
    private String specialisation; // only for provider
    private String licenseNumber; // only for provider

    public static UserProfileResponse from(SystemUser u) {
        UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .email(u.getEmail())
                .roleName(u.getRole().getRoleName())
                .isActive(u.getIsActive());

        if (u.getPatient() != null) {
            builder.firstName(u.getPatient().getFirstName())
                   .lastName(u.getPatient().getLastName())
                   .phoneNumber(u.getPatient().getPhoneNumber())
                   .address(u.getPatient().getAddress());
        } else if (u.getProvider() != null) {
            builder.firstName(u.getProvider().getFirstName())
                   .lastName(u.getProvider().getLastName())
                   .phoneNumber(u.getProvider().getPhoneNumber())
                   .specialisation(u.getProvider().getSpecialisation())
                   .licenseNumber(u.getProvider().getLicenseNumber());
        }

        return builder.build();
    }
}
