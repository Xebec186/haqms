package com.haqms.controller;

import com.haqms.dto.request.CheckInRequest;
import com.haqms.dto.request.UpdateQueueStatusRequest;
import com.haqms.dto.response.ApiResponse;
import com.haqms.dto.response.QueueEntryResponse;
import com.haqms.dto.response.QueueResponse;
import com.haqms.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    /**
     * POST /api/v1/queue/check-in
     * Assigns a queue position to a patient for a SCHEDULED or CONFIRMED
     * appointment. Creates the daily department queue if it does not exist.
     * Returns the entry with estimated wait time.
     */
    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','ADMIN')")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> checkIn(
            @Valid @RequestBody CheckInRequest request) {

        QueueEntryResponse response = queueService.checkIn(request.getAppointmentId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Checked in successfully"));
    }

    /**
     * GET /api/v1/queue/{queueId}/entries
     * Returns all queue entries sorted by priority then queue_position.
     * Used by the provider queue management dashboard.
     */
    @GetMapping("/{queueId}/entries")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<ApiResponse<List<QueueEntryResponse>>> getQueueEntries(
            @PathVariable Long queueId) {

        List<QueueEntryResponse> entries = queueService.getEntriesByQueue(queueId);
        return ResponseEntity.ok(ApiResponse.success(entries, "Queue entries retrieved"));
    }

    /**
     * GET /api/v1/queue/{queueId}
     * Returns queue metadata for a specific queue id.
     */
    @GetMapping("/{queueId}")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<ApiResponse<QueueResponse>> getQueueById(
            @PathVariable Long queueId) {

        QueueResponse response = queueService.getQueueById(queueId);
        return ResponseEntity.ok(ApiResponse.success(response, "Queue retrieved"));
    }

    /**
     * POST /api/v1/queue/{queueId}/call-next
     * Selects the next WAITING entry using priority ordering:
     * EMERGENCY first, then URGENT, then REGULAR; FIFO within each tier.
     * Transitions the selected entry from WAITING → CALLED.
     */
    @PostMapping("/{queueId}/call-next")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> callNext(
            @PathVariable Long queueId) {

        QueueEntryResponse response = queueService.callNext(queueId);
        return ResponseEntity.ok(ApiResponse.success(response, "Patient called"));
    }

    /**
     * PATCH /api/v1/queue/entries/{id}/serving
     * Transitions a CALLED entry to SERVING (consultation started).
     * Records serving_started_at timestamp.
     */
    @PatchMapping("/entries/{id}/serving")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> startServing(
            @PathVariable Long id) {

        QueueEntryResponse response = queueService.startServing(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Serving started"));
    }

    /**
     * PATCH /api/v1/queue/entries/{id}/complete
     * Transitions a SERVING entry to COMPLETED.
     * Records completed_at, computes wait_minutes, and marks the linked
     * appointment as COMPLETED.
     */
    @PatchMapping("/entries/{id}/complete")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> completeEntry(
            @PathVariable Long id) {

        QueueEntryResponse response = queueService.completeEntry(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Consultation completed"));
    }

    /**
     * PATCH /api/v1/queue/entries/{id}/missed
     * Transitions a CALLED entry to MISSED (patient did not attend when called).
     * Marks the linked appointment as NO_SHOW.
     */
    @PatchMapping("/entries/{id}/missed")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> markMissed(
            @PathVariable Long id) {

        QueueEntryResponse response = queueService.markMissed(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Entry marked as missed"));
    }

    /**
     * PATCH /api/v1/queue/{queueId}/status
     * Allows ADMIN to open, pause, or close a queue for a department.
     * Closing a queue records closed_at. Pausing suspends callNext.
     */
    @PatchMapping("/{queueId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<QueueResponse>> updateQueueStatus(
            @PathVariable Long queueId,
            @Valid @RequestBody UpdateQueueStatusRequest request) {

        QueueResponse response = queueService.updateQueueStatus(queueId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response, "Queue status updated"));
    }

    /**
     * GET /api/v1/queue/provider/{providerId}/today
     * Returns today's queue for a provider, or 404 if none exists yet
     * (no patients have checked in yet today).
     */
    @GetMapping("/provider/{providerId}/today")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<QueueResponse>> getTodaysQueue(
            @PathVariable Long departmentId) {

        QueueResponse response = queueService.getTodaysQueueByProvider(departmentId);
        return ResponseEntity.ok(ApiResponse.success(response, "Queue retrieved"));
    }
}

