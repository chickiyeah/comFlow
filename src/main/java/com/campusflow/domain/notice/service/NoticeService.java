package com.campusflow.domain.notice.service;

import com.campusflow.domain.notice.dto.NoticeRequest;
import com.campusflow.domain.notice.dto.NoticeResponse;
import com.campusflow.domain.notice.entity.Notice;
import com.campusflow.domain.notice.repository.NoticeRepository;
import com.campusflow.domain.user.entity.Role;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;

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
        assertAdmin(username);
        return NoticeResponse.from(noticeRepository.save(
                new Notice(req.title(), req.summary(), req.content(), req.important())
        ));
    }

    @Transactional
    public void delete(String username, Long id) {
        assertAdmin(username);
        noticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        noticeRepository.deleteById(id);
    }

    private void assertAdmin(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (user.getRole() != Role.ROLE_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
