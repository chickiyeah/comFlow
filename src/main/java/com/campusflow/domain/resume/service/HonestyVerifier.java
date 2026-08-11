package com.campusflow.domain.resume.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.resume.dto.HonestyReport;
import com.campusflow.domain.resume.util.JsonExtract;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 적대적 정직성 검증: 생성된 자소서 문장 중 '근거(evidence)'에 없는 과장/창작을 감지하고,
 * 감지되면 근거 기반으로 순화 재생성(최대 2회). 통과하거나 재시도 소진 시 최종본+수정로그 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HonestyVerifier {

    private final AiFacadeService aiFacadeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_REWRITE = 2;

    private static final String VERIFY_SYSTEM = """
            당신은 이력서 자기소개서의 정직성 검증관입니다.
            '지원자 근거'에 명시되지 않은 사실(경험·수치·자격·회사·성과)을 주장하는 문장을 찾아냅니다.
            - 근거에 없는 자격/경력/수치를 언급하면 위반입니다.
            - 근거에 있는 사실을 일반적 포부·의지로 표현한 것은 위반이 아닙니다.
            출력은 위반 사유 문자열의 JSON 배열만. 위반이 없으면 정확히 [] 만 출력합니다.
            """;

    private static final String VERIFY_USER = """
            [지원자 근거]
            %s

            [검사할 자소서 문장]
            %s

            근거에 없는 주장을 하는 부분을 위반 사유 배열로 출력하세요. 없으면 [].
            """;

    private static final String REWRITE_SYSTEM = """
            당신은 이력서 자기소개서를 근거 기반으로 순화(교정)하는 작성자입니다.
            지적된 과장·창작 부분을 제거하거나, 근거에 있는 사실로만 대체해 다시 씁니다.
            없는 내용은 채우지 말고 삭제하세요(창작보다 공백 우선). 존댓말, 본문만 출력.
            """;

    private static final String REWRITE_USER = """
            [지원자 근거]
            %s

            [지적된 위반]
            %s

            [고칠 원문]
            %s

            위반을 모두 제거/순화한 본문만 출력하세요.
            """;

    public record FixResult(String text, List<HonestyReport.Fix> fixes) {}

    public FixResult verifyAndFix(String sectionLabel, String text, String evidence) {
        List<HonestyReport.Fix> fixes = new ArrayList<>();
        String current = text;

        for (int i = 0; i < MAX_REWRITE; i++) {
            List<String> violations = detect(current, evidence);
            if (violations.isEmpty()) break;

            String reason = String.join("; ", violations);
            String rewritten = aiFacadeService.ask(
                    REWRITE_SYSTEM,
                    REWRITE_USER.formatted(evidence, reason, current)).trim();

            fixes.add(new HonestyReport.Fix(sectionLabel, current, rewritten, reason));
            current = rewritten;
        }
        return new FixResult(current, fixes);
    }

    private List<String> detect(String text, String evidence) {
        try {
            String raw = aiFacadeService.ask(VERIFY_SYSTEM, VERIFY_USER.formatted(evidence, text));
            String json = JsonExtract.array(raw);
            String[] arr = objectMapper.readValue(json, String[].class);
            return List.of(arr);
        } catch (Exception e) {
            log.warn("[Honesty] 검증 파싱 실패 — 통과 처리: {}", e.getMessage());
            return List.of();
        }
    }
}
