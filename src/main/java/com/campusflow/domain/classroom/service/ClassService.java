package com.campusflow.domain.classroom.service;

import com.campusflow.domain.classroom.dto.ClassCreateRequest;
import com.campusflow.domain.classroom.dto.ClassMemberResponse;
import com.campusflow.domain.classroom.dto.ClassResponse;
import com.campusflow.domain.classroom.dto.InviteRequest;
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

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 혼동 문자(I,O,0,1) 제외
    private static final int CODE_LENGTH = 6;
    private static final int CODE_MAX_ATTEMPTS = 10;

    private final ClassRoomRepository classRoomRepository;
    private final ClassMemberRepository classMemberRepository;
    private final UserRepository userRepository;
    private final ClassAccessService classAccess;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public ClassResponse create(String username, ClassCreateRequest request) {
        User owner = classAccess.currentUser(username);
        ClassRoom classRoom = ClassRoom.builder()
                .code(generateUniqueCode())
                .name(request.name())
                .subject(request.subject())
                .description(request.description())
                .ownerUser(owner)
                .build();
        classRoomRepository.save(classRoom);
        classMemberRepository.save(ClassMember.builder()
                .classRoom(classRoom)
                .user(owner)
                .role(ClassRole.OWNER)
                .build());
        return ClassResponse.from(classRoom, ClassRole.OWNER, 1);
    }

    public List<ClassResponse> getMyClasses(String username) {
        User user = classAccess.currentUser(username);
        return classMemberRepository.findByUserIdOrderByJoinedAtDesc(user.getId()).stream()
                .map(m -> ClassResponse.from(
                        m.getClassRoom(),
                        m.getRole(),
                        classMemberRepository.countByClassRoomId(m.getClassRoom().getId())))
                .toList();
    }

    public ClassResponse get(String username, Long classId) {
        ClassMember member = classAccess.requireMember(classId, username);
        return ClassResponse.from(member.getClassRoom(), member.getRole(),
                classMemberRepository.countByClassRoomId(classId));
    }

    @Transactional
    public ClassResponse join(String username, String code) {
        User user = classAccess.currentUser(username);
        ClassRoom classRoom = classRoomRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CLASS_CODE));
        if (classMemberRepository.existsByClassRoomIdAndUserId(classRoom.getId(), user.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_CLASS_MEMBER);
        }
        classMemberRepository.save(ClassMember.builder()
                .classRoom(classRoom)
                .user(user)
                .role(ClassRole.STUDENT)
                .build());
        return ClassResponse.from(classRoom, ClassRole.STUDENT,
                classMemberRepository.countByClassRoomId(classRoom.getId()));
    }

    public List<ClassMemberResponse> members(String username, Long classId) {
        classAccess.requireMember(classId, username);
        return classMemberRepository.findByClassRoomIdOrderByJoinedAtAsc(classId).stream()
                .map(ClassMemberResponse::from)
                .toList();
    }

    @Transactional
    public ClassMemberResponse invite(String username, Long classId, InviteRequest request) {
        classAccess.requireTeacher(classId, username);
        ClassRoom classRoom = classAccess.requireClass(classId);
        User invitee = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (classMemberRepository.existsByClassRoomIdAndUserId(classId, invitee.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_CLASS_MEMBER);
        }
        ClassRole role = request.role() == null || request.role() == ClassRole.OWNER
                ? ClassRole.STUDENT : request.role();
        ClassMember member = classMemberRepository.save(ClassMember.builder()
                .classRoom(classRoom)
                .user(invitee)
                .role(role)
                .build());
        return ClassMemberResponse.from(member);
    }

    @Transactional
    public void removeMember(String username, Long classId, Long userId) {
        classAccess.requireTeacher(classId, username);
        ClassMember member = classMemberRepository.findByClassRoomIdAndUserId(classId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_ACCESS_DENIED));
        if (member.getRole() == ClassRole.OWNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN); // 개설자는 제거 불가
        }
        classMemberRepository.delete(member);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < CODE_MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!classRoomRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("클래스 코드 생성에 실패했습니다.");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
