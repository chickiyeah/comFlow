package com.campusflow.domain.schedule.dto;

import com.campusflow.domain.schedule.entity.Schedule;

public record ScheduleResponse(
        Long id,
        String subjectName,
        String subjectCode,
        String professor,
        String room,
        String dayOfWeek,
        String startTime,
        String endTime,
        int year,
        int semester
) {
    public static ScheduleResponse from(Schedule s) {
        return new ScheduleResponse(
                s.getId(),
                s.getSubjectName(),
                s.getSubjectCode() != null ? s.getSubjectCode() : "",
                s.getProfessor() != null ? s.getProfessor() : "",
                s.getRoom() != null ? s.getRoom() : "",
                s.getDayOfWeek().name(),
                s.getStartTime(),
                s.getEndTime(),
                s.getYear(),
                s.getSemester()
        );
    }
}
