package com.campusflow.domain.classattendance.service;

import com.campusflow.domain.classattendance.dto.*;
import com.campusflow.domain.classattendance.entity.ClassAttendanceRecord;
import com.campusflow.domain.classattendance.entity.ClassAttendanceSession;
import com.campusflow.domain.classattendance.entity.ClassAttendanceStatus;
import com.campusflow.domain.classattendance.repository.ClassAttendanceRecordRepository;
import com.campusflow.domain.classattendance.repository.ClassAttendanceSessionRepository;
import com.campusflow.domain.classroom.entity.ClassMember;
import com.campusflow.domain.classroom.entity.ClassRole;
import com.campusflow.domain.classroom.entity.ClassRoom;
import com.campusflow.domain.classroom.repository.ClassMemberRepository;
import com.campusflow.domain.classroom.service.ClassAccessService;
import com.campusflow.domain.user.entity.User;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassAttendanceService {

    private final ClassAttendanceSessionRepository sessionRepository;
    private final ClassAttendanceRecordRepository recordRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassAccessService classAccess;

    @Transactional
    public SessionResponse createSession(String username, Long classId, SessionCreateRequest request) {
        User teacher = classAccess.requireTeacher(classId, username).getUser();
        ClassRoom classRoom = classAccess.requireClass(classId);
        ClassAttendanceSession session = sessionRepository.save(ClassAttendanceSession.builder()
                .classRoom(classRoom)
                .title(request.title())
                .date(request.date() != null ? request.date() : LocalDate.now())
                .openedBy(teacher)
                .active(true)
                .build());
        // 학생 전원 ABSENT 기록 자동 생성
        classMemberRepository.findByClassRoomIdOrderByJoinedAtAsc(classId).stream()
                .filter(m -> m.getRole() == ClassRole.STUDENT)
                .forEach(m -> recordRepository.save(ClassAttendanceRecord.builder()
                        .session(session).student(m.getUser())
                        .status(ClassAttendanceStatus.ABSENT).build()));
        return SessionResponse.of(session, records(session.getId()));
    }

    public List<SessionResponse> listSessions(String username, Long classId) {
        classAccess.requireMember(classId, username);
        return sessionRepository.findByClassRoomIdOrderByDateDesc(classId).stream()
                .map(s -> SessionResponse.of(s, null))
                .toList();
    }

    public SessionResponse getSession(String username, Long sessionId) {
        ClassAttendanceSession session = loadSession(sessionId);
        classAccess.requireTeacher(session.getClassRoom().getId(), username);
        return SessionResponse.of(session, records(sessionId));
    }

    @Transactional
    public SessionResponse mark(String username, Long sessionId, MarkRequest request) {
        ClassAttendanceSession session = loadSession(sessionId);
        Long classId = session.getClassRoom().getId();
        classAccess.requireTeacher(classId, username);
        ClassMember target = classMemberRepository.findByClassRoomIdAndUserId(classId, request.studentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_ACCESS_DENIED));
        ClassAttendanceRecord record = recordRepository
                .findBySessionIdAndStudentId(sessionId, request.studentId())
                .orElse(null);
        if (record != null) {
            record.mark(request.status(), LocalDateTime.now());
        } else {
            recordRepository.save(ClassAttendanceRecord.builder()
                    .session(session).student(target.getUser())
                    .status(request.status()).markedAt(LocalDateTime.now()).build());
        }
        return SessionResponse.of(session, records(sessionId));
    }

    public List<MyAttendanceResponse> myAttendance(String username, Long classId) {
        User user = classAccess.requireMember(classId, username).getUser();
        return recordRepository
                .findByStudentIdAndSession_ClassRoomIdOrderBySession_DateDesc(user.getId(), classId).stream()
                .map(MyAttendanceResponse::from)
                .toList();
    }

    @Transactional
    public void deleteSession(String username, Long sessionId) {
        ClassAttendanceSession session = loadSession(sessionId);
        classAccess.requireTeacher(session.getClassRoom().getId(), username);
        recordRepository.deleteBySessionId(sessionId);
        sessionRepository.delete(session);
    }

    private List<AttendanceRecordResponse> records(Long sessionId) {
        return recordRepository.findBySessionIdOrderById(sessionId).stream()
                .map(AttendanceRecordResponse::from).toList();
    }

    private ClassAttendanceSession loadSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
