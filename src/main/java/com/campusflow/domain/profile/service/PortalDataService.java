package com.campusflow.domain.profile.service;

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
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 학교 포털 데이터 조회 서비스
 * 10.8.0.14:8000의 전용 엔드포인트를 통해 NMain 데이터를 가져옴
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalDataService {

    @Value("${intranet.sync.url:http://10.8.0.14:8000}")
    private String intranetUrl;

    private final UserRepository    userRepo;
    private final StudentRepository studentRepo;
    private final ObjectMapper      objectMapper;

    /** 성적 학기 목록 조회 (ussc9001m_s01) */
    public List<Map<String, Object>> fetchGradeTerms(String username) {
        Student s = getStudent(username);
        return callPortal(s, "/api/grades/terms", Map.of());
    }

    /** 성적 상세 조회 (ussc9001m_s02) */
    public List<Map<String, Object>> fetchGradeDetail(
            String username, String year, String smr, Map<String, String> termMeta) {
        Student s = getStudent(username);
        Map<String, Object> extra = new HashMap<>(termMeta);
        extra.put("year", year);
        extra.put("smr", smr);
        extra.put("smr_nm", termMeta.getOrDefault("smr_nm", ""));
        extra.put("bach_warn_cnt",  termMeta.getOrDefault("bach_warn_cnt",  "0"));
        extra.put("sum_fac_point",  termMeta.getOrDefault("sum_fac_point",  "0"));
        extra.put("sum_acq_point",  termMeta.getOrDefault("sum_acq_point",  "0"));
        extra.put("cnt_atlec_sbjt", termMeta.getOrDefault("cnt_atlec_sbjt", "0"));
        extra.put("cnt_evl_sbjt",   termMeta.getOrDefault("cnt_evl_sbjt",   "0"));
        extra.put("perg_avg",       termMeta.getOrDefault("perg_avg",        "0"));
        extra.put("gpa_avg",        termMeta.getOrDefault("gpa_avg",         "0"));
        extra.put("perc_pnt",       termMeta.getOrDefault("perc_pnt",        "0"));
        return callPortal(s, "/api/grades/detail", extra);
    }

    /** 수강 시간표 조회 (ussu9001m_s01) */
    public List<Map<String, Object>> fetchSchedule(String username, String year, String smr) {
        Student s = getStudent(username);
        return callPortal(s, "/api/schedule/portal", Map.of("year", year, "smr", smr));
    }

    /**
     * 통합 출결 조회 (check.jvision + LMS 병합)
     * schoolPassword: LMS/check.jvision 로그인에 필요 (저장 안 됨)
     */
    public Map<String, Object> fetchAttendanceCombined(
            String username, String schoolPassword, String year, String term) {
        Student s = getStudent(username);
        // 비밀번호는 요청에만 사용, 저장 안 함
        try {
            Object cookies = objectMapper.readValue(s.getPortalSessionCookies(), Object.class);
            Map<String, Object> body = new HashMap<>();
            body.put("user_id",       s.getStudentId());
            body.put("user_pw",       schoolPassword);
            body.put("session_cookies", cookies);
            body.put("year", year);
            body.put("term", term);

            String raw = RestClient.create(intranetUrl).post()
                    .uri("/api/attendance/combined")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            return objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("통합 출결 파싱 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    /** 통학버스 조회 (sdsb9001m_s01~s03) */
    public Map<String, Object> fetchShuttle(String username) {
        Student s = getStudent(username);
        String raw = callPortalRaw(s, "/api/shuttle", Map.of());
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (!root.path("success").asBoolean()) {
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
            }
            // datasets: { ds_out_1: [...], ds_out_2: [...], ... }
            return objectMapper.convertValue(root.path("datasets"),
                    new TypeReference<Map<String, Object>>() {});
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("통학버스 파싱 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    // ── 공통 ───────────────────────────────────────────────────────────

    private List<Map<String, Object>> callPortal(Student s, String path, Map<String, ?> extra) {
        String raw = callPortalRaw(s, path, extra);
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (!root.path("success").asBoolean()) {
                return List.of();
            }
            return objectMapper.convertValue(root.path("data"),
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("{} 파싱 실패: {}", path, e.getMessage());
            return List.of();
        }
    }

    private String callPortalRaw(Student s, String path, Map<String, ?> extra) {
        if (s.getPortalAccessToken() == null || s.getPortalSessionCookies() == null) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND);
        }
        try {
            Object cookies = objectMapper.readValue(s.getPortalSessionCookies(), Object.class);
            Map<String, Object> body = new HashMap<>();
            body.put("user_id",       s.getStudentId());
            body.put("access_token",  s.getPortalAccessToken());
            body.put("session_cookies", cookies);
            body.putAll(extra.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

            return RestClient.create(intranetUrl).post()
                    .uri(path)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} 호출 실패: {}", path, e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private Student getStudent(String username) {
        Long userId = userRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
