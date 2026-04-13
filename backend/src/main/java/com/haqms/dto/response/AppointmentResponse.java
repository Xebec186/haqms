package com.haqms.dto.response;

import com.haqms.entity.Appointment;
import com.haqms.enums.AppointmentPriority;
import com.haqms.enums.AppointmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class AppointmentResponse {

    private Long                appointmentId;
    private Long                patientId;
    private String              patientFirstName;
    private String              patientLastName;
    private Long                providerId;
    private String              providerFirstName;
    private String              providerLastName;
    private Long                departmentId;
    private String              departmentName;
    private Long                scheduleId;
    private LocalDate           appointmentDate;
    private LocalTime           appointmentStartTime;
    private LocalTime           appointmentEndTime;
    private String              reason;
    private AppointmentPriority appointmentPriority;
    private AppointmentStatus   status;
    private String              cancellationReason;
    private LocalDateTime       createdAt;
    private LocalDateTime       updatedAt;

    public static AppointmentResponse from(Appointment a) {
        return AppointmentResponse.builder()
                .appointmentId(a.getAppointmentId())
                .patientId(a.getPatient().getPatientId())
                .patientFirstName(a.getPatient().getFirstName())
                .patientLastName(a.getPatient().getLastName())
                .providerId(a.getProvider().getProviderId())
                .providerFirstName(a.getProvider().getFirstName())
                .providerLastName(a.getProvider().getLastName())
                .departmentId(a.getDepartment().getDepartmentId())
                .departmentName(a.getDepartment().getName())
                .scheduleId(a.getSchedule().getScheduleId())
                .appointmentDate(a.getAppointmentDate())
                .appointmentStartTime(a.getSchedule().getStartTime())
                .appointmentEndTime(a.getSchedule().getEndTime())
                .reason(a.getReason())
                .appointmentPriority(a.getAppointmentPriority())
                .status(a.getStatus())
                .cancellationReason(a.getCancellationReason())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
