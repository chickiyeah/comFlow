package com.campusflow.domain.classpost.dto;

import com.campusflow.domain.classpost.entity.PostType;
import jakarta.validation.constraints.NotBlank;

/** materialId 지정 시 자료 참조 게시글(type=MATERIAL). */
public record PostCreateRequest(
        @NotBlank(message = "내용은 필수입니다.")
        String body,

        PostType type,
        Long materialId
) {
}
