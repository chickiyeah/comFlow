package com.campusflow.domain.material.controller;

import com.campusflow.domain.material.dto.*;
import com.campusflow.domain.material.service.MaterialAiService;
import com.campusflow.domain.material.service.MaterialService;
import com.campusflow.domain.storage.dto.FileTicketResponse;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 수업 자료. 목록/업로드는 클래스 스코프(/api/classes/{classId}/materials),
 * 개별 자료 작업은 /api/materials/{materialId} 아래에 둔다.
 */
@RestController
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final MaterialAiService materialAiService;

    @GetMapping("/api/classes/{classId}/materials")
    public ApiResponse<List<MaterialResponse>> list(@AuthenticationPrincipal String username,
                                                    @PathVariable Long classId) {
        return ApiResponse.ok(materialService.list(username, classId));
    }

    @PostMapping("/api/classes/{classId}/materials")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MaterialResponse> upload(@AuthenticationPrincipal String username,
                                                @PathVariable Long classId,
                                                @RequestParam String title,
                                                @RequestParam(required = false) String instructions,
                                                @RequestParam(required = false) Integer week,
                                                @RequestParam(required = false) String topic,
                                                @RequestParam(required = false) MultipartFile file) {
        return ApiResponse.ok(materialService.upload(username, classId, title, instructions, week, topic, file));
    }

    @GetMapping("/api/materials/{materialId}")
    public ApiResponse<MaterialDetailResponse> detail(@AuthenticationPrincipal String username,
                                                      @PathVariable Long materialId) {
        return ApiResponse.ok(materialService.getDetail(username, materialId));
    }

    @PutMapping("/api/materials/{materialId}")
    public ApiResponse<MaterialResponse> update(@AuthenticationPrincipal String username,
                                                @PathVariable Long materialId,
                                                @Valid @RequestBody MaterialUpdateRequest request) {
        return ApiResponse.ok(materialService.update(username, materialId, request));
    }

    @PatchMapping("/api/materials/{materialId}/topic")
    public ApiResponse<MaterialResponse> updateTopic(@AuthenticationPrincipal String username,
                                                     @PathVariable Long materialId,
                                                     @Valid @RequestBody TopicRequest request) {
        return ApiResponse.ok(materialService.updateTopic(username, materialId, request.topic()));
    }

    @DeleteMapping("/api/materials/{materialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long materialId) {
        materialService.delete(username, materialId);
    }

    @PostMapping("/api/materials/{materialId}/ticket")
    public ApiResponse<FileTicketResponse> streamTicket(@AuthenticationPrincipal String username,
                                                        @PathVariable Long materialId) {
        return ApiResponse.ok(materialService.streamTicket(username, materialId));
    }

    @PostMapping("/api/materials/{materialId}/summary")
    public ApiResponse<MaterialSummaryResponse> summary(@AuthenticationPrincipal String username,
                                                        @PathVariable Long materialId) {
        return ApiResponse.ok(materialAiService.summary(username, materialId));
    }

    @PostMapping("/api/materials/{materialId}/ai")
    public ApiResponse<MaterialAiResponse> ai(@AuthenticationPrincipal String username,
                                              @PathVariable Long materialId,
                                              @RequestBody MaterialAiRequest request) {
        return ApiResponse.ok(materialAiService.action(username, materialId, request));
    }
}
