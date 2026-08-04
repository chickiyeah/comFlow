package com.campusflow.domain.career.service.jobfeed;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SitemapParser {

    public record SitemapEntry(String url, Instant lastModified) {}

    public List<SitemapEntry> parse(String xml) {
        if (xml == null || xml.isBlank()) return List.of();

        Document document = Jsoup.parse(xml, "", Parser.xmlParser());
        List<SitemapEntry> entries = new ArrayList<>();
        for (Element loc : document.select("loc")) {
            String url = loc.text().trim();
            if (url.isBlank()) continue;
            Element parent = loc.parent();
            Element lastmod = parent == null ? null : parent.selectFirst("lastmod");
            entries.add(new SitemapEntry(url, parseInstant(lastmod == null ? null : lastmod.text())));
        }
        entries.sort(Comparator.comparing(SitemapEntry::lastModified,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return entries;
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value.trim()).atStartOfDay().toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }
}
