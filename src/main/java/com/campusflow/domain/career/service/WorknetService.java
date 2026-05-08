package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.JobSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WorknetService {

    private static final String BASE_URL = "https://openapi.work.go.kr/opi/opi/opia/wantedApi.do";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestTemplate restTemplate;

    @Value("${worknet.api.key:}")
    private String apiKey;

    public WorknetService() {
        this.restTemplate = new RestTemplate();
    }

    public List<JobSearchResult> searchJobs(String keyword, int page) {
        if (apiKey.isBlank()) {
            log.warn("워크넷 API 키 미설정 — 빈 결과 반환");
            return List.of();
        }
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("authKey", apiKey)
                    .queryParam("callTp", "L")
                    .queryParam("returnType", "XML")
                    .queryParam("startPage", page)
                    .queryParam("display", 20)
                    .queryParam("keyword", keyword)
                    .queryParam("occupation", "09")  // IT/컴퓨터
                    .build().toUriString();

            String xml = restTemplate.getForObject(uri, String.class);
            return parseWorknetXml(xml);
        } catch (Exception e) {
            log.error("워크넷 API 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<JobSearchResult> parseWorknetXml(String xml) throws Exception {
        List<JobSearchResult> results = new ArrayList<>();
        if (xml == null || xml.isBlank()) return results;

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        NodeList items = doc.getElementsByTagName("wanted");

        for (int i = 0; i < items.getLength(); i++) {
            Element el = (Element) items.item(i);
            String deadlineStr = getText(el, "deadline");
            LocalDate deadline = null;
            if (deadlineStr != null && deadlineStr.matches("\\d{8}")) {
                deadline = LocalDate.parse(deadlineStr, FMT);
            }
            results.add(new JobSearchResult(
                    getText(el, "wantedAuthNo"),
                    getText(el, "title"),
                    getText(el, "company"),
                    getText(el, "workRegion"),
                    getText(el, "wantedInfoUrl"),
                    deadline,
                    getText(el, "empTpNm"),
                    getText(el, "salTpNm"),
                    "워크넷"
            ));
        }
        return results;
    }

    private String getText(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        return nl.item(0).getTextContent().trim();
    }
}
