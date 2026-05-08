package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.CertExamSchedule;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class QNetService {

    private static final String BASE_URL = "https://www.data.go.kr/api/15113723/v1/uddi:national-technical-qualification-test-schedule";

    private final RestTemplate restTemplate;

    @Value("${qnet.api.key:}")
    private String apiKey;

    public QNetService() {
        this.restTemplate = new RestTemplate();
    }

    public List<CertExamSchedule> getSchedules(String keyword, int year) {
        if (apiKey.isBlank()) {
            log.warn("큐넷 API 키 미설정 — 빈 결과 반환");
            return List.of();
        }
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 50)
                    .queryParam("implYy", year)
                    .queryParam("qualgbCd", "T")  // 기술자격
                    .build(true).toUriString();

            String xml = restTemplate.getForObject(uri, String.class);
            return parseQNetXml(xml, keyword);
        } catch (Exception e) {
            log.error("큐넷 API 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<CertExamSchedule> parseQNetXml(String xml, String keyword) throws Exception {
        List<CertExamSchedule> results = new ArrayList<>();
        if (xml == null || xml.isBlank()) return results;

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        NodeList items = doc.getElementsByTagName("item");

        for (int i = 0; i < items.getLength(); i++) {
            Element el = (Element) items.item(i);
            String name = getText(el, "jmNm");
            if (name == null) continue;
            if (keyword != null && !keyword.isBlank() && !name.contains(keyword)) continue;

            results.add(new CertExamSchedule(
                    getText(el, "implSeq") + "회차 " + getText(el, "implYy") + "년",
                    name,
                    getText(el, "docRegStartDt") + " ~ " + getText(el, "docRegEndDt"),
                    getText(el, "docPassDt"),
                    getText(el, "pracRegStartDt") + " ~ " + getText(el, "pracRegEndDt"),
                    getText(el, "pracPassDt"),
                    "큐넷(Q-Net)"
            ));
        }
        return results;
    }

    private String getText(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        return nl.item(0).getTextContent().trim();
    }
}
