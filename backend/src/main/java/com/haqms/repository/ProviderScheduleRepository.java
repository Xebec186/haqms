package com.haqms.repository;

import com.haqms.entity.ProviderSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ProviderScheduleRepository extends JpaRepository<ProviderSchedule, Long> {

    List<ProviderSchedule>
    findByProviderProviderIdAndScheduleDateGreaterThanEqualAndIsAvailableTrueOrderByScheduleDateAscStartTimeAsc(
            Long providerId, LocalDate fromDate);

    boolean existsByProviderProviderIdAndScheduleDateAndStartTime(
            Long providerId, LocalDate scheduleDate, LocalTime startTime);
}
