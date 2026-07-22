package com.campusflow.domain.assignment.service;

import com.campusflow.domain.assignment.dto.CommentRequest;
import com.campusflow.domain.assignment.dto.CommentResponse;
import com.campusflow.domain.assignment.entity.Assignment;
import com.campusflow.domain.assignment.entity.AssignmentComment;
import com.campusflow.domain.assignment.repository.AssignmentCommentRepository;
import com.campusflow.domain.assignment.repository.AssignmentRepository;
import com.campusflow.domain.classroom.entity.ClassMember;
import com.campusflow.domain.classroom.entity.ClassRole;
import com.campusflow.domain.classroom.service.ClassAccessService;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 과제 비공개 코멘트(교사↔학생 1:1 스레드). 스레드 키는 (assignment, student).
 * 교사는 studentId를 지정해 특정 학생 스레드에, 학생은 항상 본인 스레드에 접근한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentCommentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentCommentRepository commentRepository;
    private final ClassAccessService classAccess;
    private final UserRepository userRepository;

    public List<CommentResponse> thread(String username, Long assignmentId, Long studentIdParam) {
        Assignment assignment = loadAssignment(assignmentId);
        ClassMember member = classAccess.requireMember(assignment.getClassRoom().getId(), username);
        Long studentId = resolveStudentId(member, studentIdParam);
        return commentRepository.findByAssignmentIdAndStudentIdOrderByCreatedAtAsc(assignmentId, studentId)
                .stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse add(String username, Long assignmentId, CommentRequest request) {
        Assignment assignment = loadAssignment(assignmentId);
        ClassMember member = classAccess.requireMember(assignment.getClassRoom().getId(), username);
        Long studentId = resolveStudentId(member, request.studentId());
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!classAccess.isMember(assignment.getClassRoom().getId(), studentId)) {
            throw new BusinessException(ErrorCode.CLASS_ACCESS_DENIED);
        }
        AssignmentComment comment = commentRepository.save(AssignmentComment.builder()
                .assignment(assignment)
                .student(student)
                .author(member.getUser())
                .body(request.body())
                .build());
        return CommentResponse.from(comment);
    }

    /** 교사면 studentIdParam 필수(그 학생 스레드), 학생이면 본인 id로 고정. */
    private Long resolveStudentId(ClassMember member, Long studentIdParam) {
        boolean teacher = member.getRole() != ClassRole.STUDENT;
        if (teacher) {
            if (studentIdParam == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            return studentIdParam;
        }
        return member.getUser().getId();
    }

    private Assignment loadAssignment(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
