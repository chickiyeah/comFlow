package com.campusflow.domain.assignment.entity;

import com.campusflow.domain.storage.entity.StoredFile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 과제에 첨부된 교사 배포 파일. */
@Entity
@Table(name = "assignment_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssignmentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_id", nullable = false)
    private StoredFile storedFile;

    @Builder
    public AssignmentFile(Assignment assignment, StoredFile storedFile) {
        this.assignment = assignment;
        this.storedFile = storedFile;
    }
}
