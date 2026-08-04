package com.campusflow.domain.notification.service;

import com.campusflow.domain.notification.dto.NotificationResponse;
import com.campusflow.domain.notification.entity.Notification;
import com.campusflow.domain.notification.entity.NotificationType;
import com.campusflow.domain.notification.entity.NotificationPref;
import com.campusflow.domain.notification.repository.NotificationPrefRepository;
import com.campusflow.domain.notification.repository.NotificationRepository;
import com.campusflow.domain.push.service.WebPushService;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int RECENT_LIMIT = 50;

    private final NotificationRepository notificationRepository;
    private final NotificationPrefRepository prefRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final WebPushService webPushService;

    /** 알림 생성 — 다른 도메인(채용 알리미·조기경보 등)에서 호출. 수신 거부 시 null 반환. */
    @Transactional
    public Notification create(Student student, NotificationType type, String title, String body, String link) {
        // 수신 설정 확인 — 채용/공지(강좌)는 끌 수 있음. 학사 경보·시스템은 항상 수신.
        Long userId = student.getUser() != null ? student.getUser().getId() : null;
        if (userId != null && !isAllowed(userId, type)) return null;

        Notification saved = notificationRepository.save(Notification.builder()
                .student(student)
                .type(type)
                .title(title)
                .body(body)
                .link(link)
                .build());
        // 브라우저 푸시도 발송 (비동기, best-effort)
        try { webPushService.sendToUser(student.getUser(), title, body, link); }
        catch (Exception ignored) { /* 푸시 실패는 인앱 알림에 영향 없음 */ }
        return saved;
    }

    public List<NotificationResponse> list(String username) {
        Long sid = getStudent(username).getId();
        return notificationRepository
                .findByStudentIdOrderByCreatedAtDesc(sid, PageRequest.of(0, RECENT_LIMIT))
                .stream().map(NotificationResponse::from).toList();
    }

    public long unreadCount(String username) {
        return notificationRepository.countByStudentIdAndReadFlagFalse(getStudent(username).getId());
    }

    @Transactional
    public void markRead(String username, Long id) {
        Long sid = getStudent(username).getId();
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!n.getStudent().getId().equals(sid)) throw new BusinessException(ErrorCode.FORBIDDEN);
        n.markRead();
    }

    @Transactional
    public void markAllRead(String username) {
        notificationRepository.markAllRead(getStudent(username).getId());
    }

    // ── 수신 설정 ────────────────────────────────────────────
    private boolean isAllowed(Long userId, NotificationType type) {
        if (type != NotificationType.JOB_ALERT && type != NotificationType.NOTICE) return true; // 경보·시스템 항상
        NotificationPref pref = prefRepository.findByUserId(userId).orElse(null);
        if (pref == null) return true; // 기본 수신
        return type == NotificationType.JOB_ALERT ? pref.isRecvJobAlert() : pref.isRecvNotice();
    }

    public Map<String, Boolean> getPref(String username) {
        Long userId = getUserId(username);
        NotificationPref p = prefRepository.findByUserId(userId).orElse(null);
        return Map.of(
                "jobAlert", p == null || p.isRecvJobAlert(),
                "notice",   p == null || p.isRecvNotice());
    }

    @Transactional
    public Map<String, Boolean> updatePref(String username, boolean jobAlert, boolean notice) {
        Long userId = getUserId(username);
        NotificationPref p = prefRepository.findByUserId(userId)
                .orElseGet(() -> new NotificationPref(userId));
        p.update(jobAlert, notice);
        prefRepository.save(p);
        return Map.of("jobAlert", jobAlert, "notice", notice);
    }

    private Long getUserId(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return u.getId();
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND)).getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
