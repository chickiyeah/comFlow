package com.campusflow.domain.material.controller;

import com.campusflow.domain.material.dto.BookmarkRequest;
import com.campusflow.domain.material.dto.BookmarkResponse;
import com.campusflow.domain.material.service.BookmarkService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials/{materialId}/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping
    public ApiResponse<List<BookmarkResponse>> list(@AuthenticationPrincipal String username,
                                                    @PathVariable Long materialId) {
        return ApiResponse.ok(bookmarkService.list(username, materialId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookmarkResponse> add(@AuthenticationPrincipal String username,
                                             @PathVariable Long materialId,
                                             @Valid @RequestBody BookmarkRequest request) {
        return ApiResponse.ok(bookmarkService.add(username, materialId, request));
    }

    @DeleteMapping("/{page}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username,
                       @PathVariable Long materialId,
                       @PathVariable int page) {
        bookmarkService.delete(username, materialId, page);
    }
}
