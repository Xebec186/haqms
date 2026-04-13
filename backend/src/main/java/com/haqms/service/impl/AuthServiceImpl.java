package com.haqms.service.impl;

import com.haqms.dto.request.ChangePasswordRequest;
import com.haqms.dto.request.LoginRequest;
import com.haqms.dto.request.RegisterRequest;
import com.haqms.dto.response.AuthResponse;
import com.haqms.dto.response.PatientResponse;
import com.haqms.entity.Patient;
import com.haqms.entity.Role;
import com.haqms.entity.SystemUser;
import com.haqms.enums.Gender;
import com.haqms.exception.ConflictException;
import com.haqms.exception.ResourceNotFoundException;
import com.haqms.exception.ValidationException;
import com.haqms.repository.PatientRepository;
import com.haqms.repository.RoleRepository;
import com.haqms.repository.SystemUserRepository;
import com.haqms.security.JwtUtil;
import com.haqms.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager  authenticationManager;
    private final SystemUserRepository   userRepository;
    private final PatientRepository      patientRepository;
    private final RoleRepository         roleRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtUtil                jwtUtil;

    /**
     * Delegates credential validation to Spring Security's AuthenticationManager
     * (backed by UserDetailsServiceImpl + BCrypt).
     * On success, generates a JWT and records the last-login timestamp.
     */
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));

        SystemUser user = (SystemUser) auth.getPrincipal();
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user);

        log.info("User '{}' logged in with role '{}'", user.getUsername(),
                user.getRole().getRoleName());

        return new AuthResponse(
                token,
                user.getRole().getRoleName(),
                user.getUserId(),
                user.getPatient()  != null ? user.getPatient().getPatientId()   : null,
                user.getProvider() != null ? user.getProvider().getProviderId() : null
        );
    }

    /**
     * Creates a Patient record and a linked PATIENT-role SystemUser atomically.
     * Validates uniqueness of phone number, Ghana Card, and username before persisting.
     */
    @Override
    @Transactional
    public PatientResponse register(RegisterRequest request) {

        if (patientRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ConflictException(
                    "A patient with phone number " + request.getPhoneNumber() + " already exists.");
        }
        if (request.getGhanaCardNumber() != null &&
                patientRepository.existsByGhanaCardNumber(request.getGhanaCardNumber())) {
            throw new ConflictException(
                    "A patient with Ghana Card number " + request.getGhanaCardNumber() + " already exists.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException(
                    "Username '" + request.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(
                    "Email '" + request.getEmail() + "' is already registered.");
        }

        // Persist patient
        Patient patient = Patient.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(Gender.valueOf(request.getGender().toUpperCase()))
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .ghanaCardNumber(request.getGhanaCardNumber())
                .address(request.getAddress())
                .isActive(true)
                .build();
        patient = patientRepository.save(patient);

        // Fetch PATIENT role (seeded at startup)
        Role patientRole = roleRepository.findByRoleName("PATIENT")
                .orElseThrow(() -> new ResourceNotFoundException("Role PATIENT not found"));

        // Persist linked system user
        SystemUser user = SystemUser.builder()
                .role(patientRole)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .patient(patient)
                .isActive(true)
                .build();
        userRepository.save(user);

        log.info("Registered new patient: {} {} (id={})",
                patient.getFirstName(), patient.getLastName(), patient.getPatientId());

        return PatientResponse.from(patient);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, SystemUser currentUser) {
        SystemUser user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ValidationException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
