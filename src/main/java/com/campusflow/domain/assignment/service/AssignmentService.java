package com.campusflow.domain.assignment.service;

import com.campusflow.domain.assignment.dto.*;
import com.campusflow.domain.assignment.entity.Assignment;
import com.campusflow.domain.assignment.entity.AssignmentFile;
import com.campusflow.domain.assignment.entity.Submission;
import com.campusflow.domain.assignment.entity.SubmissionStatus;
import com.campusflow.domain.assignment.repository.AssignmentCommentRepository;
import com.campusflow.domain.assignment.repository.AssignmentFileRepository;
import com.campusflow.domain.assignment.repository.AssignmentRepository;
import com.campusflow.domain.assignment.repository.SubmissionRepository;
import com.campusflow.domain.classroom.entity.ClassMember;
import com.campusflow.domain.classroom.entity.ClassRole;
import com.campusflow.domain.classroom.entity.ClassRoom;
import com.campusflow.domain.classroom.repository.ClassMemberRepository;
import com.campusflow.domain.classroom.service.ClassAccessService;
import com.campusflow.domain.storage.entity.StoredFile;
import com.campusflow.domain.storage.service.FileAccessTokenService;
import com.campusflow.domain.storage.service.FileStorageService;
import com.campusflow.domain.user.entity.User;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

    private static final int DEFAULT_POINTS = 100;

    private final AssignmentRepository assignmentRepository;
    private final AssignmentFileRepository assignmentFileRepository;
    private final AssignmentCommentRepository assignmentCommentRepository;
    private final SubmissionRepository submissionRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassAccessService classAccess;
    private final FileStorageService fileStorageService;
    private final FileAccessTokenService fileAccessTokenService;

    // ── 목록/상세 ────────────────────────────────────────────
    public List<AssignmentResponse> list(String username, Long classId) {
        ClassMember member = classAccess.requireMember(classId, username);
        boolean teacher = member.getRole() != ClassRole.STUDENT;
        Long userId = member.getUser().getId();
        List<Assignment> assignments = teacher
                ? assignmentRepository.findByClassRoomIdOrderByCreatedAtDesc(classId)
                : assignmentRepository.findByClassRoomIdAndDraftFalseOrderByCreatedAtDesc(classId);
        return assignments.stream()
                .map(a -> AssignmentResponse.from(a, myStatus(a.getId(), userId)))
                .toList();
    }

    public AssignmentDetailResponse getDetail(String username, Long assignmentId) {
        Assignment assignment = loadAssignment(assignmentId);
        ClassMember member = classAccess.requireMember(assignment.getClassRoom().getId(), username);
        boolean teacher = member.getRole() != ClassRole.STUDENT;
        User user = member.getUser();
        if (assignment.isDraft() && !teacher) {
            throw new BusinessException(ErrorCode.NOT_FOUND); // 학생에게 draft 미노출
        }
        List<AssignmentFileResponse> files = assignmentFileRepository.findByAssignmentId(assignmentId).stream()
                .map(f -> AssignmentFileResponse.from(f, mintStreamUrl(f.getStoredFile(), user.getId())))
                .toList();
        SubmissionResponse mySubmission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, user.getId())
                .map(s -> SubmissionResponse.from(s, mintStreamUrl(s.getStoredFile(), user.getId())))
                .orElse(null);
        SubmissionStatsResponse stats = teacher ? buildStats(assignment) : null;
        return AssignmentDetailResponse.of(assignment, teacher, files, mySubmission, stats);
    }

    // ── 교사: 생성/수정/삭제 ─────────────────────────────────
    @Transactional
    public AssignmentResponse create(String username, Long classId, AssignmentCreateRequest request) {
        User teacher = classAccess.requireTeacher(classId, username).getUser();
        ClassRoom classRoom = classAccess.requireClass(classId);
        Assignment assignment = Assignment.builder()
                .classRoom(classRoom)
                .title(request.title())
                .instructions(request.instructions())
                .dueDate(request.dueDate())
                .points(request.points() != null ? Math.max(0, request.points()) : DEFAULT_POINTS)
                .draft(request.draft() != null && request.draft())
                .topic(request.topic())
                .createdBy(teacher)
                .build();
        return AssignmentResponse.from(assignmentRepository.save(assignment), null);
    }

    @Transactional
    public AssignmentResponse update(String username, Long assignmentId, AssignmentUpdateRequest request) {
        Assignment assignment = loadAssignment(assignmentId);
        classAccess.requireTeacher(assignment.getClassRoom().getId(), username);
        assignment.update(request.title(), request.instructions(), request.dueDate(),
                request.points() != null ? Math.max(0, request.points()) : assignment.getPoints());
        return AssignmentResponse.from(assignment, null);
    }

    @Transactional
    public AssignmentResponse updateDraft(String username, Long assignmentId, boolean draft) {
        Assignment assignment = loadAssignment(assignmentId);
        classAccess.requireTeacher(assignment.getClassRoom().getId(), username);
        assignment.updateDraft(draft);
        return AssignmentResponse.from(assignment, null);
    }

    @Transactional
    public AssignmentResponse updateTopic(String username, Long assignmentId, String topic) {
        Assignment assignment = loadAssignment(assignmentId);
        classAccess.requireTeacher(assignment.getClassRoom().getId(), username);
        assignment.updateTopic(topic);
        return AssignmentResponse.from(assignment, null);
    }

    @Transactional
    public void delete(String username, Long assignmentId) {
        Assignment assignment = loadAssignment(assignmentId);
        classAccess.requireTeacher(assignment.getClassRoom().getId(), username);
        // 첨부/제출 파일 삭제 + 의존 행 정리
        List<StoredFile> toDelete = new java.util.ArrayList<>();
        assignmentFileRepository.findByAssignmentId(assignmentId)
                .forEach(f -> toDelete.add(f.getStoredFile()));
        submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId).stream()
                .map(Submission::getStoredFile).filter(java.util.Objects::nonNull).forEach(toDelete::add);
        assignmentCommentRepository.deleteByAssignmentId(assignmentId);
        assignmentFileRepository.deleteByAssignmentId(assignmentId);
        submissionRepository.deleteByAssignmentId(assignmentId);
        assignmentRepository.delete(assignment);
        toDelete.forEach(fileStorageService::delete);
    }

    @Transactional
    public AssignmentFileResponse attachFile(String username, Long assignmentId, MultipartFile file) {
        Assignment assignment = loadAssignment(assignmentId);
        User teacher = classAccess.requireTeacher(assignment.getClassRoom().getId(), username).getUser();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
        StoredFile stored = fileStorageService.store(file, teacher);
        AssignmentFile saved = assignmentFileRepository.save(AssignmentFile.builder()
                .assignment(assignment).storedFile(stored).build());
        return AssignmentFileResponse.from(saved, mintStreamUrl(stored, teacher.getId()));
    }

    // ── 학생: 제출 ───────────────────────────────────────────
    @Transactional
    public SubmissionResponse submit(String username, Long assignmentId, String content, MultipartFile file) {
        Assignment assignment = loadAssignment(assignmentId);
        User user = classAccess.requireMember(assignment.getClassRoom().getId(), username).getUser();
        if (assignment.isDraft()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        SubmissionStatus status = assignment.getDueDate() != null
                && LocalDateTime.now().isAfter(assignment.getDueDate())
                ? SubmissionStatus.LATE : SubmissionStatus.TURNED_IN;
        StoredFile newFile = (file != null && !file.isEmpty()) ? fileStorageService.store(file, user) : null;

        Submission existing = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, user.getId())
                .orElse(null);
        if (existing != null) {
            StoredFile old = existing.getStoredFile();
            existing.resubmit(content, newFile, status, LocalDateTime.now());
            if (old != null) {
                fileStorageService.delete(old);
            }
            return SubmissionResponse.from(existing, mintStreamUrl(newFile, user.getId()));
        }
        Submission saved = submissionRepository.save(Submission.builder()
                .assignment(assignment).student(user).content(content)
                .storedFile(newFile).status(status).submittedAt(LocalDateTime.now()).build());
        return SubmissionResponse.from(saved, mintStreamUrl(newFile, user.getId()));
    }

    // ── 교사: 제출 목록/통계 ─────────────────────────────────
    public List<SubmissionResponse> submissions(String username, Long assignmentId) {
        Assignment assignment = loadAssignment(assignmentId);
        User teacher = classAccess.requireTeacher(assignment.getClassRoom().getId(), username).getUser();
        return submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId).stream()
                .map(s -> SubmissionResponse.from(s, mintStreamUrl(s.getStoredFile(), teacher.getId())))
                .toList();
    }

    public SubmissionStatsResponse stats(String username, Long assignmentId) {
        Assignment assignment = loadAssignment(assignmentId);
        classAccess.requireTeacher(assignment.getClassRoom().getId(), username);
        return buildStats(assignment);
    }

    // ── 교사: 채점/반려 (submission 스코프) ──────────────────
    @Transactional
    public SubmissionResponse grade(String username, Long submissionId, GradeRequest request) {
        Submission submission = loadSubmission(submissionId);
        User teacher = classAccess.requireTeacher(
                submission.getAssignment().getClassRoom().getId(), username).getUser();
        int max = submission.getAssignment().getPoints();
        int clamped = Math.max(0, Math.min(max, request.grade()));
        submission.grade(clamped, request.feedback(), LocalDateTime.now());
        return SubmissionResponse.from(submission, mintStreamUrl(submission.getStoredFile(), teacher.getId()));
    }

    @Transactional
    public SubmissionResponse returnSubmission(String username, Long submissionId) {
        Submission submission = loadSubmission(submissionId);
        User teacher = classAccess.requireTeacher(
                submission.getAssignment().getClassRoom().getId(), username).getUser();
        submission.markReturned();
        return SubmissionResponse.from(submission, mintStreamUrl(submission.getStoredFile(), teacher.getId()));
    }

    // ── helpers ──────────────────────────────────────────────
    private SubmissionStatsResponse buildStats(Assignment assignment) {
        Long classId = assignment.getClassRoom().getId();
        long totalStudents = classMemberRepository.countByClassRoomIdAndRole(classId, ClassRole.STUDENT);
        long submitted = submissionRepository.countByAssignmentId(assignment.getId());
        long graded = submissionRepository.countByAssignmentIdAndStatus(assignment.getId(), SubmissionStatus.GRADED);
        long returned = submissionRepository.countByAssignmentIdAndStatus(assignment.getId(), SubmissionStatus.RETURNED);
        return new SubmissionStatsResponse(totalStudents, submitted, graded, returned);
    }

    private String myStatus(Long assignmentId, Long userId) {
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, userId)
                .map(s -> s.getStatus().name()).orElse(null);
    }

    private String mintStreamUrl(StoredFile stored, Long userId) {
        if (stored == null) {
            return null;
        }
        String token = fileAccessTokenService.issue(stored.getId(), userId);
        return "/api/files/" + stored.getId() + "/stream?token=" + token;
    }

    private Assignment loadAssignment(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private Submission loadSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
