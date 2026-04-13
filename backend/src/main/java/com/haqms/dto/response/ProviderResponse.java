package com.haqms.dto.response;

import com.haqms.entity.HealthcareProvider;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderResponse {

    private Long   providerId;
    private Long   departmentId;
    private String departmentName;
    private String firstName;
    private String lastName;
    private String specialisation;
    private String licenseNumber;
    private String phoneNumber;
    private String email;
    private Boolean isActive;

    public static ProviderResponse from(HealthcareProvider p) {
        return ProviderResponse.builder()
                .providerId(p.getProviderId())
                .departmentId(p.getDepartment().getDepartmentId())
                .departmentName(p.getDepartment().getName())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .specialisation(p.getSpecialisation())
                .licenseNumber(p.getLicenseNumber())
                .phoneNumber(p.getPhoneNumber())
                .email(p.getEmail())
                .isActive(p.getIsActive())
                .build();
    }
}
