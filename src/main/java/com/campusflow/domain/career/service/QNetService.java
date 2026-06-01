package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
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

@Slf4j
@Service
public class QNetService {

    @Value("${qnet.api.key:}")
    private String apiKey;

    private static final String SCHD_URL = "http://openapi.q-net.or.kr/api/service/rest/InquiryTestInformationNTQSVC/getPEList";
    private static final String LIST_URL = "http://openapi.q-net.or.kr/api/service/rest/InquiryListNationalQualifcationSVC/getList";
    private static final String INFO_URL = "http://openapi.q-net.or.kr/api/service/rest/InquiryQualInfo/getList";
    private static final String AREA_URL = "http://openapi.q-net.or.kr/api/service/rest/InquiryExamAreaSVC/getList";

    @Cacheable(value = "certSchedules", key = "(#keyword ?: 'all') + '_' + #year")
    public List<CertExamSchedule> getSchedules(String keyword, int year) {
        if (notReady()) return List.of();
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(SCHD_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 100)
                    .build(false).encode().toUriString();
            byte[] xml = RestClient.create().get().uri(uri).retrieve().body(byte[].class);
            return parseSchedules(xml, keyword);
        } catch (Exception e) {
            log.error("QNet 시험일정 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    @Cacheable(value = "qualList", key = "#keyword ?: 'all'")
    public List<QualificationItem> searchQualifications(String keyword) {
        if (notReady()) return List.of();
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(LIST_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 100)
                    .build(false).encode().toUriString();
            byte[] xml = RestClient.create().get().uri(uri).retrieve().body(byte[].class);
            return parseQualificationList(xml, keyword);
        } catch (Exception e) {
            log.error("QNet 종목목록 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    @Cacheable(value = "qualDetail", key = "(#jmCd ?: 'x') + '_' + (#qualgbCd ?: 'x')")
    public List<QualificationDetail> getQualificationDetail(String jmCd, String qualgbCd) {
        if (notReady()) return List.of();
        try {
            var builder = UriComponentsBuilder.fromHttpUrl(INFO_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 10);
            if (jmCd != null && !jmCd.isBlank()) builder.queryParam("jmCd", jmCd);
            if (qualgbCd != null && !qualgbCd.isBlank()) builder.queryParam("qualgbCd", qualgbCd);
            byte[] xml = RestClient.create().get().uri(builder.build(false).encode().toUriString()).retrieve().body(byte[].class);
            return parseQualificationDetail(xml);
        } catch (Exception e) {
            log.error("QNet 자격정보 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    @Cacheable(value = "examLocations", key = "#brchCd ?: 'all'")
    public List<ExamLocation> getExamLocations(String brchCd) {
        if (notReady()) return List.of();
        try {
            var builder = UriComponentsBuilder.fromHttpUrl(AREA_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 50);
            if (brchCd != null && !brchCd.isBlank()) builder.queryParam("brchCd", brchCd);
            byte[] xml = RestClient.create().get().uri(builder.build(false).encode().toUriString()).retrieve().body(byte[].class);
            return parseExamLocations(xml);
        } catch (Exception e) {
            log.error("QNet 시험장소 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<CertExamSchedule> parseSchedules(byte[] xml, String keyword) throws Exception {
        List<CertExamSchedule> results = new ArrayList<>();
        Document doc = parseXml(xml);
        NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element el = (Element) items.item(i);
            String desc = getText(el, "description");
            if (desc == null) continue;
            if (keyword != null && !keyword.isBlank() && !desc.contains(keyword)) continue;
            results.add(new CertExamSchedule(
                    desc, desc,
                    range(getText(el, "docregstartdt"), getText(el, "docregenddt")),
                    getText(el, "docpassdt"),
                    range(getText(el, "pracexamstartdt"), getText(el, "pracexamenddt")),
                    getText(el, "pracpassdt"),
                    "큐넷(Q-Net)"
            ));
        }
        return results;
    }

    private List<QualificationItem> parseQualificationList(byte[] xml, String keyword) throws Exception {
        List<QualificationItem> results = new ArrayList<>();
        Document doc = parseXml(xml);
        NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element el = (Element) items.item(i);
            String name = getText(el, "jmfldnm");
            if (name == null) name = getText(el, "jmNm");
            if (name == null) continue;
            if (keyword != null && !keyword.isBlank() && !name.contains(keyword)) continue;
            results.add(new QualificationItem(
                    getText(el, "seriesCd"),
                    getText(el, "jmcd") != null ? getText(el, "jmcd") : getText(el, "jmCd"),
                    name,
                    getText(el, "qualgbCd"),
                    getText(el, "mdobligfldnm"),
                    "한국산업인력공단"
            ));
        }
        return results;
    }

    private List<QualificationDetail> parseQualificationDetail(byte[] xml) throws Exception {
        List<QualificationDetail> results = new ArrayList<>();
        Document doc = parseXml(xml);
        NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element el = (Element) items.item(i);
            results.add(new QualificationDetail(
                    getText(el, "jmNm"), getText(el, "qualgbNm"), getText(el, "engJmNm"),
                    getText(el, "relatedDept"), getText(el, "applyQual"), getText(el, "examMethod"),
                    getText(el, "passStandard"), getText(el, "feeWritten"), getText(el, "feePractical")
            ));
        }
        return results;
    }

    private List<ExamLocation> parseExamLocations(byte[] xml) throws Exception {
        List<ExamLocation> results = new ArrayList<>();
        Document doc = parseXml(xml);
        NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element el = (Element) items.item(i);
            results.add(new ExamLocation(
                    getText(el, "brchNm"), getText(el, "placNm"),
                    getText(el, "addr"), getText(el, "telNo")
            ));
        }
        return results;
    }

    private Document parseXml(byte[] xml) throws Exception {
        if (xml == null || xml.length == 0) throw new IllegalArgumentException("빈 응답");
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml));
    }

    private String getText(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String val = nl.item(0).getTextContent().trim();
        return val.isEmpty() ? null : val;
    }

    private String range(String start, String end) {
        if (start == null && end == null) return "-";
        return (start != null ? formatDate(start) : "") + " ~ " + (end != null ? formatDate(end) : "");
    }

    private String formatDate(String d) {
        if (d == null || d.length() != 8) return d != null ? d : "";
        return d.substring(0, 4) + "-" + d.substring(4, 6) + "-" + d.substring(6, 8);
    }

    private boolean notReady() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("큐넷 API 키 미설정");
            return true;
        }
        return false;
    }
}
