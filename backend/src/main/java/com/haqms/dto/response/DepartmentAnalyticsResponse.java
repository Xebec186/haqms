package com.haqms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentAnalyticsResponse {

    private Long   departmentId;
    private String departmentName;
    private long   totalAppointments;
    private long   completed;
    private long   cancelled;
    private double avgWaitMinutes;
}
