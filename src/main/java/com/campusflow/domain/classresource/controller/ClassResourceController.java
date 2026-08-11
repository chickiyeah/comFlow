package com.campusflow.domain.classresource.controller;

import com.campusflow.domain.classresource.dto.ResourceResponse;
import com.campusflow.domain.classresource.entity.ResourceType;
import com.campusflow.domain.classresource.service.ClassResourceService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClassResourceController {

    private final ClassResourceService resourceService;

    @GetMapping("/api/classes/{classId}/resources")
    public ApiResponse<List<ResourceResponse>> list(@AuthenticationPrincipal String username,
                                                    @PathVariable Long classId) {
        return ApiResponse.ok(resourceService.list(username, classId));
    }

    @PostMapping("/api/classes/{classId}/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResourceResponse> create(@AuthenticationPrincipal String username,
                                                @PathVariable Long classId,
                                                @RequestParam String title,
                                                @RequestParam(required = false) ResourceType type,
                                                @RequestParam(required = false) String url,
                                                @RequestParam(required = false) MultipartFile file) {
        return ApiResponse.ok(resourceService.create(username, classId, title, type, url, file));
    }

    @DeleteMapping("/api/resources/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long resourceId) {
        resourceService.delete(username, resourceId);
    }
}
