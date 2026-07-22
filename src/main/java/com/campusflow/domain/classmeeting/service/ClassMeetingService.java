package com.campusflow.domain.classmeeting.service;

import com.campusflow.domain.classmeeting.dto.MeetingResponse;
import com.campusflow.domain.classmeeting.entity.ClassMeeting;
import com.campusflow.domain.classmeeting.repository.ClassMeetingRepository;
import com.campusflow.domain.classroom.entity.ClassRoom;
import com.campusflow.domain.classroom.service.ClassAccessService;
import com.campusflow.domain.user.entity.User;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassMeetingService {

    private static final String ROOM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    @Value("${campusflow.meeting.jitsi-base:https://meet.jit.si}")
    private String jitsiBase;

    private final ClassMeetingRepository meetingRepository;
    private final ClassAccessService classAccess;
    private final SecureRandom random = new SecureRandom();

    /** 미팅 시작(교사). 이미 활성 미팅이 있으면 그대로 반환(멱등). */
    @Transactional
    public MeetingResponse start(String username, Long classId) {
        User teacher = classAccess.requireTeacher(classId, username).getUser();
        ClassRoom classRoom = classAccess.requireClass(classId);
        return meetingRepository.findFirstByClassRoomIdAndActiveTrueOrderByStartedAtDesc(classId)
                .map(MeetingResponse::from)
                .orElseGet(() -> {
                    ClassMeeting meeting = meetingRepository.save(ClassMeeting.builder()
                            .classRoom(classRoom)
                            .roomUrl(generateRoomUrl(classId))
                            .active(true)
                            .startedBy(teacher)
                            .startedAt(LocalDateTime.now())
                            .build());
                    return MeetingResponse.from(meeting);
                });
    }

    /** 현재 활성 미팅 조회(멤버). 없으면 null. */
    public MeetingResponse current(String username, Long classId) {
        classAccess.requireMember(classId, username);
        return meetingRepository.findFirstByClassRoomIdAndActiveTrueOrderByStartedAtDesc(classId)
                .map(MeetingResponse::from)
                .orElse(null);
    }

    /** 미팅 종료(교사). */
    @Transactional
    public MeetingResponse end(String username, Long classId) {
        classAccess.requireTeacher(classId, username);
        ClassMeeting meeting = meetingRepository
                .findFirstByClassRoomIdAndActiveTrueOrderByStartedAtDesc(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        meeting.end(LocalDateTime.now());
        return MeetingResponse.from(meeting);
    }

    private String generateRoomUrl(Long classId) {
        StringBuilder suffix = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            suffix.append(ROOM_CHARS.charAt(random.nextInt(ROOM_CHARS.length())));
        }
        String base = jitsiBase.endsWith("/") ? jitsiBase.substring(0, jitsiBase.length() - 1) : jitsiBase;
        return base + "/campusflow-" + classId + "-" + suffix;
    }
}
