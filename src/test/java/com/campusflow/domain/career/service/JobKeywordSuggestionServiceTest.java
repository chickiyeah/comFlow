package com.campusflow.domain.career.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.career.dto.KeywordSuggestionResponse;
import com.campusflow.domain.jobpilot.dto.StudentProfileDto;
import com.campusflow.domain.jobpilot.service.ProfileAssembler;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.entity.Role;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobKeywordSuggestionServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ProfileAssembler profileAssembler;
    @Mock private AiFacadeService aiFacadeService;

    private JobKeywordSuggestionService service;

    @BeforeEach
    void setUp() {
        service = new JobKeywordSuggestionService(userRepository, studentRepository, profileAssembler, aiFacadeService);
    }

    private Student studentWithDesiredJob(String desiredJob) {
        User user = User.builder().username("201918023").password("pw").name("홍길동").email("a@a.com").role(Role.ROLE_STUDENT).build();
        Student student = Student.builder()
                .user(user).studentId("201918023").name("홍길동")
                .grade(2).semester(1).department("컴퓨터정보과").phone("010").email("a@a.com")
                .build();
        student.updateDesiredJob(desiredJob);
        return student;
    }

    private void mockLookup(Student student) {
        User user = student.getUser();
        when(userRepository.findByUsername("201918023")).thenReturn(Optional.of(user));
        when(studentRepository.findByUserId(any())).thenReturn(Optional.of(student));
    }

    @Test
    void aiReturnsSuggestions_parsedAndDefaultKeywordIsDesiredJob() {
        Student student = studentWithDesiredJob("백엔드 개발자");
        mockLookup(student);
        when(profileAssembler.assemble("201918023")).thenReturn(new StudentProfileDto(
                "홍길동", "컴퓨터정보과", null, List.of("Java", "Spring"), List.of(), List.of(), List.of(), List.of()));
        when(aiFacadeService.ask(anyString(), anyString()))
                .thenReturn("[\"백엔드 개발자\",\"DevOps 엔지니어\",\"AI 엔지니어\"]");

        KeywordSuggestionResponse res = service.suggest("201918023");

        assertThat(res.defaultKeyword()).isEqualTo("백엔드 개발자");
        assertThat(res.suggestions()).containsExactly("백엔드 개발자", "DevOps 엔지니어", "AI 엔지니어");
    }

    @Test
    void aiThrows_fallsBackToRuleBasedSuggestionsFromSkills() {
        Student student = studentWithDesiredJob(null);
        mockLookup(student);
        when(profileAssembler.assemble("201918023")).thenReturn(new StudentProfileDto(
                "홍길동", "컴퓨터정보과", null, List.of("Docker", "Kubernetes"), List.of(), List.of(), List.of(), List.of()));
        when(aiFacadeService.ask(anyString(), anyString())).thenThrow(new RuntimeException("gateway down"));

        KeywordSuggestionResponse res = service.suggest("201918023");

        assertThat(res.defaultKeyword()).isEqualTo("Docker");
        assertThat(res.suggestions()).isNotEmpty();
        assertThat(res.suggestions()).contains("DevOps 엔지니어");
    }
}
