package com.campusflow.domain.material.repository;

import com.campusflow.domain.material.entity.MaterialBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialBookmarkRepository extends JpaRepository<MaterialBookmark, Long> {
    List<MaterialBookmark> findByMaterialIdAndUserIdOrderByPageAsc(Long materialId, Long userId);
    Optional<MaterialBookmark> findByMaterialIdAndUserIdAndPage(Long materialId, Long userId, int page);
    boolean existsByMaterialIdAndUserIdAndPage(Long materialId, Long userId, int page);
    void deleteByMaterialId(Long materialId);
}
