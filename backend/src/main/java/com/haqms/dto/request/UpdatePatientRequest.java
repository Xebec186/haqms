package com.haqms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Only mutable contact fields. Clinical data (DOB, gender) is immutable. */
@Data
public class UpdatePatientRequest {

    @Pattern(regexp = "^\\+233[0-9]{9}$",
            message = "Phone number must be a valid Ghanaian number (+233XXXXXXXXX)")
    private String phoneNumber;

    @Email(message = "Enter a valid email address")
    private String email;

    private String address;
}
