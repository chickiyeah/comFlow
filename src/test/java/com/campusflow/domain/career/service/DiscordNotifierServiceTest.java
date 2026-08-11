package com.campusflow.domain.career.service;

import com.campusflow.domain.career.entity.ImportedJob;
import com.campusflow.domain.career.repository.ImportedJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class DiscordNotifierServiceTest {

    @Mock
    private ImportedJobRepository importedJobRepository;

    private DiscordNotifierService service;

    @BeforeEach
    void setUp() {
        service = new DiscordNotifierService(importedJobRepository);
        ReflectionTestUtils.setField(service, "alertKeywordsCsv",
                "AI,인공지능,머신러닝,ML,딥러닝,LLM,데이터,파이썬,백엔드,개발");
    }

    @Test
    void filterByKeywordsKeepsMatchingAndDropsUnrelatedJobs() {
        ImportedJob aiJob = ImportedJob.builder()
                .source("work24").url("https://jobs.example/ai")
                .title("AI 엔지니어 채용").company("회사A").keyword("개발")
                .build();
        ImportedJob salesJob = ImportedJob.builder()
                .source("work24").url("https://jobs.example/sales")
                .title("영업 관리직").company("회사B").keyword("영업")
                .build();

        List<ImportedJob> matched = service.filterByKeywords(List.of(aiJob, salesJob));

        assertThat(matched).containsExactly(aiJob);
    }

    @Test
    void notifyNewJobsWithBlankWebhookUrlReturnsSilently() {
        ReflectionTestUtils.setField(service, "webhookUrl", "");
        ImportedJob aiJob = ImportedJob.builder()
                .source("work24").url("https://jobs.example/ai")
                .title("AI 엔지니어 채용").company("회사A").keyword("개발")
                .build();

        assertThatCode(() -> service.notifyNewJobs(List.of(aiJob))).doesNotThrowAnyException();
    }
}
