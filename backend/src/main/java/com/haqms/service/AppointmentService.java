package com.haqms.service;

import com.haqms.dto.request.CreateAppointmentRequest;
import com.haqms.dto.response.AppointmentResponse;
import com.haqms.entity.SystemUser;
import com.haqms.enums.AppointmentPriority;
import com.haqms.enums.AppointmentStatus;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    AppointmentResponse createAppointment(CreateAppointmentRequest request);

    AppointmentResponse getById(Long appointmentId, SystemUser currentUser);

    List<AppointmentResponse> getByPatientId(Long patientId);

    List<AppointmentResponse> getByProviderAndDate(Long providerId, LocalDate date);

    AppointmentResponse updateStatus(Long appointmentId, AppointmentStatus newStatus,
                                     String cancellationReason, SystemUser currentUser);

    AppointmentResponse updatePriority(Long appointmentId, AppointmentPriority newPriority);

    void delete(Long appointmentId);
}
