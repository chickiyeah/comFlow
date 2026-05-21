package com.campusflow.domain.study.service;

import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.study.dto.StudyGroupRequest;
import com.campusflow.domain.study.dto.StudyGroupResponse;
import com.campusflow.domain.study.entity.StudyGroup;
import com.campusflow.domain.study.entity.StudyMember;
import com.campusflow.domain.study.entity.StudyStatus;
import com.campusflow.domain.study.repository.StudyGroupRepository;
import com.campusflow.domain.study.repository.StudyMemberRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyService {

    private final StudyGroupRepository  groupRepo;
    private final StudyMemberRepository memberRepo;
    private final UserRepository        userRepo;
    private final StudentRepository     studentRepo;

    public List<StudyGroupResponse> search(String subject, String username) {
        Long myId = getStudent(username).getId();
        List<StudyGroup> groups = subject != null && !subject.isBlank()
                ? groupRepo.findBySubjectContainingIgnoreCaseOrderByCreatedAtDesc(subject)
                : groupRepo.findAllByOrderByCreatedAtDesc();
        return groups.stream().map(g -> toResponse(g, myId)).toList();
    }

    public List<StudyGroupResponse> getMyGroups(String username) {
        Long myId = getStudent(username).getId();
        return memberRepo.findByStudentId(myId).stream()
                .map(m -> toResponse(m.getGroup(), myId)).toList();
    }

    @Transactional
    public StudyGroupResponse create(String username, StudyGroupRequest req) {
        Student student = getStudent(username);
        StudyGroup group = new StudyGroup(req.name(), req.subject(), req.description(), req.maxMembers(), student);
        group = groupRepo.save(group);
        memberRepo.save(new StudyMember(group, student));
        return toResponse(group, student.getId());
    }

    @Transactional
    public StudyGroupResponse join(String username, Long groupId) {
        Student student = getStudent(username);
        StudyGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_GROUP_NOT_FOUND));

        if (memberRepo.existsByGroupIdAndStudentId(groupId, student.getId()))
            throw new BusinessException(ErrorCode.ALREADY_JOINED);

        int current = memberRepo.countByGroupId(groupId);
        if (current >= group.getMaxMembers()) throw new BusinessException(ErrorCode.STUDY_GROUP_FULL);

        memberRepo.save(new StudyMember(group, student));
        if (current + 1 >= group.getMaxMembers()) group.markFull();

        return toResponse(group, student.getId());
    }

    @Transactional
    public void leave(String username, Long groupId) {
        Student student = getStudent(username);
        StudyMember member = memberRepo.findByGroupIdAndStudentId(groupId, student.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_GROUP_MEMBER));

        StudyGroup group = member.getGroup();
        if (group.getLeader().getId().equals(student.getId()))
            throw new BusinessException(ErrorCode.FORBIDDEN); // 방장은 탈퇴 불가

        memberRepo.delete(member);
        if (group.getStatus() == StudyStatus.FULL) group.reopen();
    }

    private StudyGroupResponse toResponse(StudyGroup g, Long myStudentId) {
        int count = memberRepo.countByGroupId(g.getId());
        boolean isMember = memberRepo.existsByGroupIdAndStudentId(g.getId(), myStudentId);
        return StudyGroupResponse.from(g, count, myStudentId).withMembership(isMember);
    }

    private Student getStudent(String username) {
        Long userId = userRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND)).getId();
        return studentRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
