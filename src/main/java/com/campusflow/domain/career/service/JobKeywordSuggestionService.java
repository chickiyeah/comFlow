package com.campusflow.domain.career.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.career.dto.KeywordSuggestionResponse;
import com.campusflow.domain.jobpilot.service.ProfileAssembler;
import com.campusflow.domain.resume.util.JsonExtract;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 로그인 학생의 희망직무·보유기술을 근거로 채용 검색 기본 키워드와
 * "이 직무는 어때요?" 추천 칩(4~6개)을 생성한다.
 *
 * AI(게이트웨이) 호출이 실패하거나 파싱이 안 되면 스킬→키워드 룰 기반으로 폴백해
 * 이 기능이 요청 전체를 실패시키지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobKeywordSuggestionService {

    private static final int MAX_SUGGESTIONS = 6;

    /** 보유 기술(부분 문자열, 소문자)이 매칭되면 대응하는 직무 검색어로 추천한다. */
    private static final Map<String, String> SKILL_KEYWORD_MAP = new LinkedHashMap<>();
    static {
        SKILL_KEYWORD_MAP.put("java", "백엔드 개발자");
        SKILL_KEYWORD_MAP.put("spring", "백엔드 개발자");
        SKILL_KEYWORD_MAP.put("python", "파이썬 개발자");
        SKILL_KEYWORD_MAP.put("react", "프론트엔드 개발자");
        SKILL_KEYWORD_MAP.put("javascript", "프론트엔드 개발자");
        SKILL_KEYWORD_MAP.put("typescript", "프론트엔드 개발자");
        SKILL_KEYWORD_MAP.put("docker", "DevOps 엔지니어");
        SKILL_KEYWORD_MAP.put("kubernetes", "DevOps 엔지니어");
        SKILL_KEYWORD_MAP.put("nginx", "DevOps 엔지니어");
        SKILL_KEYWORD_MAP.put("rag", "AI 엔지니어");
        SKILL_KEYWORD_MAP.put("litellm", "AI 엔지니어");
        SKILL_KEYWORD_MAP.put("vllm", "AI 엔지니어");
        SKILL_KEYWORD_MAP.put("mcp", "AI 엔지니어");
        SKILL_KEYWORD_MAP.put("anthropic", "AI 엔지니어");
        SKILL_KEYWORD_MAP.put("sql", "데이터 엔지니어");
        SKILL_KEYWORD_MAP.put("mysql", "데이터 엔지니어");
        SKILL_KEYWORD_MAP.put("postgresql", "데이터 엔지니어");
    }

    private static final List<String> ULTIMATE_FALLBACK = List.of("IT", "백엔드 개발자", "프론트엔드 개발자");

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ProfileAssembler profileAssembler;
    private final AiFacadeService aiFacadeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KeywordSuggestionResponse suggest(String username) {
        Student student = getStudent(username);
        String desiredJob = student.getDesiredJob();
        List<String> skills = profileAssembler.assemble(username).skills();
        if (skills == null) skills = List.of();

        String defaultKeyword;
        if (desiredJob != null && !desiredJob.isBlank()) {
            defaultKeyword = desiredJob;
        } else if (!skills.isEmpty()) {
            defaultKeyword = skills.get(0);
        } else {
            defaultKeyword = "IT";
        }

        List<String> suggestions = aiSuggestions(desiredJob, skills, student.getDepartment());
        if (suggestions == null || suggestions.isEmpty()) {
            suggestions = ruleBasedSuggestions(desiredJob, skills);
        }

        return new KeywordSuggestionResponse(defaultKeyword, suggestions);
    }

    private List<String> aiSuggestions(String desiredJob, List<String> skills, String department) {
        try {
            String system = "너는 취업 검색어 추천기다. 학생의 학과·희망직무·보유기술을 보고, "
                    + "채용사이트에서 바로 검색 가능한 간결한 '직무 검색어' 4~6개를 한국어로 제안한다. "
                    + "각 항목은 2~10자, 직무명 위주(예: 백엔드 개발자, DevOps 엔지니어, 파이썬 개발자). "
                    + "출력은 JSON 문자열 배열만.";
            StringBuilder user = new StringBuilder();
            user.append("학과: ").append(department == null ? "컴퓨터정보과" : department).append('\n');
            user.append("희망직무: ").append(desiredJob == null || desiredJob.isBlank() ? "미정" : desiredJob).append('\n');
            user.append("보유기술: ").append(skills.isEmpty() ? "없음" : String.join(", ", skills));

            String raw = aiFacadeService.ask(system, user.toString());
            String json = JsonExtract.array(raw);
            String[] parsed = objectMapper.readValue(json, String[].class);
            return cleanAndCap(List.of(parsed));
        } catch (Exception e) {
            log.warn("[JobKeywordSuggestion] AI 추천 실패, 룰 기반으로 폴백: {}", e.getMessage());
            return null;
        }
    }

    private List<String> ruleBasedSuggestions(String desiredJob, List<String> skills) {
        List<String> candidates = new ArrayList<>();
        if (desiredJob != null && !desiredJob.isBlank()) candidates.add(desiredJob);
        for (String skill : skills) {
            if (skill == null || skill.isBlank()) continue;
            String lower = skill.toLowerCase();
            for (Map.Entry<String, String> e : SKILL_KEYWORD_MAP.entrySet()) {
                if (lower.contains(e.getKey())) candidates.add(e.getValue());
            }
        }
        candidates.add("백엔드 개발자");

        List<String> cleaned = cleanAndCap(candidates);
        return cleaned.isEmpty() ? ULTIMATE_FALLBACK : cleaned;
    }

    /** trim, 공백 제거, 대소문자 무시 중복 제거, 최대 개수 제한. */
    private static List<String> cleanAndCap(List<String> raw) {
        Set<String> seenLower = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String s : raw) {
            if (s == null) continue;
            String trimmed = s.trim();
            if (trimmed.isEmpty()) continue;
            String key = trimmed.toLowerCase();
            if (seenLower.add(key)) {
                result.add(trimmed);
                if (result.size() >= MAX_SUGGESTIONS) break;
            }
        }
        return result;
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
