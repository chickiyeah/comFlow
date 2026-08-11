package com.campusflow.domain.jobalert.service;

import com.campusflow.domain.career.dto.JobSearchResult;
import com.campusflow.domain.career.service.JobkoreaService;
import com.campusflow.domain.career.service.Work24ScraperService;
import com.campusflow.domain.jobalert.dto.JobAlertRequest;
import com.campusflow.domain.jobalert.dto.JobAlertResponse;
import com.campusflow.domain.jobalert.entity.JobAlert;
import com.campusflow.domain.jobalert.repository.JobAlertRepository;
import com.campusflow.domain.notification.entity.NotificationType;
import com.campusflow.domain.notification.service.NotificationService;
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
    private final NotificationService notificationService;

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

    /** 특정 학생의 알리미를 즉시 실행 → 인앱 알림 생성 (수동 새로고침용). 생성된 알림 수 반환. */
    @Transactional
    public int runForStudent(String username) {
        Student student = getStudent(username);
        List<JobAlert> alerts = jobAlertRepository.findByStudentId(student.getId());
        int created = 0;
        for (JobAlert alert : alerts) {
            try {
                created += notifyAlert(alert);
                alert.updateLastNotified();
            } catch (Exception e) {
                log.warn("채용 알리미 수동 실행 실패 alertId={}: {}", alert.getId(), e.getMessage());
            }
        }
        return created;
    }

    /** 알림 처리. 새 공고가 있으면 이메일 발송 + 인앱 알림 생성. 알림 생성 시 1 반환. */
    private int notifyAlert(JobAlert alert) {
        Student student = alert.getStudent();

        List<JobSearchResult> results = Stream.concat(
                jobkoreaService.searchJobs(alert.getKeyword(), 0, alert.getRegion(), null, alert.getEmpType()).stream(),
                work24Service.searchJobs(alert.getKeyword(), alert.getRegion(), null, alert.getEmpType(), 1).stream()
        ).limit(5).toList();

        if (results.isEmpty()) return 0;

        // 인앱 알림 생성 (이메일 발송 여부와 무관하게 항상)
        String firstUrl = results.get(0).url();
        notificationService.create(
                student,
                NotificationType.JOB_ALERT,
                "새 채용공고 — " + alert.getKeyword(),
                String.format("'%s' 관련 새 채용공고 %d건이 있습니다.", alert.getKeyword(), results.size()),
                (firstUrl != null && !firstUrl.isBlank()) ? firstUrl : "/career");

        String email = student.getEmail() != null
                ? student.getEmail()
                : (student.getUser() != null ? student.getUser().getEmail() : null);
        if (email == null || email.isBlank()) return 1;  // 알림은 생성됨

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
        return 1;
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND)).getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
