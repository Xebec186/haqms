package com.haqms.service.impl;

import com.haqms.dto.request.CreateDepartmentRequest;
import com.haqms.dto.request.UpdateDepartmentRequest;
import com.haqms.dto.response.DepartmentResponse;
import com.haqms.dto.response.ProviderResponse;
import com.haqms.entity.Department;
import com.haqms.exception.ConflictException;
import com.haqms.exception.ResourceNotFoundException;
import com.haqms.repository.DepartmentRepository;
import com.haqms.repository.HealthcareProviderRepository;
import com.haqms.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository          departmentRepository;
    private final HealthcareProviderRepository  providerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAllActive() {
        return departmentRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(DepartmentResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse findById(Long departmentId) {
        return DepartmentResponse.from(requireDepartment(departmentId));
    }

    @Override
    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        if (departmentRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException(
                    "A department named '" + request.getName() + "' already exists.");
        }
        Department dept = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .location(request.getLocation())
                .isActive(true)
                .build();
        log.info("Created department '{}'", request.getName());
        return DepartmentResponse.from(departmentRepository.save(dept));
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long departmentId, UpdateDepartmentRequest request) {
        Department dept = requireDepartment(departmentId);

        if (request.getName() != null && !request.getName().equals(dept.getName())) {
            if (departmentRepository.existsByNameIgnoreCase(request.getName())) {
                throw new ConflictException(
                        "A department named '" + request.getName() + "' already exists.");
            }
            dept.setName(request.getName());
        }
        if (request.getDescription() != null) {
            dept.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            dept.setLocation(request.getLocation());
        }
        if (request.getIsActive() != null) {
            dept.setIsActive(request.getIsActive());
        }

        return DepartmentResponse.from(departmentRepository.save(dept));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderResponse> findActiveProvidersByDepartment(Long departmentId) {
        requireDepartment(departmentId);
        return providerRepository
                .findByDepartmentDepartmentIdAndIsActiveTrueOrderByLastNameAsc(departmentId)
                .stream()
                .map(ProviderResponse::from)
                .collect(Collectors.toList());
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private Department requireDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }
}
