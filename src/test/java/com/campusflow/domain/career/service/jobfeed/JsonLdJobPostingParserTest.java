package com.campusflow.domain.career.service.jobfeed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class JsonLdJobPostingParserTest {

    private final JsonLdJobPostingParser parser = new JsonLdJobPostingParser(new ObjectMapper());

    @Test
    void parsesJobPostingObjectAndNormalizesNestedFields() {
        String html = """
                <html><head><script type="application/ld+json">
                {
                  "@type":"JobPosting",
                  "title":"Product Designer",
                  "validThrough":"2027-07-22T23:59:59+09:00",
                  "employmentType":"FULL_TIME",
                  "hiringOrganization":{"name":"드림어스컴퍼니"},
                  "jobLocation":{"address":{"addressRegion":"서울","addressLocality":"서초구"}},
                  "experienceRequirements":{"monthsOfExperience":60},
                  "identifier":{"value":"job-123"},
                  "url":"https://example.com/jobs/job-123"
                }
                </script></head></html>
                """;

        var job = parser.parse(html, "https://fallback.example/job-123", "직행").orElseThrow();

        assertThat(job.id()).isEqualTo("job-123");
        assertThat(job.title()).isEqualTo("Product Designer");
        assertThat(job.company()).isEqualTo("드림어스컴퍼니");
        assertThat(job.location()).isEqualTo("서울 서초구");
        assertThat(job.url()).isEqualTo("https://example.com/jobs/job-123");
        assertThat(job.deadline()).isEqualTo(LocalDate.of(2027, 7, 22));
        assertThat(job.jobType()).isEqualTo("경력 5년 이상");
        assertThat(job.source()).isEqualTo("직행");
    }

    @Test
    void findsJobPostingInsideGraphAndSupportsLocationArray() {
        String html = """
                <script type="application/ld+json">
                {"@graph":[
                  {"@type":"BreadcrumbList"},
                  {"@type":"JobPosting","title":"백엔드 개발자",
                   "validThrough":"2026-08-16","experienceRequirements":"경력 2년~5년 차",
                   "hiringOrganization":{"name":"테스트랩"},
                   "jobLocation":[{"address":{"addressRegion":"서울특별시","addressLocality":"강남구"}}]}
                ]}
                </script>
                """;

        var job = parser.parse(html, "https://example.com/posting/99", "리멤버").orElseThrow();

        assertThat(job.id()).isEqualTo("99");
        assertThat(job.location()).isEqualTo("서울특별시 강남구");
        assertThat(job.jobType()).isEqualTo("경력 2년~5년 차");
        assertThat(job.url()).isEqualTo("https://example.com/posting/99");
    }

    @Test
    void invalidOrNonJobPostingHtmlReturnsEmpty() {
        assertThat(parser.parse("", "https://example.com/1", "직행")).isEmpty();
        assertThat(parser.parse("<script type='application/ld+json'>{bad}</script>",
                "https://example.com/1", "직행")).isEmpty();
        assertThat(parser.parse("<script type='application/ld+json'>{\"@type\":\"Article\"}</script>",
                "https://example.com/1", "직행")).isEmpty();
    }
}
