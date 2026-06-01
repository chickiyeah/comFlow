package com.campusflow.domain.facilities.controller;

import com.campusflow.domain.facilities.entity.FacilityStat;
import com.campusflow.domain.facilities.service.FacilityService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    @GetMapping("/stats")
    public ApiResponse<List<FacilityStat>> getStats() {
        return ApiResponse.ok(facilityService.getAll());
    }

    @PutMapping("/stats/{key}")
    public ApiResponse<FacilityStat> updateStat(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        return ApiResponse.ok(facilityService.update(key, body.get("value")));
    }
}
