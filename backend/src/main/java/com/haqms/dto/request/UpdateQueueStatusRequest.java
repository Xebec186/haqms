package com.haqms.dto.request;

import com.haqms.enums.QueueStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateQueueStatusRequest {

    @NotNull(message = "Queue status is required")
    private QueueStatus status;
}
