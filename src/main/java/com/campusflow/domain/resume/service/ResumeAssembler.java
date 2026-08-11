package com.campusflow.domain.resume.service;

import com.campusflow.domain.award.entity.Award;
import com.campusflow.domain.award.repository.AwardRepository;
import com.campusflow.domain.career.entity.ActivityStatus;
import com.campusflow.domain.career.entity.ActivityType;
import com.campusflow.domain.career.entity.CareerActivity;
import com.campusflow.domain.career.repository.CareerActivityRepository;
import com.campusflow.domain.grade.repository.GradeRepository;
import com.campusflow.domain.portfolio.entity.Portfolio;
import com.campusflow.domain.portfolio.repository.PortfolioRepository;
import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeData.*;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 학생의 실제 데이터를 이력서의 '사실 섹션'({@link ResumeData})으로 조립한다. AI를 쓰지 않는다.
 * jobpilot ProfileAssembler를 변경하지 않기 위해 같은 리포지토리를 재사용하는 별도 조립기.
 * coverLetter/meta는 ResumeAiGeneratorService가 채운다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeAssembler {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PortfolioRepository portfolioRepository;
    private final CareerActivityRepository careerActivityRepository;
    private final AwardRepository awardRepository;
    private final GradeRepository gradeRepository;

    public ResumeData assemble(String username) {
        Student student = getStudent(username);
        Long sid = student.getId();

        Personal personal = new Personal(
                student.getName(), student.getStudentId(), student.getEmail(), student.getPhone());

        Double gpa = gradeRepository.calculateGpa(sid);
        Education education = new Education(
                student.getDepartment(), student.getGrade(), student.getSemester(), gpa);

        List<Portfolio> portfolios = portfolioRepository.findByStudentIdOrderByStartDateDesc(sid);

        Set<String> skillSet = new LinkedHashSet<>();
        portfolios.forEach(p -> skillSet.addAll(splitTech(p.getTechStack())));
        List<SkillGroup> skills = skillSet.isEmpty()
                ? List.of()
                : List.of(new SkillGroup("보유 기술", new ArrayList<>(skillSet)));

        List<ProjectEntry> projects = portfolios.stream()
                .map(p -> new ProjectEntry(
                        p.getTitle(), period(p.getStartDate(), p.getEndDate()),
                        splitTech(p.getTechStack()), p.getRole(),
                        null, null, p.getDescription(),   // problem/solution은 AI가 서사화, result엔 설명 시드
                        p.getGithubUrl(), p.getDeployUrl()))
                .toList();

        List<CareerEntry> careers = careerActivityRepository
                .findByStudentIdAndTypeOrderByCreatedAtDesc(sid, ActivityType.INTERNSHIP).stream()
                .map(a -> new CareerEntry(
                        nz(a.getOrganization()), dateStr(a.getCompletedDate()),
                        a.getTitle(), "경력", nz(a.getMemo())))
                .toList();

        List<CertEntry> certs = new ArrayList<>();
        for (CareerActivity a : careerActivityRepository
                .findByStudentIdAndTypeOrderByCreatedAtDesc(sid, ActivityType.CERTIFICATE)) {
            if (a.getStatus() == ActivityStatus.COMPLETED) {
                certs.add(new CertEntry(a.getTitle(), nz(a.getOrganization()), dateStr(a.getCompletedDate())));
            }
        }

        List<LanguageEntry> languages = new ArrayList<>();
        for (CareerActivity a : careerActivityRepository
                .findByStudentIdAndTypeOrderByCreatedAtDesc(sid, ActivityType.LANGUAGE_TEST)) {
            if (a.getStatus() == ActivityStatus.COMPLETED) {
                languages.add(new LanguageEntry(a.getTitle(), nz(a.getScore()), dateStr(a.getCompletedDate())));
            }
        }

        List<AwardEntry> awards = awardRepository.findByStudentIdOrderByAwardDateDesc(sid).stream()
                .map(a -> new AwardEntry(
                        a.getTitle(), nz(a.getOrganization()),
                        a.getLevel() == null ? "" : a.getLevel().getLabel(), dateStr(a.getAwardDate())))
                .toList();

        String targetJob = student.getDesiredJob() == null ? "" : student.getDesiredJob();

        return new ResumeData(personal, education, targetJob, skills, projects, careers,
                certs, languages, awards, List.of(), null);
    }

    /** 정직성 대조·프롬프트용 근거 텍스트. 여기 없는 사실은 자소서에 쓰면 안 된다. */
    public String buildEvidence(ResumeData d) {
        StringBuilder sb = new StringBuilder();
        if (d.targetJob() != null && !d.targetJob().isBlank()) {
            sb.append("[희망직무] ").append(d.targetJob()).append('\n');
        }
        if (d.education() != null) {
            sb.append("[학력] ").append(nz(d.education().department()))
              .append(' ').append(d.education().grade()).append("학년 ")
              .append(d.education().semester()).append("학기");
            if (d.education().gpa() != null) sb.append(" (GPA ").append(d.education().gpa()).append(')');
            sb.append('\n');
        }
        if (!d.skills().isEmpty()) {
            sb.append("[스킬] ");
            for (SkillGroup g : d.skills()) sb.append(String.join(", ", g.items())).append(' ');
            sb.append('\n');
        }
        if (!d.certs().isEmpty()) {
            sb.append("[자격증] ");
            sb.append(String.join(", ", d.certs().stream().map(CertEntry::name).toList())).append('\n');
        }
        if (!d.languages().isEmpty()) {
            sb.append("[어학] ");
            sb.append(String.join(", ", d.languages().stream()
                    .map(l -> l.name() + " " + nz(l.score())).toList())).append('\n');
        }
        if (!d.careers().isEmpty()) {
            sb.append("[경력]\n");
            for (CareerEntry c : d.careers())
                sb.append("  - ").append(c.org()).append(" / ").append(c.role())
                  .append(" / ").append(nz(c.period())).append('\n');
        }
        if (!d.projects().isEmpty()) {
            sb.append("[프로젝트]\n");
            for (ProjectEntry p : d.projects()) {
                sb.append("  - ").append(p.title()).append(": ").append(nz(p.result())).append('\n');
                if (p.techStack() != null && !p.techStack().isEmpty())
                    sb.append("      tech: ").append(String.join(", ", p.techStack())).append('\n');
            }
        }
        if (!d.awards().isEmpty()) {
            sb.append("[수상] ");
            sb.append(String.join(", ", d.awards().stream()
                    .map(a -> a.title() + "(" + a.level() + ")").toList())).append('\n');
        }
        return sb.toString().trim();
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }

    private static List<String> splitTech(String techStack) {
        if (techStack == null || techStack.isBlank()) return List.of();
        return Arrays.stream(techStack.split("[,/]"))
                .map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private static String period(LocalDate s, LocalDate e) {
        if (s == null && e == null) return "";
        return (s == null ? "" : s.toString()) + " ~ " + (e == null ? "" : e.toString());
    }

    private static String dateStr(LocalDate d) { return d == null ? "" : d.toString(); }

    private static String nz(String s) { return s == null ? "" : s; }
}
