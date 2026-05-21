package com.campusflow.global.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from:noreply@campusflow.jvision.org}")
    private String from;

    @Async
    public void send(String to, String subject, String htmlContent) {
        if (to == null || to.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("[CampusFlow] " + subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.debug("이메일 발송 완료: {}", to);
        } catch (Exception e) {
            log.error("이메일 발송 실패 → {}: {}", to, e.getMessage());
        }
    }
}
