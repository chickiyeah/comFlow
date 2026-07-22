package com.campusflow.domain.assignment.dto;

import java.util.List;

/**
 * 성적부(read-model, 신규 테이블 없음). 교사는 전체 행 + 과제별 평균, 학생은 본인 행만(averages null).
 */
public record GradebookResponse(
        List<Column> assignments,
        List<Row> rows,
        List<Average> averages
) {
    public record Column(Long assignmentId, String title, int points) {}

    public record Cell(Long assignmentId, Integer grade, String status) {}

    public record Row(Long studentId, String studentName, List<Cell> cells) {}

    public record Average(Long assignmentId, Double average) {}
}
