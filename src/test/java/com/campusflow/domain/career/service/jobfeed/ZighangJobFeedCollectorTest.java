package com.campusflow.domain.career.service.jobfeed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ZighangJobFeedCollectorTest {

    private static final MediaType UTF8_HTML = new MediaType("text", "html", StandardCharsets.UTF_8);

    @Test
    void selectsNewestRecruitmentSitemapAndParsesPosting() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ZighangJobFeedCollector collector = new ZighangJobFeedCollector(
                builder.build(), new SitemapParser(), new JsonLdJobPostingParser(new ObjectMapper()), 5);

        server.expect(requestTo(ZighangJobFeedCollector.SITEMAP_INDEX))
                .andRespond(withSuccess("""
                        <sitemapindex>
                          <sitemap><loc>https://zighang.com/seo/sitemap/sitemap-recruitment-1.xml</loc></sitemap>
                          <sitemap><loc>https://zighang.com/seo/sitemap/sitemap-recruitment-12.xml</loc></sitemap>
                        </sitemapindex>
                        """, MediaType.APPLICATION_XML));
        server.expect(requestTo("https://zighang.com/seo/sitemap/sitemap-recruitment-12.xml"))
                .andRespond(withSuccess("""
                        <urlset><url><loc>https://zighang.com/recruitment/job-12</loc><lastmod>2026-07-22</lastmod></url></urlset>
                        """, MediaType.APPLICATION_XML));
        server.expect(requestTo("https://zighang.com/recruitment/job-12"))
                .andRespond(withSuccess(jobPosting("직행 백엔드", "직행테크"), UTF8_HTML));

        var jobs = collector.collectLatest();

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("직행 백엔드");
        assertThat(jobs.get(0).source()).isEqualTo("직행");
        server.verify();
    }

    private String jobPosting(String title, String company) {
        return """
                <script type="application/ld+json">
                {"@type":"JobPosting","title":"%s","validThrough":"2027-07-22",
                 "hiringOrganization":{"name":"%s"},"identifier":{"value":"job-12"}}
                </script>
                """.formatted(title, company);
    }
}
