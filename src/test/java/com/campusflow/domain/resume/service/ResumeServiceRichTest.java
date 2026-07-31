package com.campusflow.domain.resume.service;

import com.campusflow.domain.resume.dto.ResumeRequest;
import com.campusflow.domain.resume.dto.ResumeResponse;
import com.campusflow.domain.resume.entity.Resume;
import com.campusflow.domain.resume.repository.ResumeRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceRichTest {

    @Mock ResumeRepository resumeRepository;
    @Mock com.campusflow.domain.portfolio.repository.PortfolioRepository portfolioRepository;
    @Mock StudentRepository studentRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ResumeService service;

    @Test
    void create가_resumeData와_template을_저장한다() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(10L);
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.of(student));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeRequest req = new ResumeRequest("제목", "요약", "Java,Python", "백엔드",
                "{\"personal\":{\"name\":\"홍길동\"}}", "general", null);

        ResumeResponse res = service.create("u", req);

        assertThat(res.template()).isEqualTo("general");
        assertThat(res.resumeData()).contains("홍길동");
    }
}
