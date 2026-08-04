package com.campusflow.domain.career.repository;

import com.campusflow.domain.career.entity.ImportedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ImportedJobRepository extends JpaRepository<ImportedJob, Long> {

    Optional<ImportedJob> findByUrl(String url);

    /**
     * 적재된 공고 조회.
     * - deadline 이 minDeadline 이상인 것만 (deadline null=상시는 항상 포함).
     *   '지난 공고 숨기기'면 minDeadline=오늘, 전체보기면 아주 과거 날짜를 넘긴다.
     * - keyword 가 있으면 제목/회사명 부분일치.
     * - 마감 임박순(deadline asc), 상시(null)는 뒤로, 같은 조건은 최신 수집순.
     */
    @Query("""
            select j from ImportedJob j
            where (j.deadline is null or j.deadline >= :minDeadline)
              and (:keyword is null or j.title like %:keyword% or j.company like %:keyword%)
            order by case when j.deadline is null then 1 else 0 end, j.deadline asc, j.importedAt desc
            """)
    List<ImportedJob> search(@Param("minDeadline") LocalDate minDeadline,
                             @Param("keyword") String keyword);

    /** 마감이 일정 기준일 이전인 만료 공고 정리(상시 null은 보존). */
    long deleteByDeadlineBefore(LocalDate cutoff);
}
