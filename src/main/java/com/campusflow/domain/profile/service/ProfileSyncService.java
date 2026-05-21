package com.campusflow.domain.profile.service;

import com.campusflow.domain.profile.dto.ProfileResponse;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 학교 포털(10.8.0.14:8000) 연동 서비스
 * 학번/비밀번호로 학교 포털 인증 → 학생 정보 + 학생증 사진 + 토큰 + 세션 쿠키 동기화
 * NMain API는 Bearer token + 세션 쿠키 모두 필요 (어느 하나만으로는 need login 오류)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileSyncService {

    @Value("${intranet.sync.url:http://10.8.0.14:8000}")
    private String intranetUrl;

    private final UserRepository    userRepo;
    private final StudentRepository studentRepo;
    private final ObjectMapper      objectMapper;

    public ProfileResponse getProfile(String username) {
        return ProfileResponse.from(getStudent(username));
    }

    @Transactional
    public ProfileResponse syncWithPortal(String username, String schoolPassword) {
        Student student = getStudent(username);

        String raw;
        try {
            raw = RestClient.create(intranetUrl).post()
                    .uri("/api/sync-profile")
                    .header("Content-Type", "application/json")
                    .body(Map.of("user_id", student.getStudentId(), "user_pw", schoolPassword))
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("학교 포털 API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            if (!root.path("success").asBoolean()) {
                String msg = root.path("message").asText("학교 포털 연동 실패");
                throw new IllegalStateException(msg);
            }

            JsonNode data = root.path("data");

            String accessToken  = data.path("accessToken").asText(null);
            String refreshToken = data.path("refreshToken").asText(null);

            // sessionCookies는 JSON 객체 → 문자열로 직렬화해서 DB 저장
            String sessionCookiesJson = null;
            JsonNode cookiesNode = data.path("sessionCookies");
            if (!cookiesNode.isMissingNode() && !cookiesNode.isNull()) {
                sessionCookiesJson = objectMapper.writeValueAsString(cookiesNode);
            }

            student.applyIntranetSync(
                    data.path("name").asText(null),
                    data.path("phone").asText(null),
                    data.path("email").asText(null),
                    data.has("profileImage") && !data.path("profileImage").isNull()
                            ? data.path("profileImage").asText() : null,
                    accessToken,
                    refreshToken,
                    sessionCookiesJson
            );

            log.info("[포털 연동] {} — accessToken={} refreshToken={} cookies={}",
                    student.getStudentId(),
                    accessToken  != null ? accessToken.length()  + "chars" : "null",
                    refreshToken != null ? refreshToken.length() + "chars" : "null",
                    sessionCookiesJson != null ? "ok" : "null");

            return ProfileResponse.from(student);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("포털 응답 파싱 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    /**
     * 저장된 세션 쿠키 + Bearer 토큰으로 포털 데이터 조회
     * NMain API는 두 가지 모두 필요
     */
    public String fetchPortalData(String username, String sqlId) {
        Student student = getStudent(username);

        if (student.getPortalAccessToken() == null || student.getPortalSessionCookies() == null) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND); // 미연동 상태
        }

        try {
            // session_cookies may be a List (new domain-aware format) or Map (old flat format)
            Object cookies = objectMapper.readValue(
                    student.getPortalSessionCookies(),
                    Object.class
            );

            Map<String, Object> body = new HashMap<>();
            body.put("user_id",       student.getStudentId());
            body.put("access_token",  student.getPortalAccessToken());
            body.put("session_cookies", cookies);
            if (sqlId != null) body.put("sql_id", sqlId);

            return RestClient.create(intranetUrl).post()
                    .uri("/api/portal")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("포털 데이터 조회 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Transactional
    public ProfileResponse disableSync(String username) {
        Student student = getStudent(username);
        student.disableIntranetSync();
        return ProfileResponse.from(student);
    }

    private Student getStudent(String username) {
        Long userId = userRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
