package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.BlindRecruitCompany;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 한국산업인력공단_블라인드 채용 기업 API (data.go.kr 15041620)
 * NCS 기반 블라인드 채용 도입 기관/기업 목록 — 현재 403 (별도 권한 신청 필요)
 */
@Slf4j
@Service
public class BlindRecruitService {

    private static final String BASE_URL = "http://apis.data.go.kr/B490007/ncs.go.kr/api/openapi16.do";

    @Value("${blind.recruit.api.key:}")
    private String apiKey;

    public List<BlindRecruitCompany> search(String keyword, int page) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("블라인드 채용 API 키 미설정");
            return List.of();
        }
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", page)
                    .queryParam("numOfRows", 30)
                    .build(true).toUriString();

            String xml = RestClient.create().get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            return parse(xml, keyword);
        } catch (Exception e) {
            log.error("블라인드 채용 API 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<BlindRecruitCompany> parse(String xml, String keyword) throws Exception {
        List<BlindRecruitCompany> results = new ArrayList<>();
        if (xml == null || xml.isBlank()) return results;

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        NodeList items = doc.getElementsByTagName("item");

        for (int i = 0; i < items.getLength(); i++) {
            Element el = (Element) items.item(i);
            String name = getText(el, "insttNm");
            if (name == null) continue;
            if (keyword != null && !keyword.isBlank() && !name.contains(keyword)) continue;
            results.add(new BlindRecruitCompany(
                    name,
                    getText(el, "ncsDivNm"),
                    getText(el, "recrutField"),
                    getText(el, "year")
            ));
        }
        return results;
    }

    private String getText(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String val = nl.item(0).getTextContent().trim();
        return val.isEmpty() ? null : val;
    }
}
