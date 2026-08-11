package com.campusflow.domain.notice.service;

import com.campusflow.domain.notice.dto.NoticeRequest;
import com.campusflow.domain.notice.dto.NoticeResponse;
import com.campusflow.domain.notice.entity.Notice;
import com.campusflow.domain.notice.repository.NoticeRepository;
import com.campusflow.domain.notification.entity.NotificationType;
import com.campusflow.domain.notification.service.NotificationService;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.entity.Role;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;

    public List<NoticeResponse> getAll() {
        return noticeRepository.findAllByOrderByImportantDescCreatedAtDesc()
                .stream().map(NoticeResponse::from).toList();
    }

    public List<NoticeResponse> getRecent(int limit) {
        return noticeRepository.findAllByOrderByImportantDescCreatedAtDesc(PageRequest.of(0, limit))
                .stream().map(NoticeResponse::from).toList();
    }

    @Transactional
    public NoticeResponse create(String username, NoticeRequest req) {
        assertStaff(username);
        Notice notice = noticeRepository.save(
                new Notice(req.title(), req.summary(), req.content(), req.important()));

        // 전체 학생에게 인앱 알림 + 웹푸시 broadcast (best-effort)
        String body = (req.summary() != null && !req.summary().isBlank()) ? req.summary() : req.title();
        String prefix = req.important() ? "[중요] 새 공지: " : "새 공지: ";
        int sent = 0;
        for (Student s : studentRepository.findAll()) {
            try {
                notificationService.create(s, NotificationType.NOTICE, prefix + req.title(), body, "/notices");
                sent++;
            } catch (Exception e) {
                log.warn("[공지 broadcast] 학생 {} 알림 실패: {}", s.getId(), e.getMessage());
            }
        }
        log.info("[공지 broadcast] '{}' — {}명에게 발송", req.title(), sent);
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void delete(String username, Long id) {
        assertStaff(username);
        noticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        noticeRepository.deleteById(id);
    }

    /** 교직원(관리자 또는 교수자)만 공지 작성/삭제 가능 */
    private void assertStaff(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (user.getRole() != Role.ROLE_ADMIN && user.getRole() != Role.ROLE_PROFESSOR) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
