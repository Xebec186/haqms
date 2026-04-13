package com.haqms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProviderRequest {

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String specialisation;

    @NotBlank(message = "License number is required")
    @Size(max = 50, message = "License number must not exceed 50 characters")
    private String licenseNumber;

    private String phoneNumber;
    private String email;

    // Optional: if supplied, a PROVIDER-role system user account is created
    private String username;
    private String password;
}
