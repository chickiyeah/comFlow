package com.campusflow.domain.multimodal.controller;

import com.campusflow.domain.multimodal.dto.MultimodalResponse;
import com.campusflow.domain.multimodal.service.MultimodalService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/multimodal")
@RequiredArgsConstructor
public class MultimodalController {

    private final MultimodalService multimodalService;

    @PostMapping("/analyze")
    public ApiResponse<MultimodalResponse> analyze(@AuthenticationPrincipal String username,
                                                   @RequestParam MultipartFile file,
                                                   @RequestParam(required = false) String question) {
        return ApiResponse.ok(multimodalService.analyze(file, question));
    }
}
