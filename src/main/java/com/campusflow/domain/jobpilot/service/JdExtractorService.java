package com.campusflow.domain.jobpilot.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.jobpilot.dto.JobPosting;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * [추출] 모듈. 공고 원문 → 구조화된 {@link JobPosting}.
 * 참조구현 jd_extractor 를 CampusFlow(AiFacadeService + Jackson) 로 포팅.
 *
 * 설계 원칙(참조구현 prompt.py):
 *  1. 원문에 명시된 것만 추출. 없으면 null/빈 배열. 추측·창작 금지.
 *  2. 한국 채용공고 특성(글자수 제한, 공백포함/제외, '신입·경력') 반영.
 *  3. 스키마에 맞는 JSON 객체 하나만 출력.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdExtractorService {

    private final AiFacadeService aiFacadeService;
    private final ObjectMapper objectMapper;

    private static final int MIN_LEN = 30;

    private static final String SYSTEM_PROMPT = """
            당신은 한국 채용 공고에서 핵심 정보를 정확히 추출하는 파서입니다.
            주어진 공고 텍스트를 읽고, 아래 JSON 스키마에 맞춰 구조화하세요.

            [절대 규칙]
            - 원문에 명시적으로 있는 정보만 추출합니다. 추론·추측·창작은 절대 금지합니다.
            - 정보가 없는 단일 필드는 null, 리스트 필드는 빈 배열([])로 둡니다.
            - 원문 표현을 임의로 바꾸지 않습니다(특히 마감일, 경력, 학력은 원문 표기 유지).
            - 출력은 JSON 객체 하나만. 코드블록(```), 설명, 주석을 붙이지 않습니다.

            [JSON 스키마 — 키 이름을 정확히 이대로 사용]
            {
              "company": "회사명 또는 null",
              "position": "직무/모집분야명 또는 null",
              "employmentType": "정규직|계약직|인턴|기타 또는 null",
              "experience": "경력 요건 원문 (예: '신입·경력') 또는 null",
              "education": "학력 요건 원문 (예: '대졸이상','무관') 또는 null",
              "location": "근무 지역 또는 null",
              "deadline": "마감일 원문 표기 그대로 또는 null",
              "salary": "급여 원문 또는 null",
              "requiredSkills": ["요구 기술/툴 (Python, Docker, RAG 등)"],
              "requirements": ["필수 자격요건 문장"],
              "preferred": ["우대사항 문장"],
              "responsibilities": ["주요 업무/수행 직무 문장"],
              "essayQuestions": [
                {"question": "자소서 문항", "charLimit": 800, "charLimitType": "공백포함|공백제외 또는 null"}
              ],
              "rawNotes": "위 항목에 안 들어가는 중요한 비고(조기마감 가능 등) 또는 null",
              "missingFields": ["company,position,deadline,experience,education 중 못 찾아 null 로 둔 필드명"]
            }

            [지침]
            - requiredSkills 는 기술/툴 위주. 소프트스킬(커뮤니케이션 등)은 requirements 로.
            - charLimit 은 숫자만(예: 800). 명시 없으면 null. charLimitType 은 원문에 있을 때만.
            - 공고에 자소서 문항 언급이 전혀 없으면 essayQuestions 는 빈 배열.
            - 반드시 유효한 JSON 하나만 출력하세요.
            """;

    public JobPosting extract(String jdText) {
        String text = jdText == null ? "" : jdText.trim();
        if (text.length() < MIN_LEN) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String userMessage = """
                다음은 채용 공고 텍스트입니다. 스키마에 맞춰 JSON으로 추출하세요.
                <job_posting>
                %s
                </job_posting>
                """.formatted(text);

        String raw = aiFacadeService.ask(SYSTEM_PROMPT, userMessage);
        String json = stripFences(raw);

        try {
            JobPosting parsed = objectMapper.readValue(json, JobPosting.class);
            return parsed.normalized();
        } catch (Exception e) {
            log.warn("[JobPilot] 추출 JSON 파싱 실패: {} / 원문: {}", e.getMessage(),
                    json.substring(0, Math.min(300, json.length())));
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    /** 모델이 ```json ... ``` 으로 감싸거나 앞뒤 잡텍스트를 붙여도 JSON 본문만 회수. */
    private static String stripFences(String raw) {
        if (raw == null) return "";
        String t = raw.trim().replaceAll("(?s)```json?\\s*", "").replaceAll("```\\s*$", "").trim();
        // 첫 '{' 부터 마지막 '}' 까지만 취함 (reasoning 모델의 군더더기 방어)
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) return t.substring(start, end + 1);
        return t;
    }
}
