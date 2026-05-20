package com.campusflow.domain.notice.repository;

import com.campusflow.domain.notice.entity.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByImportantDescCreatedAtDesc();
    List<Notice> findAllByOrderByImportantDescCreatedAtDesc(Pageable pageable);
}
