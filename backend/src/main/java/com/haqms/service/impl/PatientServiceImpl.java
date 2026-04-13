package com.haqms.service.impl;

import com.haqms.dto.request.CreatePatientRequest;
import com.haqms.dto.request.UpdatePatientRequest;
import com.haqms.dto.response.PatientResponse;
import com.haqms.entity.Patient;
import com.haqms.enums.Gender;
import com.haqms.exception.ConflictException;
import com.haqms.exception.ResourceNotFoundException;
import com.haqms.repository.PatientRepository;
import com.haqms.repository.SystemUserRepository;
import com.haqms.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientServiceImpl implements PatientService {

    private final PatientRepository    patientRepository;
    private final SystemUserRepository userRepository;

    /**
     * Staff-side patient registration (no system user account created).
     * Self-registration with account creation goes through AuthService.register().
     */
    @Override
    @Transactional
    public PatientResponse register(CreatePatientRequest request) {

        if (patientRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ConflictException(
                    "A patient with phone number " + request.getPhoneNumber() + " already exists.");
        }
        if (request.getGhanaCardNumber() != null &&
                patientRepository.existsByGhanaCardNumber(request.getGhanaCardNumber())) {
            throw new ConflictException(
                    "A patient with Ghana Card number " + request.getGhanaCardNumber() + " already exists.");
        }

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

        return PatientResponse.from(patientRepository.save(patient));
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse findById(Long patientId) {
        return PatientResponse.from(requirePatient(patientId));
    }

    /**
     * Resolves the patient linked to a system user account.
     * Used by GET /patients/me so patients can fetch their own profile via JWT.
     */
    @Override
    @Transactional(readOnly = true)
    public PatientResponse findByUserId(Long userId) {
        Patient patient = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId))
                .getPatient();
        if (patient == null) {
            throw new ResourceNotFoundException("No patient profile linked to user: " + userId);
        }
        return PatientResponse.from(patient);
    }

    /**
     * Partial update — only contact fields are mutable after registration.
     * Clinical data (DOB, gender, Ghana Card) is immutable.
     */
    @Override
    @Transactional
    public PatientResponse update(Long patientId, UpdatePatientRequest request) {

        Patient patient = requirePatient(patientId);

        if (request.getPhoneNumber() != null &&
                !request.getPhoneNumber().equals(patient.getPhoneNumber())) {
            if (patientRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new ConflictException(
                        "Phone number " + request.getPhoneNumber() + " is already in use.");
            }
            patient.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null) {
            patient.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            patient.setAddress(request.getAddress());
        }

        return PatientResponse.from(patientRepository.save(patient));
    }

    /**
     * Paginated search across last name and first name.
     * If searchTerm is null or blank, returns all active patients.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> search(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return patientRepository.findAll(pageable).map(PatientResponse::from);
        }
        return patientRepository
                .findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(
                        searchTerm, searchTerm, pageable)
                .map(PatientResponse::from);
    }


    // ── private helpers ───────────────────────────────────────────────────────

    private Patient requirePatient(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
    }
}
