// CreateSystemUserRequest.java
package com.haqms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSystemUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Role is required")
    private String roleName; // ADMIN, RECEPTIONIST, PROVIDER, PATIENT

    // Optional — link to an existing provider record if role = PROVIDER
    private Long providerId;

    // Optional — link to an existing patient record if role = PATIENT
    private Long patientId;
}