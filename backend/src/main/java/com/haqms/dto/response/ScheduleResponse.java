package com.haqms.dto.response;

import com.haqms.entity.ProviderSchedule;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class ScheduleResponse {

    private Long      scheduleId;
    private Long      providerId;
    private String    providerFirstName;
    private String    providerLastName;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer   maxSlots;
    private Boolean   isAvailable;

    public static ScheduleResponse from(ProviderSchedule s) {
        return ScheduleResponse.builder()
                .scheduleId(s.getScheduleId())
                .providerId(s.getProvider().getProviderId())
                .providerFirstName(s.getProvider().getFirstName())
                .providerLastName(s.getProvider().getLastName())
                .scheduleDate(s.getScheduleDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .maxSlots(s.getMaxSlots())
                .isAvailable(s.getIsAvailable())
                .build();
    }
}

