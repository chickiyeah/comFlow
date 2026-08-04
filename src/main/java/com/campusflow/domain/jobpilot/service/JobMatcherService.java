package com.campusflow.domain.jobpilot.service;

import com.campusflow.domain.jobpilot.dto.JobPosting;
import com.campusflow.domain.jobpilot.dto.MatchReport;
import com.campusflow.domain.jobpilot.dto.StudentProfileDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * [매칭] 모듈. 공고 요구사항 ↔ 학생 프로필 대조 → {@link MatchReport}.
 *
 * 참조구현은 ChromaDB 임베딩 유사도를 썼으나, CampusFlow 규칙상 ChromaDB 직접 연결은
 * 금지(CLAUDE.md)이므로 **규칙 기반(키워드) 매칭**으로 구현한다. 결정적이고 빠르며,
 * 학과 프로필 규모(스킬·프로젝트 수십 건)에서는 충분하다.
 *
 * - requiredSkills 보유 여부로 matched/missing 산출 (missing = 'must' gap)
 * - preferred 중 단일 기술 토큰은 'preferred' gap 으로 추가
 * - strengths 의 evidence 는 매칭 근거가 된 실제 프로필 항목 텍스트
 */
@Slf4j
@Service
public class JobMatcherService {

    /** 매칭 근거 후보가 되는 프로필 항목. */
    private record ProfileItem(String label, String text) {}

    public MatchReport match(JobPosting jobRaw, StudentProfileDto profileRaw) {
        JobPosting job = jobRaw.normalized();
        StudentProfileDto profile = profileRaw.normalized();

        List<ProfileItem> items = buildItems(profile);

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<MatchReport.Strength> strengths = new ArrayList<>();
        List<MatchReport.Gap> gaps = new ArrayList<>();

        // 1) 필수 요구 스킬 (missing → 'must')
        for (String skill : dedup(job.requiredSkills())) {
            ProfileItem evidence = findEvidence(skill, items);
            if (evidence != null) {
                matched.add(skill);
                strengths.add(new MatchReport.Strength(skill, evidence.label() + ": " + shorten(evidence.text())));
            } else {
                missing.add(skill);
                gaps.add(new MatchReport.Gap(skill, "must"));
            }
        }

        // 2) 우대사항 중 '단일 기술 토큰'만 추려 미보유 시 'preferred' gap
        for (String pref : dedup(job.preferred())) {
            if (!looksLikeSkillToken(pref)) continue;
            if (matched.stream().anyMatch(m -> m.equalsIgnoreCase(pref))) continue;
            if (findEvidence(pref, items) == null
                    && gaps.stream().noneMatch(g -> g.skill().equalsIgnoreCase(pref))) {
                gaps.add(new MatchReport.Gap(pref, "preferred"));
            }
        }

        String summary = buildSummary(job, matched, missing, gaps);
        return new MatchReport(matched, missing, strengths, gaps, summary);
    }

    private List<ProfileItem> buildItems(StudentProfileDto p) {
        List<ProfileItem> items = new ArrayList<>();
        p.skills().forEach(s -> items.add(new ProfileItem("스킬", s)));
        p.certifications().forEach(c -> items.add(new ProfileItem("자격증", c)));
        for (var proj : p.projects()) {
            StringBuilder t = new StringBuilder(nz(proj.name())).append(' ').append(nz(proj.summary()));
            if (proj.tech() != null) t.append(' ').append(String.join(" ", proj.tech()));
            if (proj.highlights() != null) t.append(' ').append(String.join(" ", proj.highlights()));
            items.add(new ProfileItem("프로젝트", t.toString().trim()));
        }
        for (var c : p.careers()) {
            StringBuilder t = new StringBuilder(nz(c.company())).append(' ').append(nz(c.role()));
            if (c.highlights() != null) t.append(' ').append(String.join(" ", c.highlights()));
            items.add(new ProfileItem("경력", t.toString().trim()));
        }
        return items;
    }

    /** 요구 스킬을 포함하는 프로필 항목을 찾는다(대소문자 무시, 부분일치 양방향). */
    private ProfileItem findEvidence(String skill, List<ProfileItem> items) {
        String s = skill.toLowerCase().trim();
        if (s.isEmpty()) return null;
        for (ProfileItem item : items) {
            String text = item.text().toLowerCase();
            if (text.contains(s)) return item;
            // 짧은 토큰(<=5자)은 역방향도 허용 (예: 'sql' ↔ 'mysql')
            if (s.length() <= 5 && containsToken(text, s)) return item;
        }
        return null;
    }

    private static boolean containsToken(String haystack, String token) {
        for (String w : haystack.split("[^a-z0-9가-힣]+")) {
            if (w.contains(token)) return true;
        }
        return false;
    }

    private static boolean looksLikeSkillToken(String s) {
        if (s == null) return false;
        String t = s.trim();
        // 공백 1개 이하 + 25자 이하 → 기술 토큰으로 간주 (문장형 우대사항 제외)
        return !t.isEmpty() && t.length() <= 25 && t.chars().filter(c -> c == ' ').count() <= 1;
    }

    private String buildSummary(JobPosting job, List<String> matched, List<String> missing,
                                List<MatchReport.Gap> gaps) {
        int required = dedup(job.requiredSkills()).size();
        StringBuilder sb = new StringBuilder();
        if (required > 0) {
            sb.append("요구 스킬 ").append(required).append("개 중 ").append(matched.size()).append("개 보유.");
        } else {
            sb.append("공고에 명시된 요구 스킬이 적어 보유 역량 중심으로 작성 권장.");
        }
        if (!matched.isEmpty()) {
            sb.append(" 강점: ").append(String.join(", ", matched.stream().limit(5).toList())).append('.');
        }
        List<String> mustGaps = gaps.stream().filter(g -> "must".equals(g.severity())).map(MatchReport.Gap::skill).limit(5).toList();
        if (!mustGaps.isEmpty()) {
            sb.append(" 보완 필요(필수): ").append(String.join(", ", mustGaps))
                    .append(" — 정직하게 학습 의지로 연결할 것.");
        }
        return sb.toString();
    }

    private static List<String> dedup(List<String> list) {
        Set<String> seen = new LinkedHashSet<>();
        if (list != null) for (String s : list) if (s != null && !s.isBlank()) seen.add(s.trim());
        return new ArrayList<>(seen);
    }

    private static String shorten(String s) {
        return s.length() <= 60 ? s : s.substring(0, 60) + "…";
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
