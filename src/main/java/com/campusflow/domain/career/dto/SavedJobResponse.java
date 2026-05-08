package com.campusflow.domain.career.dto;

import com.campusflow.domain.career.entity.SavedJob;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SavedJobResponse(
        Long id,
        String title,
        String company,
        String location,
        String url,
        LocalDate deadline,
        String jobType,
        String salary,
        String description,
        String source,
        LocalDateTime savedAt
) {
    public static SavedJobResponse from(SavedJob j) {
        return new SavedJobResponse(
                j.getId(), j.getTitle(), j.getCompany(), j.getLocation(),
                j.getUrl(), j.getDeadline(), j.getJobType(), j.getSalary(),
                j.getDescription(), j.getSource(), j.getSavedAt()
        );
    }
}
