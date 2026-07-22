package com.campusflow.domain.material.highlight.service;

import com.campusflow.domain.classroom.service.ClassAccessService;
import com.campusflow.domain.material.entity.Material;
import com.campusflow.domain.material.highlight.dto.HighlightResponse;
import com.campusflow.domain.material.highlight.dto.HighlightStartRequest;
import com.campusflow.domain.material.highlight.entity.HighlightStatus;
import com.campusflow.domain.material.highlight.entity.MaterialHighlightAnalysis;
import com.campusflow.domain.material.highlight.repository.MaterialHighlightAnalysisRepository;
import com.campusflow.domain.material.highlight.repository.MaterialHighlightPageRepository;
import com.campusflow.domain.material.highlight.repository.MaterialPdfHighlightRepository;
import com.campusflow.domain.material.repository.MaterialRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * PDF 스마트 하이라이트(텍스트 전용). 클라이언트가 페이지 텍스트를 제출하면 분석을 만들고
 * 커밋 후 비동기 처리기를 기동한다. 동일 fingerprint의 완료 분석이 있으면 재계산 없이 반환.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialHighlightService {

    private final MaterialHighlightAnalysisRepository analysisRepository;
    private final MaterialHighlightPageRepository pageRepository;
    private final MaterialPdfHighlightRepository highlightRepository;
    private final MaterialRepository materialRepository;
    private final MaterialHighlightProcessor processor;
    private final ClassAccessService classAccess;

    public HighlightResponse get(String username, Long materialId) {
        Material material = loadAsMember(username, materialId);
        MaterialHighlightAnalysis analysis = analysisRepository
                .findFirstByMaterialIdOrderByIdDesc(material.getId())
                .orElse(null);
        if (analysis == null) {
            return new HighlightResponse(null, "NONE", 0, 0, null, List.of());
        }
        return HighlightResponse.of(analysis, currentHighlights(analysis));
    }

    @Transactional
    public HighlightResponse start(String username, Long materialId, HighlightStartRequest request) {
        Material material = loadAsMember(username, materialId);
        if (request.pages() == null || request.pages().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String fingerprint = fingerprint(request);

        MaterialHighlightAnalysis existing = analysisRepository
                .findFirstByMaterialIdOrderByIdDesc(material.getId()).orElse(null);
        if (existing != null && existing.getStatus() == HighlightStatus.READY
                && fingerprint.equals(existing.getFingerprint())) {
            return HighlightResponse.of(existing, currentHighlights(existing)); // dedupe
        }
        if (existing != null) {
            highlightRepository.deleteByAnalysisId(existing.getId());
            pageRepository.deleteByAnalysisId(existing.getId());
            analysisRepository.delete(existing);
        }

        MaterialHighlightAnalysis analysis = analysisRepository.save(MaterialHighlightAnalysis.builder()
                .material(material)
                .status(HighlightStatus.PROCESSING)
                .fingerprint(fingerprint)
                .totalPages(request.pages().size())
                .build());
        request.pages().forEach(p -> pageRepository.save(
                com.campusflow.domain.material.highlight.entity.MaterialHighlightPage.builder()
                        .analysis(analysis)
                        .pageNumber(p.pageNumber() != null ? p.pageNumber() : 0)
                        .sourceText(p.text())
                        .build()));

        triggerAfterCommit(analysis.getId());
        return HighlightResponse.of(analysis, List.of());
    }

    @Transactional
    public HighlightResponse retry(String username, Long materialId) {
        Material material = loadAsMember(username, materialId);
        MaterialHighlightAnalysis analysis = analysisRepository
                .findFirstByMaterialIdOrderByIdDesc(material.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (analysis.getStatus() == HighlightStatus.PROCESSING) {
            return HighlightResponse.of(analysis, List.of()); // 이미 진행 중
        }
        highlightRepository.deleteByAnalysisId(analysis.getId());
        analysis.resetForRetry();
        triggerAfterCommit(analysis.getId());
        return HighlightResponse.of(analysis, List.of());
    }

    // ── helpers ──────────────────────────────────────────────
    private List<com.campusflow.domain.material.highlight.entity.MaterialPdfHighlight> currentHighlights(
            MaterialHighlightAnalysis analysis) {
        return highlightRepository.findByAnalysisIdOrderByPageNumberAscDisplayOrderAsc(analysis.getId());
    }

    /** 트랜잭션 커밋 후 비동기 처리 기동 (커밋 전 기동 시 async 스레드가 미커밋 데이터를 못 봄). */
    private void triggerAfterCommit(Long analysisId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    processor.process(analysisId);
                }
            });
        } else {
            processor.process(analysisId);
        }
    }

    private String fingerprint(HighlightStartRequest request) {
        StringBuilder sb = new StringBuilder();
        request.pages().forEach(p -> sb.append(p.pageNumber()).append(':')
                .append(p.text() != null ? p.text() : "").append('\n'));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(sb.toString().hashCode());
        }
    }

    private Material loadAsMember(String username, Long materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        classAccess.requireMember(material.getClassRoom().getId(), username);
        return material;
    }
}
