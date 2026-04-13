package com.haqms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "isActive flag is required")
    private Boolean isActive;
}
