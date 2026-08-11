package com.campusflow.domain.storage.dto;

/**
 * 파일 스트리밍 티켓 응답. {@code streamUrl}은 티켓이 포함된 완성 URL이라 프론트가 그대로
 * {@code <img src>/<video src>/<iframe src>}에 넣으면 된다.
 */
public record FileTicketResponse(Long fileId, String streamUrl, String token) {
}
