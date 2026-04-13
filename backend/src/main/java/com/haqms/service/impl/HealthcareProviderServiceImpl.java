package com.haqms.service.impl;

import com.haqms.dto.request.CreateProviderRequest;
import com.haqms.dto.request.UpdateProviderRequest;
import com.haqms.dto.response.ProviderResponse;
import com.haqms.entity.Department;
import com.haqms.entity.HealthcareProvider;
import com.haqms.entity.Role;
import com.haqms.entity.SystemUser;
import com.haqms.exception.ConflictException;
import com.haqms.exception.ResourceNotFoundException;
import com.haqms.repository.DepartmentRepository;
import com.haqms.repository.HealthcareProviderRepository;
import com.haqms.repository.RoleRepository;
import com.haqms.repository.SystemUserRepository;
import com.haqms.service.HealthcareProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthcareProviderServiceImpl implements HealthcareProviderService {

    private final HealthcareProviderRepository providerRepository;
    private final DepartmentRepository         departmentRepository;
    private final SystemUserRepository         userRepository;
    private final RoleRepository               roleRepository;
    private final PasswordEncoder              passwordEncoder;

    /**
     * Returns all providers, optionally filtered by department.
     * Ordered by last name ascending.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProviderResponse> findAll(Long departmentId) {
        List<HealthcareProvider> providers;
        if (departmentId != null) {
            providers = providerRepository
                    .findByDepartmentDepartmentIdAndIsActiveTrueOrderByLastNameAsc(departmentId);
        } else {
            providers = providerRepository.findAllByOrderByLastNameAsc();
        }
        return providers.stream()
                .map(ProviderResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderResponse findById(Long providerId) {
        return ProviderResponse.from(requireProvider(providerId));
    }

    /**
     * Creates a new healthcare provider.
     * If username and password are supplied in the request, also creates a linked
     * PROVIDER-role system user account in the same transaction.
     */
    @Override
    @Transactional
    public ProviderResponse create(CreateProviderRequest request) {

        if (providerRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new ConflictException(
                    "A provider with license number '" +
                    request.getLicenseNumber() + "' already exists.");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found: " + request.getDepartmentId()));

        HealthcareProvider provider = HealthcareProvider.builder()
                .department(department)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .specialisation(request.getSpecialisation())
                .licenseNumber(request.getLicenseNumber())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .isActive(true)
                .build();
        provider = providerRepository.save(provider);

        // Optionally create a system user account for this provider
        if (request.getUsername() != null && request.getPassword() != null) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ConflictException(
                        "Username '" + request.getUsername() + "' is already taken.");
            }
            Role providerRole = roleRepository.findByRoleName("PROVIDER")
                    .orElseThrow(() -> new ResourceNotFoundException("Role PROVIDER not found"));

            SystemUser user = SystemUser.builder()
                    .role(providerRole)
                    .username(request.getUsername())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .email(request.getEmail())
                    .provider(provider)
                    .isActive(true)
                    .build();
            userRepository.save(user);
            log.info("System user account created for provider {}", provider.getProviderId());
        }

        log.info("Created provider: Dr. {} {} (id={}, license={})",
                provider.getFirstName(), provider.getLastName(),
                provider.getProviderId(), provider.getLicenseNumber());

        return ProviderResponse.from(provider);
    }

    /**
     * Updates mutable provider fields.
     * License number is immutable after creation.
     */
    @Override
    @Transactional
    public ProviderResponse update(Long providerId, UpdateProviderRequest request) {
        HealthcareProvider provider = requireProvider(providerId);

        if (request.getSpecialisation() != null) {
            provider.setSpecialisation(request.getSpecialisation());
        }
        if (request.getEmail() != null) {
            provider.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            provider.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getIsActive() != null) {
            provider.setIsActive(request.getIsActive());
        }
        if (request.getDepartmentId() != null) {
            Department newDept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found: " + request.getDepartmentId()));
            provider.setDepartment(newDept);
        }

        return ProviderResponse.from(providerRepository.save(provider));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private HealthcareProvider requireProvider(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + id));
    }
}
