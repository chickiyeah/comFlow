package com.campusflow.domain.grade.repository;

import com.campusflow.domain.grade.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudentIdOrderByGradeYearAscGradeSemesterAsc(Long studentId);

    List<Grade> findByStudentIdAndGradeYearAndGradeSemester(Long studentId, int year, int semester);

    @Query("SELECT SUM(g.gradePoint * g.credits) / SUM(g.credits) FROM Grade g WHERE g.student.id = :studentId")
    Double calculateGpa(@Param("studentId") Long studentId);
}
