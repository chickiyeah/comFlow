package com.campusflow.domain.jobalert.service;

import com.campusflow.domain.career.dto.JobSearchResult;
import com.campusflow.domain.career.service.JobkoreaService;
import com.campusflow.domain.career.service.Work24ScraperService;
import com.campusflow.domain.jobalert.dto.JobAlertRequest;
import com.campusflow.domain.jobalert.dto.JobAlertResponse;
import com.campusflow.domain.jobalert.entity.JobAlert;
import com.campusflow.domain.jobalert.repository.JobAlertRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.campusflow.global.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobAlertService {

    private final JobAlertRepository  jobAlertRepository;
    private final UserRepository      userRepository;
    private final StudentRepository   studentRepository;
    private final MailService         mailService;
    private final JobkoreaService     jobkoreaService;
    private final Work24ScraperService work24Service;

    public List<JobAlertResponse> getMyAlerts(String username) {
        return jobAlertRepository.findByStudentId(getStudent(username).getId())
                .stream().map(JobAlertResponse::from).toList();
    }

    @Transactional
    public JobAlertResponse create(String username, JobAlertRequest req) {
        Student student = getStudent(username);
        JobAlert alert = new JobAlert(student, req.keyword(), req.region(), req.empType());
        return JobAlertResponse.from(jobAlertRepository.save(alert));
    }

    @Transactional
    public void delete(String username, Long id) {
        JobAlert alert = jobAlertRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_ALERT_NOT_FOUND));
        if (!alert.getStudent().getId().equals(getStudent(username).getId()))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        jobAlertRepository.delete(alert);
    }

    @Transactional
    public void runDailyAlert() {
        List<JobAlert> alerts = jobAlertRepository.findAll();
        for (JobAlert alert : alerts) {
            try {
                notifyAlert(alert);
                alert.updateLastNotified();
            } catch (Exception e) {
                log.warn("채용 알리미 발송 실패 alertId={}: {}", alert.getId(), e.getMessage());
            }
        }
        log.info("[채용알리미] {} 건 처리 완료", alerts.size());
    }

    private void notifyAlert(JobAlert alert) {
        Student student = alert.getStudent();
        String email = student.getEmail() != null
                ? student.getEmail()
                : (student.getUser() != null ? student.getUser().getEmail() : null);
        if (email == null || email.isBlank()) return;

        List<JobSearchResult> results = Stream.concat(
                jobkoreaService.searchJobs(alert.getKeyword(), 0, alert.getRegion(), null, alert.getEmpType()).stream(),
                work24Service.searchJobs(alert.getKeyword(), alert.getRegion(), null, alert.getEmpType(), 1).stream()
        ).limit(5).toList();

        if (results.isEmpty()) return;

        StringBuilder items = new StringBuilder();
        for (JobSearchResult r : results) {
            items.append("""
                    <li style='margin:8px 0'>
                      <a href='%s' style='color:#00236f;font-weight:bold'>%s</a>
                      <span style='color:#888'> — %s</span>
                      %s
                    </li>
                    """.formatted(
                    r.url() != null ? r.url() : "#",
                    r.title(),
                    r.company(),
                    r.deadline() != null ? " <span style='color:#dc2626'>마감 " + r.deadline() + "</span>" : ""
            ));
        }

        String html = """
                <div style="font-family:sans-serif;max-width:600px;margin:auto;padding:24px">
                  <h2 style="color:#00236f">💼 새 채용공고 알림 — CampusFlow</h2>
                  <p>안녕하세요, <strong>%s</strong> 학생!</p>
                  <p><strong>%s</strong> 관련 새 채용공고 %d건이 도착했습니다.</p>
                  <ul style="padding-left:20px">%s</ul>
                  <p style="color:#888;font-size:12px">CampusFlow 채용 알리미 자동 발송</p>
                </div>
                """.formatted(student.getName(), alert.getKeyword(), results.size(), items);

        mailService.send(email, "새 채용공고 — " + alert.getKeyword(), html);
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND)).getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
