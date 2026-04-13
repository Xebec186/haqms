package com.haqms.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDepartmentRequest {

    @Size(max = 150)
    private String name;

    @Size(max = 300)
    private String description;

    @Size(max = 150)
    private String location;

    private Boolean isActive;
}
