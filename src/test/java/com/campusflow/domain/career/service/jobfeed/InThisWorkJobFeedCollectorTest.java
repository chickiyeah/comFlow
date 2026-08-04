package com.campusflow.domain.career.service.jobfeed;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InThisWorkJobFeedCollectorTest {

    private static final MediaType UTF8_HTML = new MediaType("text", "html", StandardCharsets.UTF_8);

    @Test
    void selectsNewestSitemapParsesArticleAndSkipsClosedPosting() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InThisWorkJobFeedCollector collector = new InThisWorkJobFeedCollector(
                builder.build(), new SitemapParser(), 5);

        server.expect(requestTo(InThisWorkJobFeedCollector.SITEMAP_INDEX))
                .andRespond(withSuccess("""
                        <sitemapindex>
                          <sitemap><loc>https://inthiswork.com/sitemap-1.xml</loc><lastmod>2026-07-20</lastmod></sitemap>
                          <sitemap><loc>https://inthiswork.com/sitemap-8.xml</loc><lastmod>2026-07-22</lastmod></sitemap>
                        </sitemapindex>
                        """, MediaType.APPLICATION_XML));
        server.expect(requestTo("https://inthiswork.com/sitemap-8.xml"))
                .andRespond(withSuccess("""
                        <urlset>
                          <url><loc>https://inthiswork.com/archives/200</loc><lastmod>2026-07-22</lastmod></url>
                          <url><loc>https://inthiswork.com/archives/199</loc><lastmod>2026-07-21</lastmod></url>
                        </urlset>
                        """, MediaType.APPLICATION_XML));
        server.expect(requestTo("https://inthiswork.com/archives/200"))
                .andRespond(withSuccess(activeArticle(), UTF8_HTML));
        server.expect(requestTo("https://inthiswork.com/archives/199"))
                .andRespond(withSuccess("""
                        <meta property="og:title" content="마감회사｜서버 개발자">
                        <p>⛔️ 이 공고는 마감된 공고 혹은 비공개입니다</p>
                        """, UTF8_HTML));

        var jobs = collector.collectLatest();

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).id()).isEqualTo("200");
        assertThat(jobs.get(0).company()).isEqualTo("리디 주식회사");
        assertThat(jobs.get(0).title()).isEqualTo("Full Stack Software Engineer (신입)");
        assertThat(jobs.get(0).jobType()).isEqualTo("정규직");
        assertThat(jobs.get(0).location()).isEqualTo("대한민국 서울특별시 강남구 역삼동 702-28");
        assertThat(jobs.get(0).source()).isEqualTo("인디스워크");
        server.verify();
    }

    private String activeArticle() {
        return """
                <html><head><meta property="og:title" content="리디 주식회사｜Full Stack Software Engineer (신입)"></head>
                <body>
                  <p><br><strong>고용형태</strong><br>정규직</p>
                  <p><br><strong>근무지</strong><br>리디 주식회사<br>대한민국 서울특별시 강남구 역삼동 702-28</p>
                </body></html>
                """;
    }
}
