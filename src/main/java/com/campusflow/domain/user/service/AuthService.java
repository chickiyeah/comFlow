package com.campusflow.domain.user.service;

import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.dto.LoginRequest;
import com.campusflow.domain.user.dto.RegisterRequest;
import com.campusflow.domain.user.dto.TokenResponse;
import com.campusflow.domain.user.entity.Role;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.domain.notification.entity.NotificationType;
import com.campusflow.domain.notification.service.NotificationService;
import com.campusflow.domain.session.service.UserSessionService;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.campusflow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final EmailVerificationService emailVerificationService;
    private final UserSessionService sessionService;
    private final NotificationService notificationService;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.DUPLICATE_STUDENT_ID);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (!emailVerificationService.isVerified(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .email(request.email())
                .role(request.role())
                .build();
        userRepository.save(user);

        // 학생 역할이면 Student 엔티티도 생성 (grade/semester는 1학년 1학기 기본값)
        if (request.role() == Role.ROLE_STUDENT) {
            Student student = Student.builder()
                    .user(user)
                    .studentId(request.username())
                    .name(request.name())
                    .grade(1)
                    .semester(1)
                    .department("컴퓨터정보과")
                    .email(request.email())
                    .build();
            studentRepository.save(student);
        }
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String userAgent, String ip) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            User user = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

            // 세션(기기) 등록 — jti를 토큰에 심어 원격 로그아웃 가능
            UserSessionService.Created session = sessionService.createSession(user, userAgent, ip);
            String token = tokenProvider.generateToken(auth, session.jti());

            // 다른 기기가 이미 로그인 중이었다면 중복접속 알림 (학생 한정, best-effort)
            if (session.concurrent()) {
                studentRepository.findByUserId(user.getId()).ifPresent(student -> {
                    try {
                        notificationService.create(student, NotificationType.SYSTEM,
                                "새 기기 로그인 감지",
                                "다른 기기에서 계정에 로그인했습니다. 본인이 아니라면 프로필 > 접속 기기에서 로그아웃하세요.",
                                "/profile");
                    } catch (Exception ignored) { /* 알림 실패는 로그인에 영향 없음 */ }
                });
            }

            return new TokenResponse(token, user.getName(), user.getRole().name());
        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    /** 아이디 찾기 — 이메일로 가입된 계정의 아이디(학번) 반환 */
    @Transactional(readOnly = true)
    public String findUsername(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND))
                .getUsername();
    }

    /** 비밀번호 재설정 — 이메일 인증코드 확인 후 새 비밀번호로 변경 */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        emailVerificationService.verifyCode(email, code); // 코드 불일치/만료 시 예외
        user.updatePassword(passwordEncoder.encode(newPassword));
    }
}
