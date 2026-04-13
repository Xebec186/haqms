package com.haqms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AnalyticsSummaryResponse {

    private LocalDate date;
    private long totalAppointments;
    private long scheduled;
    private long confirmed;
    private long completed;
    private long cancelled;
    private long noShow;
    private long activeQueues;
    private long patientsWaiting;
    private long emergencyWaiting;
    private long urgentWaiting;
}

