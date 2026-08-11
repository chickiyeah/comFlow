package com.campusflow.domain.material.service;

import com.campusflow.domain.classroom.entity.ClassRoom;
import com.campusflow.domain.classroom.service.ClassAccessService;
import com.campusflow.domain.material.dto.MaterialDetailResponse;
import com.campusflow.domain.material.dto.MaterialResponse;
import com.campusflow.domain.material.dto.MaterialUpdateRequest;
import com.campusflow.domain.material.entity.Material;
import com.campusflow.domain.material.repository.MaterialBookmarkRepository;
import com.campusflow.domain.material.repository.MaterialRepository;
import com.campusflow.domain.material.repository.MaterialSummaryRepository;
import com.campusflow.domain.portfolio.service.FileParserService;
import com.campusflow.domain.storage.dto.FileTicketResponse;
import com.campusflow.domain.storage.entity.StoredFile;
import com.campusflow.domain.storage.service.FileAccessTokenService;
import com.campusflow.domain.storage.service.FileStorageService;
import com.campusflow.domain.user.entity.User;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialSummaryRepository summaryRepository;
    private final MaterialBookmarkRepository bookmarkRepository;
    private final ClassAccessService classAccess;
    private final FileStorageService fileStorageService;
    private final FileParserService fileParserService;
    private final FileAccessTokenService fileAccessTokenService;

    public List<MaterialResponse> list(String username, Long classId) {
        classAccess.requireMember(classId, username);
        return materialRepository.findByClassRoomIdOrderByWeekAscCreatedAtAsc(classId).stream()
                .map(m -> MaterialResponse.from(m, summaryRepository.existsByMaterialId(m.getId())))
                .toList();
    }

    public MaterialDetailResponse getDetail(String username, Long materialId) {
        Material material = loadMaterial(materialId);
        User user = classAccess.requireMember(material.getClassRoom().getId(), username).getUser();
        return MaterialDetailResponse.from(material, mintStreamUrl(material, user));
    }

    @Transactional
    public MaterialResponse upload(String username, Long classId, String title, String instructions,
                                   Integer week, String topic, MultipartFile file) {
        User teacher = classAccess.requireTeacher(classId, username).getUser();
        ClassRoom classRoom = classAccess.requireClass(classId);

        StoredFile stored = null;
        String textContent = null;
        if (file != null && !file.isEmpty()) {
            stored = fileStorageService.store(file, teacher);
            textContent = extractTextSafely(file);
        }

        Material material = Material.builder()
                .classRoom(classRoom)
                .title(title)
                .instructions(instructions)
                .week(week)
                .topic(topic)
                .storedFile(stored)
                .textContent(textContent)
                .createdBy(teacher)
                .build();
        return MaterialResponse.from(materialRepository.save(material), false);
    }

    @Transactional
    public MaterialResponse update(String username, Long materialId, MaterialUpdateRequest request) {
        Material material = loadMaterial(materialId);
        classAccess.requireTeacher(material.getClassRoom().getId(), username);
        material.update(request.title(), request.instructions(), request.week());
        return MaterialResponse.from(material, summaryRepository.existsByMaterialId(materialId));
    }

    @Transactional
    public MaterialResponse updateTopic(String username, Long materialId, String topic) {
        Material material = loadMaterial(materialId);
        classAccess.requireTeacher(material.getClassRoom().getId(), username);
        material.updateTopic(topic);
        return MaterialResponse.from(material, summaryRepository.existsByMaterialId(materialId));
    }

    @Transactional
    public void delete(String username, Long materialId) {
        Material material = loadMaterial(materialId);
        classAccess.requireTeacher(material.getClassRoom().getId(), username);
        // 의존 행 먼저 제거 (FK 제약)
        summaryRepository.deleteByMaterialId(materialId);
        bookmarkRepository.deleteByMaterialId(materialId);
        StoredFile stored = material.getStoredFile();
        materialRepository.delete(material);
        if (stored != null) {
            fileStorageService.delete(stored);
        }
    }

    /** 자료 파일 스트리밍 티켓 발급 (멤버). */
    public FileTicketResponse streamTicket(String username, Long materialId) {
        Material material = loadMaterial(materialId);
        User user = classAccess.requireMember(material.getClassRoom().getId(), username).getUser();
        StoredFile stored = material.getStoredFile();
        if (stored == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        String token = fileAccessTokenService.issue(stored.getId(), user.getId());
        return new FileTicketResponse(stored.getId(),
                "/api/files/" + stored.getId() + "/stream?token=" + token, token);
    }

    private String mintStreamUrl(Material material, User user) {
        StoredFile stored = material.getStoredFile();
        if (stored == null) {
            return null;
        }
        String token = fileAccessTokenService.issue(stored.getId(), user.getId());
        return "/api/files/" + stored.getId() + "/stream?token=" + token;
    }

    private String extractTextSafely(MultipartFile file) {
        try {
            return fileParserService.extractText(file);
        } catch (IllegalArgumentException e) {
            // PDF/PPTX 외 형식(이미지·영상 등) — 본문 텍스트 없음
            return null;
        } catch (Exception e) {
            log.warn("자료 텍스트 추출 실패({}): {}", file.getOriginalFilename(), e.getMessage());
            return null;
        }
    }

    private Material loadMaterial(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
