package com.campusflow.domain.progress.service;

import com.campusflow.domain.classroom.entity.ClassMember;
import com.campusflow.domain.classroom.entity.ClassRole;
import com.campusflow.domain.classroom.repository.ClassMemberRepository;
import com.campusflow.domain.kmate.repository.KmateHistoryRepository;
import com.campusflow.domain.material.repository.MaterialRepository;
import com.campusflow.domain.progress.dto.ProgressSummaryResponse;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 클래스룸 대시보드 요약. 여러 도메인 레포를 읽어 조립하는 read-model(신규 테이블 없음).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressService {

    private final UserRepository userRepository;
    private final ClassMemberRepository classMemberRepository;
    private final KmateHistoryRepository kmateHistoryRepository;
    private final MaterialRepository materialRepository;

    public ProgressSummaryResponse summary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        List<ClassMember> memberships = classMemberRepository.findByUserIdOrderByJoinedAtDesc(user.getId());
        long teaching = memberships.stream().filter(m -> m.getRole() != ClassRole.STUDENT).count();
        long joined = memberships.stream().filter(m -> m.getRole() == ClassRole.STUDENT).count();
        long materials = memberships.stream()
                .mapToLong(m -> materialRepository.countByClassRoomId(m.getClassRoom().getId()))
                .sum();
        long kmateQuestions = kmateHistoryRepository.countByUserId(user.getId());

        return new ProgressSummaryResponse(joined, teaching, kmateQuestions, materials);
    }
}
