package com.haqms.service;

import com.haqms.dto.request.CreatePatientRequest;
import com.haqms.dto.request.UpdatePatientRequest;
import com.haqms.dto.response.PatientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PatientService {

    PatientResponse register(CreatePatientRequest request);

    PatientResponse findById(Long patientId);

    PatientResponse findByUserId(Long userId);

    PatientResponse update(Long patientId, UpdatePatientRequest request);

    Page<PatientResponse> search(String searchTerm, Pageable pageable);
}
