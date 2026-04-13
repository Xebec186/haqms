package com.haqms.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateProviderRequest {

    private String  specialisation;

    @Email(message = "Enter a valid email address")
    private String  email;

    private String  phoneNumber;
    private Long    departmentId;
    private Boolean isActive;
}
