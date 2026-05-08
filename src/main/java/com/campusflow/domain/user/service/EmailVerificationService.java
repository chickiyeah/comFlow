package com.campusflow.domain.user.service;

import com.campusflow.config.MailProperties;
import com.campusflow.domain.user.entity.EmailVerification;
import com.campusflow.domain.user.repository.EmailVerificationRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void sendCode(String email) {
        verificationRepository.deleteAllByEmail(email);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        verificationRepository.save(EmailVerification.builder().email(email).code(code).build());
        sendMail(email, code);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        EmailVerification ev = verificationRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_CODE_NOT_FOUND));

        if (ev.isExpired()) throw new BusinessException(ErrorCode.EMAIL_CODE_EXPIRED);
        if (!ev.getCode().equals(code)) throw new BusinessException(ErrorCode.EMAIL_CODE_INVALID);

        ev.verify();
    }

    public boolean isVerified(String email) {
        return verificationRepository.findByEmail(email)
                .map(ev -> ev.isVerified() && !ev.isExpired())
                .orElse(false);
    }

    private void sendMail(String to, String code) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject("[CampusFlow] 이메일 인증 코드");
            helper.setText(buildHtml(code), true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String buildHtml(String code) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;background:#f7f9fb;border-radius:16px;">
                  <div style="text-align:center;margin-bottom:24px;">
                    <span style="background:#00236f;color:#bff365;font-size:24px;font-weight:900;padding:8px 20px;border-radius:12px;letter-spacing:2px;">CF</span>
                  </div>
                  <h2 style="color:#00236f;text-align:center;margin-bottom:8px;">이메일 인증 코드</h2>
                  <p style="color:#757682;text-align:center;font-size:14px;margin-bottom:24px;">아래 6자리 코드를 입력해 이메일을 인증하세요.<br>코드는 <strong>10분간</strong> 유효합니다.</p>
                  <div style="background:#fff;border:2px solid #bff365;border-radius:16px;padding:24px;text-align:center;">
                    <span style="font-size:40px;font-weight:900;letter-spacing:12px;color:#00236f;">%s</span>
                  </div>
                  <p style="color:#c5c5d3;text-align:center;font-size:12px;margin-top:24px;">본인이 요청하지 않은 경우 이 메일을 무시하세요.</p>
                </div>
                """.formatted(code);
    }
}
