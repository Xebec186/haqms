package com.haqms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreatePatientRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+233[0-9]{9}$",
            message = "Phone number must be a valid Ghanaian number (+233XXXXXXXXX)")
    private String phoneNumber;

    @Email(message = "Enter a valid email address")
    private String email;

    @Pattern(
            regexp = "^GHA-[0-9]{9}-[0-9]$",
            message = "Ghana Card number must be in format GHA-XXXXXXXXX-X"
    )
    private String ghanaCardNumber;

    private String address;
}
