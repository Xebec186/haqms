package com.haqms.service;

import com.haqms.dto.request.CreateScheduleRequest;
import com.haqms.dto.request.UpdateScheduleRequest;
import com.haqms.dto.response.ScheduleResponse;

import java.util.List;

public interface ProviderScheduleService {

    List<ScheduleResponse> findAvailableByProvider(Long providerId);

    ScheduleResponse create(CreateScheduleRequest request);

    ScheduleResponse update(Long scheduleId, UpdateScheduleRequest request);
}
