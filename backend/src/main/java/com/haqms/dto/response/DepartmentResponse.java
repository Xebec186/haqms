package com.haqms.dto.response;

import com.haqms.entity.Department;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepartmentResponse {

    private Long    departmentId;
    private String  name;
    private String  description;
    private String  location;
    private Boolean isActive;

    public static DepartmentResponse from(Department d) {
        return DepartmentResponse.builder()
                .departmentId(d.getDepartmentId())
                .name(d.getName())
                .description(d.getDescription())
                .location(d.getLocation())
                .isActive(d.getIsActive())
                .build();
    }
}
