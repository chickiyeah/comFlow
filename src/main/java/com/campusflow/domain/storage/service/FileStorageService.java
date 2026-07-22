package com.campusflow.domain.storage.service;

import com.campusflow.domain.storage.entity.StoredFile;
import com.campusflow.domain.storage.repository.StoredFileRepository;
import com.campusflow.domain.user.entity.User;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * 로컬 디스크 파일 저장소. 바이트는 {@code campusflow.storage.root} 아래 {@code yyyy/MM/UUID.ext}로 저장하고
 * 메타데이터는 {@link StoredFile}로 DB에 기록한다. (정적 서빙 없음 — 스트리밍은 FileStreamController가 담당)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${campusflow.storage.root:./storage}")
    private String root;

    private final StoredFileRepository storedFileRepository;

    private Path rootPath;

    @PostConstruct
    void init() {
        rootPath = Paths.get(root).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
            log.info("파일 저장 루트: {}", rootPath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    @Transactional
    public StoredFile store(MultipartFile file, User owner) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
        String original = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot);
        }
        LocalDate now = LocalDate.now();
        String key = "%04d/%02d/%s%s".formatted(now.getYear(), now.getMonthValue(), UUID.randomUUID(), ext);
        Path target = rootPath.resolve(key).normalize();
        if (!target.startsWith(rootPath)) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.warn("파일 저장 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
        String contentType = Objects.requireNonNullElse(file.getContentType(), "application/octet-stream");
        StoredFile stored = StoredFile.builder()
                .storageKey(key)
                .originalFilename(original)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .ownerUser(owner)
                .build();
        return storedFileRepository.save(stored);
    }

    public StoredFile getById(Long id) {
        return storedFileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
    }

    public Resource loadAsResource(StoredFile stored) {
        Path path = rootPath.resolve(stored.getStorageKey()).normalize();
        Resource resource = new FileSystemResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return resource;
    }

    @Transactional
    public void delete(StoredFile stored) {
        try {
            Files.deleteIfExists(rootPath.resolve(stored.getStorageKey()).normalize());
        } catch (IOException e) {
            log.warn("파일 삭제 실패({}): {}", stored.getStorageKey(), e.getMessage());
        }
        storedFileRepository.delete(stored);
    }
}
