package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.JobSearchResult;
import com.campusflow.domain.career.entity.ImportedJob;
import com.campusflow.domain.career.repository.ImportedJobRepository;
import com.campusflow.domain.career.service.jobfeed.JobFeedCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobImportServiceTest {

    @Mock private Work24ScraperService work24;
    @Mock private JobkoreaService jobkorea;
    @Mock private SaraminService saramin;
    @Mock private WorknetService worknet;
    @Mock private ImportedJobRepository repository;
    @Mock private JobFeedCollector failingFeed;
    @Mock private JobFeedCollector healthyFeed;
    @Mock private DiscordNotifierService discordNotifierService;

    private JobImportService service;

    @BeforeEach
    void setUp() {
        service = new JobImportService(work24, jobkorea, saramin, worknet, repository,
                List.of(failingFeed, healthyFeed), discordNotifierService);
    }

    @Test
    void feedFailureIsIsolatedAndLaterFeedIsSavedOnce() {
        when(failingFeed.source()).thenReturn("실패소스");
        when(failingFeed.collectLatest()).thenThrow(new IllegalStateException("temporary"));
        when(healthyFeed.collectLatest()).thenReturn(List.of(job("1", "https://jobs.example/1", LocalDate.now().plusDays(5))));
        when(repository.findByUrl("https://jobs.example/1")).thenReturn(Optional.empty());

        int count = service.importAll();

        assertThat(count).isEqualTo(1);
        verify(failingFeed).collectLatest();
        verify(healthyFeed).collectLatest();
        ArgumentCaptor<ImportedJob> saved = ArgumentCaptor.forClass(ImportedJob.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSource()).isEqualTo("원티드");
        assertThat(saved.getValue().getKeyword()).isEqualTo("feed");
    }

    @Test
    void expiredAndBlankUrlFeedJobsAreSkipped() {
        when(failingFeed.collectLatest()).thenReturn(List.of(
                job("old", "https://jobs.example/old", LocalDate.now().minusDays(1)),
                job("blank", " ", LocalDate.now().plusDays(1))));
        when(healthyFeed.collectLatest()).thenReturn(List.of());

        assertThat(service.importAll()).isZero();

        verify(repository, never()).save(any());
        verify(repository, never()).findByUrl(any());
    }

    @Test
    void duplicateUrlRefreshesExistingEntityWithoutInsert() {
        ImportedJob existing = ImportedJob.builder()
                .source("원티드").url("https://jobs.example/1").title("이전 제목").company("회사")
                .build();
        when(failingFeed.collectLatest()).thenReturn(List.of());
        when(healthyFeed.collectLatest()).thenReturn(List.of(job("1", "https://jobs.example/1", LocalDate.now().plusDays(2))));
        when(repository.findByUrl("https://jobs.example/1")).thenReturn(Optional.of(existing));

        assertThat(service.importAll()).isEqualTo(1);

        assertThat(existing.getTitle()).isEqualTo("새 제목");
        verify(repository, never()).save(any());
    }

    private JobSearchResult job(String id, String url, LocalDate deadline) {
        return new JobSearchResult(id, "새 제목", "새 회사", "서울", url, deadline,
                "신입", null, "원티드");
    }
}
