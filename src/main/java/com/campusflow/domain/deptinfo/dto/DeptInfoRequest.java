package com.campusflow.domain.deptinfo.dto;

import com.campusflow.domain.deptinfo.entity.DeptInfoCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeptInfoRequest(
        @NotNull DeptInfoCategory category,
        @NotBlank String title,
        @NotBlank String content,
        String keywords,
        Boolean active
) {}
