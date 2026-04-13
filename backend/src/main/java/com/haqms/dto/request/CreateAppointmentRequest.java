package com.haqms.dto.request;

import com.haqms.enums.AppointmentPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAppointmentRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    @NotBlank(message = "Reason for visit is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    /** Defaults to REGULAR. PATIENT role may not set EMERGENCY or URGENT. */
    private AppointmentPriority priority = AppointmentPriority.REGULAR;

    /** Set by the controller from the JWT principal — not from client body. */
    private Long bookedByUserId;
}
