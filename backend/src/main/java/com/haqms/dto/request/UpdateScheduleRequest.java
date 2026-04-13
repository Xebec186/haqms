package com.haqms.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateScheduleRequest {

    @Min(1) @Max(100)
    private Integer maxSlots;

    private Boolean isAvailable;
}
