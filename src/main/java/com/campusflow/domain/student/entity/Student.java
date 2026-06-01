package com.campusflow.domain.student.entity;

import com.campusflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(unique = true, nullable = false, length = 20)
    private String studentId; // 학번

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private int grade;

    @Column(nullable = false)
    private int semester;

    @Column(nullable = false, length = 50)
    private String department;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    // ── 학교 포털 연동 ────────────────────────────────────────
    @Column(columnDefinition = "LONGTEXT")
    private String profileImageData;      // 학생증 사진 Base64

    private Boolean intranetSyncEnabled;  // 연동 ON/OFF

    private LocalDateTime intranetSyncedAt; // 마지막 동기화 시각

    @Column(columnDefinition = "TEXT")
    private String portalAccessToken;     // 학교 포털 Access Token (JWT)

    @Column(columnDefinition = "TEXT")
    private String portalRefreshToken;    // 학교 포털 Refresh Token (JWT, 김)

    @Column(columnDefinition = "TEXT")
    private String portalSessionCookies;  // 세션 쿠키 직렬화 JSON (Access Token과 함께 필수)

    @Column(columnDefinition = "TEXT")
    private String portalPassword;        // 학교 포털 비밀번호 (저장 동의 후 보관)

    @Builder
    public Student(User user, String studentId, String name, int grade, int semester,
                   String department, String phone, String email) {
        this.user       = user;
        this.studentId  = studentId;
        this.name       = name;
        this.grade      = grade;
        this.semester   = semester;
        this.department = department;
        this.phone      = phone;
        this.email      = email;
        this.intranetSyncEnabled = false;
    }

    /** 포털 연동 동기화 — 토큰 포함 */
    /**
     * 학교 포털 동기화 적용
     * sessionCookies: 로그인 시 발급된 세션 쿠키 JSON (Access Token과 반드시 함께 사용)
     */
    public void applyIntranetSync(String name, String phone, String email,
                                   String profileImageData,
                                   String accessToken, String refreshToken,
                                   String sessionCookiesJson, String password) {
        if (name != null && !name.isBlank())        this.name = name;
        if (phone != null && !phone.isBlank())       this.phone = phone;
        if (email != null && !email.isBlank())       this.email = email;
        if (profileImageData != null)                this.profileImageData = profileImageData;
        if (accessToken != null)                     this.portalAccessToken = accessToken;
        if (refreshToken != null)                    this.portalRefreshToken = refreshToken;
        if (sessionCookiesJson != null)              this.portalSessionCookies = sessionCookiesJson;
        if (password != null && !password.isBlank()) this.portalPassword = password;
        this.intranetSyncEnabled = true;
        this.intranetSyncedAt    = LocalDateTime.now();
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public void updateAcademic(int grade, int semester) {
        this.grade = grade;
        this.semester = semester;
    }

    public void disableIntranetSync() {
        this.intranetSyncEnabled    = false;
        this.portalAccessToken      = null;
        this.portalRefreshToken     = null;
        this.portalSessionCookies   = null;
        this.portalPassword         = null;
    }
}
