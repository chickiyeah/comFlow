package com.campusflow.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * ChromaDB REST API 연동 — 학과 내부 문서 벡터 검색
 * ChromaDB v0.4+ HTTP API 기준
 */
@Service
@RequiredArgsConstructor
public class ChromaDbService {

    @Value("${chroma.host}")
    private String chromaHost;

    @Value("${chroma.port}")
    private int chromaPort;

    private static final String COLLECTION_NAME = "campus_docs";

    public List<String> searchRelevantDocs(String query, int nResults) {
        String baseUrl = "http://" + chromaHost + ":" + chromaPort;
        RestClient client = RestClient.create(baseUrl);

        Map<String, Object> body = Map.of(
                "query_texts", List.of(query),
                "n_results", nResults,
                "include", List.of("documents")
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = client.post()
                .uri("/api/v1/collections/" + COLLECTION_NAME + "/query")
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) return List.of();

        @SuppressWarnings("unchecked")
        List<List<String>> documents = (List<List<String>>) response.get("documents");
        return documents != null && !documents.isEmpty() ? documents.get(0) : List.of();
    }
}
