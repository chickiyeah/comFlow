package com.campusflow.domain.award.controller;

import com.campusflow.domain.award.dto.AwardRequest;
import com.campusflow.domain.award.dto.AwardResponse;
import com.campusflow.domain.award.service.AwardService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/awards")
@RequiredArgsConstructor
public class AwardController {

    private final AwardService awardService;

    @GetMapping("/me")
    public ApiResponse<List<AwardResponse>> getMyAwards(@AuthenticationPrincipal UserDetails user) {
        return ApiResponse.ok(awardService.getMyAwards(user.getUsername()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AwardResponse> create(@AuthenticationPrincipal UserDetails user,
                                              @Valid @RequestBody AwardRequest request) {
        return ApiResponse.ok(awardService.create(user.getUsername(), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        awardService.delete(user.getUsername(), id);
    }
}
