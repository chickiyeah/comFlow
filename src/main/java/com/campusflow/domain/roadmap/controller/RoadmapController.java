package com.campusflow.domain.roadmap.controller;

import com.campusflow.domain.roadmap.dto.RoadmapRequest;
import com.campusflow.domain.roadmap.dto.RoadmapResponse;
import com.campusflow.domain.roadmap.service.RoadmapService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    @PostMapping("/generate")
    public ApiResponse<RoadmapResponse> generateRoadmap(@Valid @RequestBody RoadmapRequest request) {
        return ApiResponse.ok(roadmapService.generateRoadmap(request));
    }
}
