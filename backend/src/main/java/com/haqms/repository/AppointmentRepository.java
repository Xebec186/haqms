package com.haqms.repository;

import com.haqms.entity.Appointment;
import com.haqms.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // ── Capacity check ────────────────────────────────────────────────────────

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.schedule.scheduleId = :scheduleId " +
           "AND a.status NOT IN ('CANCELLED', 'NO_SHOW')")
    long countActiveByScheduleId(@Param("scheduleId") Long scheduleId);

    // ── Patient queries ───────────────────────────────────────────────────────

    List<Appointment> findByPatientPatientIdOrderByAppointmentDate(
            Long patientId);

    // ── Provider / date queries ───────────────────────────────────────────────

    List<Appointment> findByProviderProviderIdAndAppointmentDate(
            Long providerId, LocalDate appointmentDate);

    List<Appointment> findByAppointmentDate(LocalDate appointmentDate);

    // ── Analytics counts ──────────────────────────────────────────────────────

    long countByAppointmentDate(LocalDate appointmentDate);

    long countByAppointmentDateAndStatus(LocalDate appointmentDate, AppointmentStatus status);

    long countByDepartmentDepartmentIdAndAppointmentDateBetween(
            Long departmentId, LocalDate from, LocalDate to);

    long countByDepartmentDepartmentIdAndAppointmentDateBetweenAndStatus(
            Long departmentId, LocalDate from, LocalDate to, AppointmentStatus status);
}
