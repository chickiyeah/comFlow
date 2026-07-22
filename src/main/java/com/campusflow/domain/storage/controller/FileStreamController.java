package com.campusflow.domain.storage.controller;

import com.campusflow.domain.storage.dto.FileTicketResponse;
import com.campusflow.domain.storage.entity.StoredFile;
import com.campusflow.domain.storage.service.FileAccessTokenService;
import com.campusflow.domain.storage.service.FileStorageService;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 인증 파일 스트리밍. {@code POST /{id}/ticket}은 Bearer 인증 + 소유 검사 후 단기 티켓을 발급하고,
 * {@code GET /{id}/stream?token=...}은 SecurityConfig permitAll이지만 티켓을 자체 검증한다(Range 지원).
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileStreamController {

    private final FileStorageService fileStorageService;
    private final FileAccessTokenService fileAccessTokenService;
    private final UserRepository userRepository;

    /** 본인이 소유한 파일의 스트리밍 티켓 발급. (자료/자원 등 소비 도메인은 자체 권한검사 후 FileAccessTokenService를 직접 사용) */
    @PostMapping("/{id}/ticket")
    public ApiResponse<FileTicketResponse> ticket(@AuthenticationPrincipal String username, @PathVariable Long id) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        StoredFile stored = fileStorageService.getById(id);
        if (stored.getOwnerUser() == null || !stored.getOwnerUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
        }
        String token = fileAccessTokenService.issue(id, user.getId());
        String streamUrl = "/api/files/" + id + "/stream?token=" + token;
        return ApiResponse.ok(new FileTicketResponse(id, streamUrl, token));
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<?> stream(@PathVariable Long id,
                                    @RequestParam String token,
                                    @RequestHeader HttpHeaders headers) {
        fileAccessTokenService.verify(token, id);
        StoredFile stored = fileStorageService.getById(id);
        Resource resource = fileStorageService.loadAsResource(stored);
        MediaType mediaType = safeMediaType(stored.getContentType());
        long length;
        try {
            length = resource.contentLength();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        String disposition = ContentDisposition.inline()
                .filename(stored.getOriginalFilename(), StandardCharsets.UTF_8).toString();

        List<HttpRange> ranges = headers.getRange();
        if (ranges.isEmpty()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .contentType(mediaType)
                    .contentLength(length)
                    .body(resource);
        }
        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(length);
        long end = range.getRangeEnd(length);
        long rangeLength = end - start + 1;
        ResourceRegion region = new ResourceRegion(resource, start, rangeLength);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(mediaType)
                .body(region);
    }

    private MediaType safeMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
