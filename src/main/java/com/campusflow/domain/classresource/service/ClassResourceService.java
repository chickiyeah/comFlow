package com.campusflow.domain.classresource.service;

import com.campusflow.domain.classresource.dto.ResourceResponse;
import com.campusflow.domain.classresource.entity.ClassResource;
import com.campusflow.domain.classresource.entity.ResourceType;
import com.campusflow.domain.classresource.repository.ClassResourceRepository;
import com.campusflow.domain.classroom.entity.ClassRoom;
import com.campusflow.domain.classroom.service.ClassAccessService;
import com.campusflow.domain.storage.entity.StoredFile;
import com.campusflow.domain.storage.service.FileAccessTokenService;
import com.campusflow.domain.storage.service.FileStorageService;
import com.campusflow.domain.user.entity.User;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassResourceService {

    private final ClassResourceRepository resourceRepository;
    private final ClassAccessService classAccess;
    private final FileStorageService fileStorageService;
    private final FileAccessTokenService fileAccessTokenService;

    public List<ResourceResponse> list(String username, Long classId) {
        User user = classAccess.requireMember(classId, username).getUser();
        return resourceRepository.findByClassRoomIdOrderByCreatedAtDesc(classId).stream()
                .map(r -> ResourceResponse.from(r, mintStreamUrl(r.getStoredFile(), user.getId())))
                .toList();
    }

    @Transactional
    public ResourceResponse create(String username, Long classId, String title, ResourceType type,
                                   String url, MultipartFile file) {
        User teacher = classAccess.requireTeacher(classId, username).getUser();
        ClassRoom classRoom = classAccess.requireClass(classId);
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        ResourceType resolved = type != null ? type
                : (file != null && !file.isEmpty() ? ResourceType.FILE : ResourceType.LINK);

        StoredFile stored = null;
        String link = null;
        if (resolved == ResourceType.FILE) {
            if (file == null || file.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            stored = fileStorageService.store(file, teacher);
        } else {
            if (url == null || url.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            link = url;
        }
        ClassResource resource = resourceRepository.save(ClassResource.builder()
                .classRoom(classRoom).type(resolved).title(title).url(link)
                .storedFile(stored).createdBy(teacher).build());
        return ResourceResponse.from(resource, mintStreamUrl(stored, teacher.getId()));
    }

    @Transactional
    public void delete(String username, Long resourceId) {
        ClassResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        classAccess.requireTeacher(resource.getClassRoom().getId(), username);
        StoredFile stored = resource.getStoredFile();
        resourceRepository.delete(resource);
        if (stored != null) {
            fileStorageService.delete(stored);
        }
    }

    private String mintStreamUrl(StoredFile stored, Long userId) {
        if (stored == null) {
            return null;
        }
        String token = fileAccessTokenService.issue(stored.getId(), userId);
        return "/api/files/" + stored.getId() + "/stream?token=" + token;
    }
}
