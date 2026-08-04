package com.campusflow.domain.notification.repository;

import com.campusflow.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);

    long countByStudentIdAndReadFlagFalse(Long studentId);

    @Modifying
    @Query("update Notification n set n.readFlag = true where n.student.id = :studentId and n.readFlag = false")
    int markAllRead(@Param("studentId") Long studentId);
}
