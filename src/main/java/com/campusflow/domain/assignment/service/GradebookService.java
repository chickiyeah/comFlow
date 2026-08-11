package com.campusflow.domain.assignment.service;

import com.campusflow.domain.assignment.dto.GradebookResponse;
import com.campusflow.domain.assignment.entity.Assignment;
import com.campusflow.domain.assignment.entity.Submission;
import com.campusflow.domain.assignment.repository.AssignmentRepository;
import com.campusflow.domain.assignment.repository.SubmissionRepository;
import com.campusflow.domain.classroom.entity.ClassMember;
import com.campusflow.domain.classroom.entity.ClassRole;
import com.campusflow.domain.classroom.repository.ClassMemberRepository;
import com.campusflow.domain.classroom.service.ClassAccessService;
import com.campusflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 성적부(read-model). 신규 테이블 없이 assignments + submissions로 조립한다.
 * 교사: 전체 학생 × 과제 격자 + 과제별 평균. 학생: 본인 행만(averages=null).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradebookService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassAccessService classAccess;

    public GradebookResponse gradebook(String username, Long classId) {
        ClassMember member = classAccess.requireMember(classId, username);
        boolean teacher = member.getRole() != ClassRole.STUDENT;

        // 열: 공개된 과제 (오래된→최신 순으로 안정적 정렬)
        List<Assignment> assignments = assignmentRepository
                .findByClassRoomIdAndDraftFalseOrderByCreatedAtDesc(classId).stream()
                .sorted(Comparator.comparing(Assignment::getCreatedAt))
                .toList();
        List<GradebookResponse.Column> columns = assignments.stream()
                .map(a -> new GradebookResponse.Column(a.getId(), a.getTitle(), a.getPoints()))
                .toList();

        if (teacher) {
            return teacherView(classId, assignments, columns);
        }
        return studentView(member.getUser(), classId, assignments, columns);
    }

    private GradebookResponse teacherView(Long classId, List<Assignment> assignments,
                                          List<GradebookResponse.Column> columns) {
        // (assignmentId, studentId) → submission
        Map<Long, Map<Long, Submission>> byAssignment = submissionRepository
                .findByAssignment_ClassRoomId(classId).stream()
                .collect(Collectors.groupingBy(s -> s.getAssignment().getId(),
                        Collectors.toMap(s -> s.getStudent().getId(), s -> s, (a, b) -> a)));

        List<User> students = classMemberRepository.findByClassRoomIdOrderByJoinedAtAsc(classId).stream()
                .filter(m -> m.getRole() == ClassRole.STUDENT)
                .map(ClassMember::getUser)
                .toList();

        List<GradebookResponse.Row> rows = new ArrayList<>();
        for (User student : students) {
            List<GradebookResponse.Cell> cells = assignments.stream()
                    .map(a -> cell(a, byAssignment.getOrDefault(a.getId(), Map.of()).get(student.getId())))
                    .toList();
            rows.add(new GradebookResponse.Row(student.getId(), student.getName(), cells));
        }

        List<GradebookResponse.Average> averages = assignments.stream().map(a -> {
            Map<Long, Submission> subs = byAssignment.getOrDefault(a.getId(), Map.of());
            var graded = subs.values().stream()
                    .map(Submission::getGrade).filter(g -> g != null).mapToInt(Integer::intValue);
            var stats = graded.summaryStatistics();
            Double avg = stats.getCount() > 0 ? stats.getAverage() : null;
            return new GradebookResponse.Average(a.getId(), avg);
        }).toList();

        return new GradebookResponse(columns, rows, averages);
    }

    private GradebookResponse studentView(User student, Long classId, List<Assignment> assignments,
                                          List<GradebookResponse.Column> columns) {
        Map<Long, Submission> mine = submissionRepository
                .findByStudentIdAndAssignment_ClassRoomId(student.getId(), classId).stream()
                .collect(Collectors.toMap(s -> s.getAssignment().getId(), s -> s, (a, b) -> a));
        List<GradebookResponse.Cell> cells = assignments.stream()
                .map(a -> cell(a, mine.get(a.getId())))
                .toList();
        GradebookResponse.Row row = new GradebookResponse.Row(student.getId(), student.getName(), cells);
        return new GradebookResponse(columns, List.of(row), null);
    }

    private GradebookResponse.Cell cell(Assignment a, Submission s) {
        if (s == null) {
            return new GradebookResponse.Cell(a.getId(), null, null);
        }
        return new GradebookResponse.Cell(a.getId(), s.getGrade(), s.getStatus().name());
    }
}
