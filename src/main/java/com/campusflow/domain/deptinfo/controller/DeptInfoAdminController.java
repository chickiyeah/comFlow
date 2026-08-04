package com.campusflow.domain.deptinfo.controller;

import com.campusflow.domain.deptinfo.dto.DeptInfoRequest;
import com.campusflow.domain.deptinfo.dto.DeptInfoResponse;
import com.campusflow.domain.deptinfo.service.DeptInfoService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 학과 내부정보 관리 (ADMIN 전용 — /api/admin/** 보안 규칙 적용) */
@RestController
@RequestMapping("/api/admin/dept-info")
@RequiredArgsConstructor
public class DeptInfoAdminController {

    private final DeptInfoService deptInfoService;

    @GetMapping
    public ApiResponse<List<DeptInfoResponse>> list() {
        return ApiResponse.ok(deptInfoService.list());
    }

    @PostMapping
    public ApiResponse<DeptInfoResponse> create(@Valid @RequestBody DeptInfoRequest request) {
        return ApiResponse.ok(deptInfoService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DeptInfoResponse> update(@PathVariable Long id, @Valid @RequestBody DeptInfoRequest request) {
        return ApiResponse.ok(deptInfoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deptInfoService.delete(id);
        return ApiResponse.ok(null);
    }
}
