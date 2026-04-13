package com.haqms.controller;

import com.haqms.dto.response.ApiResponse;
import com.haqms.dto.response.ProviderResponse;
import com.haqms.dto.response.QueueEntryResponse;
import com.haqms.dto.response.QueueResponse;
import com.haqms.entity.SystemUser;
import com.haqms.service.HealthcareProviderService;
import com.haqms.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final HealthcareProviderService providerService;
    private final QueueService queueService;

    /**
     * GET /api/v1/providers/me
     * Returns the authenticated provider's own profile.
     * The provider_id is resolved from the JWT via the linked SystemUser.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<ProviderResponse>> getMyProfile(
            @AuthenticationPrincipal SystemUser currentUser) {

        ProviderResponse response = providerService.findById(currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Profile retrieved"));
    }

    @GetMapping("/queue")
    @PreAuthorize(("hasRole('PROVIDER')"))
    public ResponseEntity<ApiResponse<QueueResponse>> getMyQueue(@AuthenticationPrincipal SystemUser currentUser) {
        QueueResponse response = queueService.getTodaysQueueByProvider(currentUser.getProvider().getProviderId());
        return ResponseEntity.ok(ApiResponse.success(response, "Queue retrieved"));
    }

    @GetMapping("/queue-entries")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<List<QueueEntryResponse>>> getMyQueueEntries(
            @AuthenticationPrincipal SystemUser currentUser) {
        List<QueueEntryResponse> response = queueService.getEntriesByProvider(currentUser.getProvider().getProviderId());
        return ResponseEntity.ok(ApiResponse.success(response, "Queue Entries retrieved"));
    }
}
