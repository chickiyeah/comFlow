package com.campusflow.domain.classpost.service;

import com.campusflow.domain.classpost.dto.*;
import com.campusflow.domain.classpost.entity.ClassPost;
import com.campusflow.domain.classpost.entity.PostComment;
import com.campusflow.domain.classpost.entity.PostType;
import com.campusflow.domain.classpost.repository.ClassPostRepository;
import com.campusflow.domain.classpost.repository.PostCommentRepository;
import com.campusflow.domain.classroom.entity.ClassMember;
import com.campusflow.domain.classroom.entity.ClassRole;
import com.campusflow.domain.classroom.entity.ClassRoom;
import com.campusflow.domain.classroom.service.ClassAccessService;
import com.campusflow.domain.material.entity.Material;
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
public class ClassPostService {

    private final ClassPostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final MaterialRepository materialRepository;
    private final ClassAccessService classAccess;

    public List<PostResponse> list(String username, Long classId) {
        classAccess.requireMember(classId, username);
        return postRepository.findByClassRoomIdOrderByCreatedAtDesc(classId).stream()
                .map(p -> PostResponse.from(p, comments(p.getId())))
                .toList();
    }

    @Transactional
    public PostResponse create(String username, Long classId, PostCreateRequest request) {
        User author = classAccess.requireMember(classId, username).getUser();
        ClassRoom classRoom = classAccess.requireClass(classId);
        Material material = null;
        PostType type = request.type() != null ? request.type() : PostType.ANNOUNCEMENT;
        if (request.materialId() != null) {
            material = materialRepository.findById(request.materialId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            if (!material.getClassRoom().getId().equals(classId)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            type = PostType.MATERIAL;
        } else if (type == PostType.MATERIAL) {
            type = PostType.ANNOUNCEMENT; // material 없이 MATERIAL 불가
        }
        ClassPost post = postRepository.save(ClassPost.builder()
                .classRoom(classRoom).type(type).material(material).author(author).body(request.body()).build());
        return PostResponse.from(post, List.of());
    }

    @Transactional
    public PostResponse update(String username, Long postId, PostUpdateRequest request) {
        ClassPost post = loadPost(postId);
        ClassMember member = classAccess.requireMember(post.getClassRoom().getId(), username);
        requireModifiable(post.getAuthor().getId(), member);
        post.updateBody(request.body());
        return PostResponse.from(post, comments(postId));
    }

    @Transactional
    public void delete(String username, Long postId) {
        ClassPost post = loadPost(postId);
        ClassMember member = classAccess.requireMember(post.getClassRoom().getId(), username);
        requireModifiable(post.getAuthor().getId(), member);
        commentRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    @Transactional
    public PostCommentResponse addComment(String username, Long postId, PostCommentRequest request) {
        ClassPost post = loadPost(postId);
        User author = classAccess.requireMember(post.getClassRoom().getId(), username).getUser();
        PostComment comment = commentRepository.save(PostComment.builder()
                .post(post).author(author).body(request.body()).build());
        return PostCommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(String username, Long postId, Long commentId) {
        ClassPost post = loadPost(postId);
        ClassMember member = classAccess.requireMember(post.getClassRoom().getId(), username);
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!comment.getPost().getId().equals(postId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        requireModifiable(comment.getAuthor().getId(), member);
        commentRepository.delete(comment);
    }

    // ── helpers ──────────────────────────────────────────────
    private List<PostCommentResponse> comments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(PostCommentResponse::from).toList();
    }

    private void requireModifiable(Long authorId, ClassMember member) {
        boolean teacher = member.getRole() != ClassRole.STUDENT;
        if (!teacher && !authorId.equals(member.getUser().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private ClassPost loadPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
