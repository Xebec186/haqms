package com.haqms.controller;

import com.haqms.dto.response.ApiResponse;
import com.haqms.dto.response.SystemUserResponse;
import com.haqms.entity.SystemUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class SystemUserController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SystemUserResponse>> getMyProfile(
            @AuthenticationPrincipal SystemUser currentUser) {

        SystemUserResponse response = SystemUserResponse.from(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile retrieved"));
    }
}
