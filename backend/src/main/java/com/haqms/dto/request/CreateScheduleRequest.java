package com.haqms.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateScheduleRequest {

    /** Set by the controller from the path variable — not from client body. */
    private Long providerId;

    @NotNull(message = "Schedule date is required")
    @FutureOrPresent(message = "Schedule date must not be in the past")
    private LocalDate scheduleDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @Min(value = 1, message = "Max slots must be at least 1")
    @Max(value = 100, message = "Max slots must not exceed 100")
    private Integer maxSlots = 20;
}
