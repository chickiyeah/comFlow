package com.campusflow.domain.classpost.controller;

import com.campusflow.domain.classpost.dto.*;
import com.campusflow.domain.classpost.service.ClassPostService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClassPostController {

    private final ClassPostService postService;

    @GetMapping("/api/classes/{classId}/posts")
    public ApiResponse<List<PostResponse>> list(@AuthenticationPrincipal String username,
                                                @PathVariable Long classId) {
        return ApiResponse.ok(postService.list(username, classId));
    }

    @PostMapping("/api/classes/{classId}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> create(@AuthenticationPrincipal String username,
                                            @PathVariable Long classId,
                                            @Valid @RequestBody PostCreateRequest request) {
        return ApiResponse.ok(postService.create(username, classId, request));
    }

    @PutMapping("/api/posts/{postId}")
    public ApiResponse<PostResponse> update(@AuthenticationPrincipal String username,
                                            @PathVariable Long postId,
                                            @Valid @RequestBody PostUpdateRequest request) {
        return ApiResponse.ok(postService.update(username, postId, request));
    }

    @DeleteMapping("/api/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long postId) {
        postService.delete(username, postId);
    }

    @PostMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostCommentResponse> addComment(@AuthenticationPrincipal String username,
                                                       @PathVariable Long postId,
                                                       @Valid @RequestBody PostCommentRequest request) {
        return ApiResponse.ok(postService.addComment(username, postId, request));
    }

    @DeleteMapping("/api/posts/{postId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal String username,
                              @PathVariable Long postId,
                              @PathVariable Long commentId) {
        postService.deleteComment(username, postId, commentId);
    }
}
