package com.haqms.dto.request;

import com.haqms.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private AppointmentStatus status;

    @Size(max = 300, message = "Cancellation reason must not exceed 300 characters")
    private String cancellationReason;
}
