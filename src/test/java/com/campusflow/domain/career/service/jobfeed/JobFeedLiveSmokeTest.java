package com.campusflow.domain.career.service.jobfeed;

import com.campusflow.domain.career.dto.JobSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "JOB_FEED_LIVE_TEST", matches = "(?i)true")
class JobFeedLiveSmokeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void zighangReturnsValidPublicPosting() {
        assertValid(new ZighangJobFeedCollector(RestClient.create(), new SitemapParser(),
                new JsonLdJobPostingParser(objectMapper), 3).collectLatest(), "직행");
    }

    @Test
    void rememberReturnsValidPublicPosting() {
        assertValid(new RememberJobFeedCollector(RestClient.create(), new SitemapParser(),
                new JsonLdJobPostingParser(objectMapper), 3).collectLatest(), "리멤버");
    }

    @Test
    void inThisWorkReturnsValidPublicPosting() {
        assertValid(new InThisWorkJobFeedCollector(RestClient.create(), new SitemapParser(), 3)
                .collectLatest(), "인디스워크");
    }

    @Test
    void wantedReturnsValidPublicPosting() {
        assertValid(new WantedJobFeedCollector(RestClient.create(), objectMapper, 3).collectLatest(), "원티드");
    }

    private void assertValid(List<JobSearchResult> jobs, String source) {
        assertThat(jobs).as(source + " should return at least one active posting").isNotEmpty();
        assertThat(jobs).allSatisfy(job -> {
            assertThat(job.source()).isEqualTo(source);
            assertThat(job.title()).isNotBlank();
            assertThat(job.company()).isNotBlank();
            assertThat(job.url()).startsWith("https://");
            if (job.deadline() != null) assertThat(job.deadline()).isAfterOrEqualTo(LocalDate.now());
        });
    }
}
