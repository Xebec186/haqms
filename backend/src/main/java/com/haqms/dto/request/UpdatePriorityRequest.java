package com.haqms.dto.request;

import com.haqms.enums.AppointmentPriority;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePriorityRequest {

    @NotNull(message = "Priority is required")
    private AppointmentPriority priority;
}
