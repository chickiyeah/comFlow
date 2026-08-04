package com.campusflow.domain.career.service.jobfeed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RememberJobFeedCollectorTest {

    private static final MediaType UTF8_HTML = new MediaType("text", "html", StandardCharsets.UTF_8);

    @Test
    void readsPublicJobSitemapAndParsesPosting() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RememberJobFeedCollector collector = new RememberJobFeedCollector(
                builder.build(), new SitemapParser(), new JsonLdJobPostingParser(new ObjectMapper()), 5);

        server.expect(requestTo(RememberJobFeedCollector.SITEMAP_URL))
                .andExpect(header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 Chrome/126.0 Safari/537.36"))
                .andRespond(withSuccess("""
                        <urlset><url><loc>https://career.rememberapp.co.kr/job/posting/328292</loc><lastmod>2026-07-22</lastmod></url></urlset>
                        """, MediaType.APPLICATION_XML));
        server.expect(requestTo("https://career.rememberapp.co.kr/job/posting/328292"))
                .andRespond(withSuccess("""
                        <script type="application/ld+json">
                        {"@type":"JobPosting","title":"AI 플랫폼 엔지니어","validThrough":"2027-08-16",
                         "experienceRequirements":"경력 3년 이상","hiringOrganization":{"name":"리멤버랩"}}
                        </script>
                        """, UTF8_HTML));

        var jobs = collector.collectLatest();

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).company()).isEqualTo("리멤버랩");
        assertThat(jobs.get(0).source()).isEqualTo("리멤버");
        server.verify();
    }
}
