package com.haqms.service;

import com.haqms.dto.request.CreateSystemUserRequest;
import com.haqms.dto.response.AnalyticsSummaryResponse;
import com.haqms.dto.response.DepartmentAnalyticsResponse;
import com.haqms.dto.response.SystemUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AdminService {

    SystemUserResponse createUser(CreateSystemUserRequest request);

    Page<SystemUserResponse> listUsers(String roleName, Pageable pageable);

    SystemUserResponse getUserById(Long userId);

    SystemUserResponse updateUserStatus(Long userId, Boolean isActive);

    AnalyticsSummaryResponse getDailySummary(LocalDate date);

    List<DepartmentAnalyticsResponse> getDepartmentAnalytics(LocalDate from, LocalDate to);

    AnalyticsSummaryResponse getAnalyticsSummary(LocalDate date);
}
