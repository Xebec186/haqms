package com.haqms.dto.response;

import com.haqms.entity.Patient;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PatientResponse {

    private Long          patientId;
    private String        firstName;
    private String        lastName;
    private LocalDate     dateOfBirth;
    private String        gender;
    private String        phoneNumber;
    private String        email;
    private String        address;
    private Boolean       isActive;
    private LocalDateTime createdAt;

    // ghanaCardNumber omitted from responses — data minimisation (Ghana DPA 2012)

    public static PatientResponse from(Patient p) {
        return PatientResponse.builder()
                .patientId(p.getPatientId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender() != null ? p.getGender().name() : null)
                .phoneNumber(p.getPhoneNumber())
                .email(p.getEmail())
                .address(p.getAddress())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
