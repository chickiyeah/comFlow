package com.campusflow.domain.classroom.service;

import com.campusflow.domain.classroom.entity.ClassMember;
import com.campusflow.domain.classroom.entity.ClassRole;
import com.campusflow.domain.classroom.entity.ClassRoom;
import com.campusflow.domain.classroom.repository.ClassMemberRepository;
import com.campusflow.domain.classroom.repository.ClassRoomRepository;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클래스 스코프 권한/현재유저 해석 헬퍼. 모든 클래스 관련 서비스가 주입해서 쓴다.
 * (기존 학생 스코프 도메인의 getStudentByUsername 헬퍼에 대응하되, 클래스는 User 스코프이므로 User로 해석)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassAccessService {

    private final UserRepository userRepository;
    private final ClassRoomRepository classRoomRepository;
    private final ClassMemberRepository classMemberRepository;

    public User currentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    public ClassRoom requireClass(Long classId) {
        return classRoomRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));
    }

    public ClassMember requireMember(Long classId, String username) {
        return requireMember(classId, currentUser(username));
    }

    public ClassMember requireMember(Long classId, User user) {
        return classMemberRepository.findByClassRoomIdAndUserId(classId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_ACCESS_DENIED));
    }

    /** OWNER/TEACHER만 통과. STUDENT면 {@link ErrorCode#NOT_CLASS_TEACHER}. */
    public ClassMember requireTeacher(Long classId, String username) {
        ClassMember member = requireMember(classId, username);
        if (member.getRole() == ClassRole.STUDENT) {
            throw new BusinessException(ErrorCode.NOT_CLASS_TEACHER);
        }
        return member;
    }

    public boolean isMember(Long classId, Long userId) {
        return classMemberRepository.existsByClassRoomIdAndUserId(classId, userId);
    }

    public boolean isTeacher(Long classId, Long userId) {
        return classMemberRepository.findByClassRoomIdAndUserId(classId, userId)
                .map(m -> m.getRole() != ClassRole.STUDENT)
                .orElse(false);
    }
}
