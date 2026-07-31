package com.campusflow.domain.resume.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 리치 이력서 전 섹션. 사실 섹션(personal~awards)은 ResumeAssembler가 DB에서 조립하고,
 * coverLetter/meta는 ResumeAiGeneratorService가 채운다. Resume.resumeData(JSON)로 저장된다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResumeData(
        Personal personal,
        Education education,
        List<SkillGroup> skills,
        List<ProjectEntry> projects,
        List<CareerEntry> careers,
        List<CertEntry> certs,
        List<LanguageEntry> languages,
        List<AwardEntry> awards,
        List<CoverLetterSection> coverLetter,
        Meta meta
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Personal(String name, String studentId, String email, String phone) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Education(String department, int grade, int semester, Double gpa) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SkillGroup(String category, List<String> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectEntry(String title, String period, List<String> techStack,
                               String role, String problem, String solution, String result,
                               String githubUrl, String deployUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CareerEntry(String org, String period, String role, String type, String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CertEntry(String name, String org, String date) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LanguageEntry(String name, String score, String date) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AwardEntry(String title, String org, String level, String date) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CoverLetterSection(String question, String body, int charCount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(String template, String generatedAt, HonestyReport honestyReport) {}
}
