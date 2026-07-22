package com.campusflow.domain.storage.service;

import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * 파일 스트리밍용 단기 HMAC 티켓 발급/검증. {@code <img>/<video>/<iframe>}는 Authorization 헤더를 보낼 수 없어
 * JWT 필터를 우회해야 하므로, 소비 도메인(자료/자원 등)이 권한 검사를 마친 뒤 이 서비스로 단일 파일·단기 티켓을 발급한다.
 * 형식: {@code base64url(fileId:userId:exp) + "." + base64url(HMAC_SHA256)}
 */
@Slf4j
@Service
public class FileAccessTokenService {

    @Value("${campusflow.storage.ticket-secret}")
    private String secret;

    @Value("${campusflow.storage.ticket-ttl-seconds:120}")
    private long ttlSeconds;

    /** 파일 스트리밍 티켓 발급. */
    public String issue(Long fileId, Long userId) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = fileId + ":" + userId + ":" + exp;
        return encode(payload) + "." + sign(payload);
    }

    /**
     * 티켓 검증. 서명·만료·fileId 일치를 확인하고 userId를 반환. 실패 시 {@link ErrorCode#FILE_ACCESS_DENIED}.
     */
    public long verify(String token, Long fileId) {
        if (token == null || !token.contains(".")) {
            throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
        }
        String[] parts = token.split("\\.", 2);
        final String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
        }
        if (!constantTimeEquals(sign(payload), parts[1])) {
            throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
        }
        String[] fields = payload.split(":");
        if (fields.length != 3) {
            throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
        }
        try {
            long tokenFileId = Long.parseLong(fields[0]);
            long userId = Long.parseLong(fields[1]);
            long exp = Long.parseLong(fields[2]);
            if (tokenFileId != fileId || Instant.now().getEpochSecond() > exp) {
                throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
            }
            return userId;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("HMAC 서명 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
