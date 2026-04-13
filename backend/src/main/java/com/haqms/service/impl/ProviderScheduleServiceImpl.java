package com.haqms.service.impl;

import com.haqms.dto.request.CreateScheduleRequest;
import com.haqms.dto.request.UpdateScheduleRequest;
import com.haqms.dto.response.ScheduleResponse;
import com.haqms.entity.HealthcareProvider;
import com.haqms.entity.ProviderSchedule;
import com.haqms.exception.ConflictException;
import com.haqms.exception.ResourceNotFoundException;
import com.haqms.exception.ValidationException;
import com.haqms.repository.HealthcareProviderRepository;
import com.haqms.repository.ProviderScheduleRepository;
import com.haqms.service.ProviderScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderScheduleServiceImpl implements ProviderScheduleService {

    private final ProviderScheduleRepository  scheduleRepository;
    private final HealthcareProviderRepository providerRepository;

    /**
     * Returns all available (is_available=true) schedules from today onward
     * for the given provider, sorted by date then start time.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findAvailableByProvider(Long providerId) {
        requireProvider(providerId);
        return scheduleRepository
                .findByProviderProviderIdAndScheduleDateGreaterThanEqualAndIsAvailableTrueOrderByScheduleDateAscStartTimeAsc(
                        providerId, LocalDate.now())
                .stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new schedule for a provider.
     * Validates:
     * - end_time > start_time
     * - No duplicate (provider, date, start_time) — mirrors DB unique constraint.
     */
    @Override
    @Transactional
    public ScheduleResponse create(CreateScheduleRequest request) {
        HealthcareProvider provider = requireProvider(request.getProviderId());

        if (request.getEndTime().isBefore(request.getStartTime()) ||
                request.getEndTime().equals(request.getStartTime())) {
            throw new ValidationException("End time must be after start time.");
        }

        if (scheduleRepository.existsByProviderProviderIdAndScheduleDateAndStartTime(
                request.getProviderId(),
                request.getScheduleDate(),
                request.getStartTime())) {
            throw new ConflictException(
                    "Provider " + request.getProviderId() +
                    " already has a schedule on " + request.getScheduleDate() +
                    " starting at " + request.getStartTime());
        }

        ProviderSchedule schedule = ProviderSchedule.builder()
                .provider(provider)
                .scheduleDate(request.getScheduleDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxSlots(request.getMaxSlots() != null ? request.getMaxSlots() : 20)
                .isAvailable(true)
                .build();

        log.info("Created schedule for provider {} on {} ({}-{})",
                provider.getProviderId(), request.getScheduleDate(),
                request.getStartTime(), request.getEndTime());

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    /**
     * Partially updates a schedule — only max_slots and is_available are mutable.
     * Date and time windows are immutable after creation.
     */
    @Override
    @Transactional
    public ScheduleResponse update(Long scheduleId, UpdateScheduleRequest request) {
        ProviderSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Schedule not found: " + scheduleId));

        if (request.getMaxSlots() != null) {
            schedule.setMaxSlots(request.getMaxSlots());
        }
        if (request.getIsAvailable() != null) {
            schedule.setIsAvailable(request.getIsAvailable());
        }

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private HealthcareProvider requireProvider(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + id));
    }
}
