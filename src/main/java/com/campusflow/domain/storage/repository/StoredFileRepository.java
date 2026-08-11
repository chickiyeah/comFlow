package com.campusflow.domain.storage.repository;

import com.campusflow.domain.storage.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
