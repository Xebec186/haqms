package com.haqms.controller;

import com.haqms.dto.response.ApiResponse;
import com.haqms.dto.response.QueueEntryResponse;
import com.haqms.entity.SystemUser;
import com.haqms.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/queue-entry")
@RequiredArgsConstructor
public class QueueEntryController {
    private final QueueService queueService;

    /**
     * GET /api/v1/queue-entry/{appointmentId}
     * Returns queue-level summary: status, current position, total registered.
     * Polled by the patient queue-status screen every 30–60 seconds.
     */
    @GetMapping("/{appointmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> getQueue(
            @PathVariable Long appointmentId, @AuthenticationPrincipal SystemUser currentUser) {

        QueueEntryResponse response = queueService.getQueueEntryByAppointmentId(appointmentId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Queue retrieved"));
    }
}
