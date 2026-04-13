package com.haqms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "provider_schedules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider_id","schedule_date","start_time"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private HealthcareProvider provider;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "max_slots", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer maxSlots = 20;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;
}
