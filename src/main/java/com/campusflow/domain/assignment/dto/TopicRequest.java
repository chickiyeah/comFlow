package com.campusflow.domain.assignment.dto;

import jakarta.validation.constraints.Size;

public record TopicRequest(
        @Size(max = 100, message = "주제는 100자 이하여야 합니다.")
        String topic
) {
}
