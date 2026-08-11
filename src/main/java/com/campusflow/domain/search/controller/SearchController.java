package com.campusflow.domain.search.controller;

import com.campusflow.domain.course.repository.OnlineCourseRepository;
import com.campusflow.domain.notice.repository.NoticeRepository;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/** 통합 검색 — 공지·온라인 강좌 제목 검색 (TopNav 검색창) */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final NoticeRepository noticeRepository;
    private final OnlineCourseRepository courseRepository;

    public record SearchItem(String type, Long id, String title, String subtitle, String link) {}

    @GetMapping
    public ApiResponse<List<SearchItem>> search(@RequestParam(name = "q", defaultValue = "") String q) {
        List<SearchItem> results = new ArrayList<>();
        String query = q.trim();
        if (query.length() < 1) return ApiResponse.ok(results);

        noticeRepository.findTop5ByTitleContainingIgnoreCaseOrderByCreatedAtDesc(query).forEach(n ->
                results.add(new SearchItem("notice", n.getId(), n.getTitle(),
                        n.getSummary() != null ? n.getSummary() : "공지사항", "/notices")));

        courseRepository.findTop5ByActiveTrueAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(query).forEach(c ->
                results.add(new SearchItem("course", c.getId(), c.getTitle(),
                        c.getInstructorName() != null ? c.getInstructorName() : "온라인 강좌", "/courses")));

        return ApiResponse.ok(results);
    }
}
