package com.campusflow.domain.material.service;

import com.campusflow.domain.classroom.service.ClassAccessService;
import com.campusflow.domain.material.dto.BookmarkRequest;
import com.campusflow.domain.material.dto.BookmarkResponse;
import com.campusflow.domain.material.entity.Material;
import com.campusflow.domain.material.entity.MaterialBookmark;
import com.campusflow.domain.material.repository.MaterialBookmarkRepository;
import com.campusflow.domain.material.repository.MaterialRepository;
import com.campusflow.domain.user.entity.User;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final MaterialBookmarkRepository bookmarkRepository;
    private final MaterialRepository materialRepository;
    private final ClassAccessService classAccess;

    public List<BookmarkResponse> list(String username, Long materialId) {
        User user = memberOf(username, materialId);
        return bookmarkRepository.findByMaterialIdAndUserIdOrderByPageAsc(materialId, user.getId()).stream()
                .map(BookmarkResponse::from)
                .toList();
    }

    @Transactional
    public BookmarkResponse add(String username, Long materialId, BookmarkRequest request) {
        User user = memberOf(username, materialId);
        Material material = materialRepository.getReferenceById(materialId);
        MaterialBookmark bookmark = bookmarkRepository
                .findByMaterialIdAndUserIdAndPage(materialId, user.getId(), request.page())
                .orElse(null);
        if (bookmark != null) {
            bookmark.updateNote(request.note());
            return BookmarkResponse.from(bookmark);
        }
        MaterialBookmark saved = bookmarkRepository.save(MaterialBookmark.builder()
                .material(material)
                .user(user)
                .page(request.page())
                .note(request.note())
                .build());
        return BookmarkResponse.from(saved);
    }

    @Transactional
    public void delete(String username, Long materialId, int page) {
        User user = memberOf(username, materialId);
        MaterialBookmark bookmark = bookmarkRepository
                .findByMaterialIdAndUserIdAndPage(materialId, user.getId(), page)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        bookmarkRepository.delete(bookmark);
    }

    private User memberOf(String username, Long materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return classAccess.requireMember(material.getClassRoom().getId(), username).getUser();
    }
}
