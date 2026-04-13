package com.haqms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckInRequest {

    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;
}
