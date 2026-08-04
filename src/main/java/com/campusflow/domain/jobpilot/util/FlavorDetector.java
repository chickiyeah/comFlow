package com.campusflow.domain.jobpilot.util;

import java.util.List;
import java.util.Map;

/**
 * 직무 색(flavor) 판별 + 색별 강조 가이드 (참조구현 generator/flavor.py 포팅).
 *
 * 같은 프로필이라도 직무 색에 따라 어떤 경험을 앞세울지 가이드를 프롬프트에 주입한다.
 * 판별은 규칙 기반(빠르고 결정적). 키워드는 position + requiredSkills + responsibilities 에서 본다.
 */
public final class FlavorDetector {

    private FlavorDetector() {}

    public record Flavor(String key, String label, String emphasis) {}

    public static final Map<String, Flavor> FLAVORS = Map.of(
            "mlops", new Flavor("mlops", "MLOps / 모델 운영",
                    "모델 서빙·라우팅, 사용량/비용 관측, RAG 파이프라인을 전면에 둔다. 인프라(서버/컨테이너)는 보조로."),
            "infra", new Flavor("infra", "시스템 / 인프라 엔지니어",
                    "서버 운영, 컨테이너(Docker/Portainer), 네트워크, 가상화, 모니터링, 리눅스를 전면에 둔다. "
                            + "AI 관련 경험은 마지막에 한 덩어리로 곁들여 'AI' 키워드만 커버."),
            "platform", new Flavor("platform", "AI 플랫폼 엔지니어",
                    "LLMOps(모델 서빙/관측), 컨테이너 배포, RAG를 함께 전면에. 공고가 요구한 스킬 키워드와 1:1로 대응시킨다."),
            "backend", new Flavor("backend", "백엔드 개발",
                    "Python/FastAPI/Spring 등 서버 개발, REST API 설계, 데이터 처리, 시스템 연동을 전면에 둔다."),
            "ops", new Flavor("ops", "운영지원 / 정산 / 데이터관리",
                    "데이터 정확성과 꼼꼼함, 커뮤니케이션, 운영 지원 경험을 전면에 둔다. 기술 용어는 풀어서 설명한다(비개발 면접관 고려)."),
            "general", new Flavor("general", "일반",
                    "직무와 가장 관련된 경험을 골라 앞세우고, 핵심 역량을 균형 있게 보여준다.")
    );

    // 키워드 → flavor (위에서부터 우선 매칭)
    private static final List<Map.Entry<String, List<String>>> RULES = List.of(
            Map.entry("mlops",    List.of("mlops", "모델 서빙", "모델서빙", "model serving", "추론 서빙")),
            Map.entry("platform", List.of("llmops", "ai 플랫폼", "플랫폼 엔지니어", "ai platform", "kubernetes", "openshift")),
            Map.entry("infra",    List.of("시스템 엔지니어", "인프라", "서버 관리", "서버관리", "네트워크", "리눅스",
                    "컨테이너", "infra", "system engineer", "sre", "devops")),
            Map.entry("backend",  List.of("백엔드", "backend", "서버 개발", "api 개발")),
            Map.entry("ops",      List.of("정산", "운영 지원", "운영지원", "데이터 관리", "데이터관리", "erp", "사무"))
    );

    /** 직무 색 판별. */
    public static Flavor detect(String position, List<String> skills, List<String> responsibilities) {
        StringBuilder hay = new StringBuilder(position == null ? "" : position).append(' ');
        if (skills != null) skills.forEach(s -> hay.append(s).append(' '));
        if (responsibilities != null) responsibilities.forEach(s -> hay.append(s).append(' '));
        String haystack = hay.toString().toLowerCase();

        for (var rule : RULES) {
            for (String kw : rule.getValue()) {
                if (haystack.contains(kw.toLowerCase())) {
                    return FLAVORS.get(rule.getKey());
                }
            }
        }
        return FLAVORS.get("general");
    }
}
