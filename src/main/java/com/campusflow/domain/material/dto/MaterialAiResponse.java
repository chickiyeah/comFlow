package com.campusflow.domain.material.dto;

/**
 * 자료별 AI 액션 응답. type=chat이면 {@code answer}(평문), type=quiz면 {@code data}(문항 배열).
 */
public record MaterialAiResponse(
        String type,
        String answer,
        Object data
) {
    public static MaterialAiResponse chat(String answer) {
        return new MaterialAiResponse("chat", answer, null);
    }

    public static MaterialAiResponse quiz(Object questions) {
        return new MaterialAiResponse("quiz", null, questions);
    }
}
