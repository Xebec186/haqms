package com.haqms.service;

import com.haqms.dto.request.CreateDepartmentRequest;
import com.haqms.dto.request.UpdateDepartmentRequest;
import com.haqms.dto.response.DepartmentResponse;
import com.haqms.dto.response.ProviderResponse;

import java.util.List;

public interface DepartmentService {

    List<DepartmentResponse> findAllActive();

    DepartmentResponse findById(Long departmentId);

    DepartmentResponse create(CreateDepartmentRequest request);

    DepartmentResponse update(Long departmentId, UpdateDepartmentRequest request);

    List<ProviderResponse> findActiveProvidersByDepartment(Long departmentId);
}
