package com.campusflow.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "유효하지 않은 입력입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),

    // 인증
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    DUPLICATE_STUDENT_ID(HttpStatus.CONFLICT, "이미 존재하는 학번입니다."),

    // 학생
    STUDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."),

    // 성적
    GRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "성적 정보를 찾을 수 없습니다."),

    // 출결
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "출결 정보를 찾을 수 없습니다."),

    // 로드맵
    ROADMAP_NOT_FOUND(HttpStatus.NOT_FOUND, "로드맵을 찾을 수 없습니다."),

    // 이메일 인증
    EMAIL_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "인증 코드를 먼저 요청해주세요."),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다. 다시 요청해주세요."),
    EMAIL_CODE_INVALID(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),

    // AI
    AI_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI 서비스 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
