package com.campusflow.domain.review.service;

import com.campusflow.domain.review.dto.CourseReviewRequest;
import com.campusflow.domain.review.dto.CourseReviewResponse;
import com.campusflow.domain.review.entity.CourseReview;
import com.campusflow.domain.review.repository.CourseReviewRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewService {

    private final CourseReviewRepository reviewRepo;
    private final UserRepository         userRepo;
    private final StudentRepository      studentRepo;

    public Map<String, Object> getBySubject(String subjectName, String username) {
        Long myId = getStudent(username).getId();
        List<CourseReviewResponse> reviews =
                reviewRepo.findBySubjectNameContainingIgnoreCaseOrderByCreatedAtDesc(subjectName)
                        .stream().map(r -> CourseReviewResponse.from(r, myId)).toList();
        Double avg = reviewRepo.getAverageRating(subjectName);
        return Map.of("reviews", reviews, "averageRating", avg != null ? avg : 0.0, "count", reviews.size());
    }

    public List<CourseReviewResponse> getMyReviews(String username) {
        Long myId = getStudent(username).getId();
        return reviewRepo.findByStudentIdOrderByCreatedAtDesc(myId)
                .stream().map(r -> CourseReviewResponse.from(r, myId)).toList();
    }

    @Transactional
    public CourseReviewResponse create(String username, CourseReviewRequest req) {
        Student student = getStudent(username);
        CourseReview review = new CourseReview(
                student, req.subjectName(), req.professor(),
                req.year(), req.semester(), req.rating(), req.content(), req.anonymous()
        );
        return CourseReviewResponse.from(reviewRepo.save(review), student.getId());
    }

    @Transactional
    public void delete(String username, Long id) {
        CourseReview review = reviewRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.getStudent().getId().equals(getStudent(username).getId()))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        reviewRepo.delete(review);
    }

    private Student getStudent(String username) {
        Long userId = userRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND)).getId();
        return studentRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
