package com.campusflow.domain.user.repository;

import com.campusflow.domain.user.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    // sendCode()가 항상 기존 레코드를 삭제하므로 email당 최대 1개만 존재
    Optional<EmailVerification> findByEmail(String email);

    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.email = :email")
    void deleteAllByEmail(@Param("email") String email);
}
