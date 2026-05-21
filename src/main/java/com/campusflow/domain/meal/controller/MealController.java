package com.campusflow.domain.meal.controller;

import com.campusflow.global.response.ApiResponse;
import com.campusflow.global.service.KomjeongService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/meal")
@RequiredArgsConstructor
public class MealController {

    private final KomjeongService komjeongService;

    @GetMapping("/today")
    public ApiResponse<Map<String, String>> getToday() {
        String meal = komjeongService.getMeal("today");
        return ApiResponse.ok(Map.of("date", "today", "menu", meal.isBlank() ? "학식 정보를 가져올 수 없습니다." : meal));
    }

    @GetMapping
    public ApiResponse<Map<String, String>> getMeal(@RequestParam(defaultValue = "today") String date) {
        String meal = komjeongService.getMeal(date);
        return ApiResponse.ok(Map.of("date", date, "menu", meal.isBlank() ? "학식 정보를 가져올 수 없습니다." : meal));
    }
}
