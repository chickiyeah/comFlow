package com.campusflow.domain.classpost.repository;

import com.campusflow.domain.classpost.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    List<PostComment> findByPostIdOrderByCreatedAtAsc(Long postId);
    void deleteByPostId(Long postId);
}
