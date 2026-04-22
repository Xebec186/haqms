package com.haqms.service;

import com.haqms.dto.request.CreateProviderRequest;
import com.haqms.dto.request.UpdateProviderRequest;
import com.haqms.dto.response.ProviderResponse;

import java.util.List;

public interface HealthcareProviderService {

    List<ProviderResponse> findAll(Long departmentId);

    ProviderResponse findById(Long providerId);

    ProviderResponse findByUserId(Long userId);

    ProviderResponse create(CreateProviderRequest request);

    ProviderResponse update(Long providerId, UpdateProviderRequest request);
}
