package com.campusflow.domain.career.service.jobfeed;

import com.campusflow.domain.career.dto.JobSearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JsonLdJobPostingParser {

    private final ObjectMapper objectMapper;

    public JsonLdJobPostingParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<JobSearchResult> parse(String html, String pageUrl, String source) {
        if (html == null || html.isBlank()) return Optional.empty();

        for (Element script : Jsoup.parse(html).select("script[type=application/ld+json]")) {
            try {
                JsonNode posting = findJobPosting(objectMapper.readTree(script.data()));
                if (posting != null) return toResult(posting, pageUrl, source);
            } catch (Exception ignored) {
                // A page can contain unrelated or malformed structured-data blocks.
            }
        }
        return Optional.empty();
    }

    private JsonNode findJobPosting(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findJobPosting(child);
                if (found != null) return found;
            }
            return null;
        }
        if (!node.isObject()) return null;

        JsonNode type = node.get("@type");
        if ((type != null && type.isTextual() && "JobPosting".equalsIgnoreCase(type.asText()))
                || (type != null && type.isArray() && containsType(type, "JobPosting"))) {
            return node;
        }
        return findJobPosting(node.get("@graph"));
    }

    private boolean containsType(JsonNode types, String expected) {
        for (JsonNode type : types) {
            if (expected.equalsIgnoreCase(type.asText())) return true;
        }
        return false;
    }

    private Optional<JobSearchResult> toResult(JsonNode posting, String pageUrl, String source) {
        String title = text(posting, "title");
        String company = text(posting.path("hiringOrganization"), "name");
        if (title == null || company == null) return Optional.empty();

        String url = firstNonBlank(text(posting, "url"), pageUrl);
        String id = text(posting.path("identifier"), "value");
        if (id == null) id = lastPathSegment(url);

        return Optional.of(new JobSearchResult(
                id,
                title,
                company,
                location(posting.get("jobLocation")),
                url,
                date(text(posting, "validThrough")),
                jobType(posting),
                null,
                source
        ));
    }

    private String location(JsonNode locationNode) {
        if (locationNode == null || locationNode.isNull()) return null;
        List<JsonNode> locations = new ArrayList<>();
        if (locationNode.isArray()) locationNode.forEach(locations::add);
        else locations.add(locationNode);

        Set<String> parts = new LinkedHashSet<>();
        for (JsonNode location : locations) {
            JsonNode address = location.path("address");
            add(parts, text(address, "addressRegion"));
            add(parts, text(address, "addressLocality"));
        }
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private String jobType(JsonNode posting) {
        JsonNode experience = posting.get("experienceRequirements");
        if (experience != null && experience.isTextual()) return blankToNull(experience.asText());
        if (experience != null && experience.isObject()) {
            int months = experience.path("monthsOfExperience").asInt(-1);
            if (months == 0) return "신입";
            if (months > 0) return "경력 " + Math.max(1, months / 12) + "년 이상";
        }
        return switch (posting.path("employmentType").asText("")) {
            case "FULL_TIME" -> "정규직";
            case "PART_TIME" -> "파트타임";
            case "CONTRACTOR" -> "계약직";
            case "INTERN" -> "인턴";
            default -> null;
        };
    }

    private LocalDate date(String value) {
        if (value == null || value.length() < 10) return null;
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String lastPathSegment(String url) {
        if (url == null) return null;
        try {
            String path = URI.create(url).getPath();
            if (path == null || path.isBlank()) return null;
            String[] parts = path.split("/");
            return parts.length == 0 ? null : blankToNull(parts[parts.length - 1]);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        return blankToNull(node.path(field).asText(null));
    }

    private void add(Set<String> values, String value) {
        if (value != null) values.add(value);
    }

    private String firstNonBlank(String first, String second) {
        return first != null ? first : blankToNull(second);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
