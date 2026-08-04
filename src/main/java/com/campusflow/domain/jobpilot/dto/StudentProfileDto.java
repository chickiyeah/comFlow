package com.campusflow.domain.jobpilot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 학생 프로필 — [매칭]·[생성]의 사실 근거(source of truth).
 * 자소서는 여기에 있는 내용만으로 작성한다(할루시네이션 가드의 데이터 측 방어선).
 *
 * careers / projects 를 구분하는 이유: 자소서에서 '회사 경력'과 '개인 프로젝트'를
 * 정직하게 구분하기 위함(참조구현 generator/profile.py).
 *
 * 서버에서 ProfileAssembler 가 학생 데이터로 조립하지만, 검토 UI 에서 사용자가
 * 보정해 다시 보낼 수도 있으므로 요청/응답 양방향으로 쓰인다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StudentProfileDto(
        String name,
        String education,                 // 학력 (예: 전주비전대 컴퓨터정보과)
        String summary,                   // 한 줄 정체성
        List<String> skills,
        List<String> certifications,
        List<CareerItem> careers,         // 회사 경력
        List<ProjectItem> projects,       // 프로젝트
        List<String> constraints          // 생성 시 지켜야 할 제약 (예: '학력 약점은 쓰지 않음')
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CareerItem(
            String company,
            String role,
            String period,
            boolean isPersonal,           // 개인 프로젝트면 true
            List<String> highlights
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectItem(
            String name,
            String summary,
            List<String> tech,
            List<String> highlights,
            boolean isPersonal
    ) {}

    public StudentProfileDto normalized() {
        return new StudentProfileDto(
                name, education, summary,
                orEmpty(skills), orEmpty(certifications),
                orEmpty(careers), orEmpty(projects), orEmpty(constraints)
        );
    }

    /** 프롬프트에 넣을 근거 텍스트로 직렬화 (generator/profile.py as_evidence_block 포팅). */
    public String asEvidenceBlock() {
        StringBuilder sb = new StringBuilder();
        if (education != null && !education.isBlank()) sb.append("[학력] ").append(education).append('\n');
        if (notEmpty(skills)) sb.append("[스킬] ").append(String.join(", ", skills)).append('\n');
        if (notEmpty(certifications)) sb.append("[자격증] ").append(String.join(", ", certifications)).append('\n');
        if (notEmpty(careers)) {
            sb.append("[회사 경력]\n");
            for (CareerItem c : careers) {
                String tag = c.isPersonal() ? "(개인)" : "(회사)";
                sb.append("  - ").append(tag).append(' ').append(c.company())
                        .append(" / ").append(c.role())
                        .append(" / ").append(c.period() == null ? "" : c.period()).append('\n');
                if (c.highlights() != null) for (String h : c.highlights()) sb.append("      · ").append(h).append('\n');
            }
        }
        if (notEmpty(projects)) {
            sb.append("[프로젝트]\n");
            for (ProjectItem p : projects) {
                String tag = p.isPersonal() ? "(개인)" : "(회사)";
                sb.append("  - ").append(tag).append(' ').append(p.name())
                        .append(": ").append(p.summary() == null ? "" : p.summary()).append('\n');
                if (notEmpty(p.tech())) sb.append("      tech: ").append(String.join(", ", p.tech())).append('\n');
                if (p.highlights() != null) for (String h : p.highlights()) sb.append("      · ").append(h).append('\n');
            }
        }
        return sb.toString().trim();
    }

    private static boolean notEmpty(List<?> l) { return l != null && !l.isEmpty(); }

    private static <T> List<T> orEmpty(List<T> list) { return list == null ? List.of() : list; }
}
