package com.campusflow.domain.career.service.jobfeed;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SitemapParserTest {

    private final SitemapParser parser = new SitemapParser();

    @Test
    void parsesUrlSetAndOrdersNewestFirst() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <url><loc>https://example.com/old</loc><lastmod>2026-07-20</lastmod></url>
                  <url><loc>https://example.com/new</loc><lastmod>2026-07-22T01:02:03Z</lastmod></url>
                  <url><loc>https://example.com/no-date</loc></url>
                </urlset>
                """;

        var entries = parser.parse(xml);

        assertThat(entries).extracting(SitemapParser.SitemapEntry::url)
                .containsExactly("https://example.com/new", "https://example.com/old", "https://example.com/no-date");
        assertThat(entries.get(0).lastModified()).isNotNull();
        assertThat(entries.get(2).lastModified()).isNull();
    }

    @Test
    void parsesSitemapIndexWithNamespace() {
        String xml = """
                <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <sitemap><loc>https://example.com/sitemap-1.xml</loc><lastmod>2026-07-21</lastmod></sitemap>
                  <sitemap><loc>https://example.com/sitemap-2.xml</loc><lastmod>2026-07-22</lastmod></sitemap>
                </sitemapindex>
                """;

        assertThat(parser.parse(xml)).extracting(SitemapParser.SitemapEntry::url)
                .containsExactly("https://example.com/sitemap-2.xml", "https://example.com/sitemap-1.xml");
    }

    @Test
    void malformedOrBlankXmlReturnsEmptyList() {
        assertThat(parser.parse(null)).isEmpty();
        assertThat(parser.parse(" ")).isEmpty();
        assertThat(parser.parse("not xml at all")).isEmpty();
    }
}
