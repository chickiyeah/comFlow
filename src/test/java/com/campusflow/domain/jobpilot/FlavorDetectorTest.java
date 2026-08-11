package com.campusflow.domain.jobpilot;

import com.campusflow.domain.jobpilot.util.FlavorDetector;
import com.campusflow.domain.jobpilot.util.FlavorDetector.Flavor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 직무 색 판별 — 참조구현 flavor.py 의 검증 케이스 재현. */
class FlavorDetectorTest {

    @Test
    void mlops_판별() {
        Flavor f = FlavorDetector.detect("MLOps 엔지니어", List.of("모델 서빙", "RAG"), List.of());
        assertThat(f.key()).isEqualTo("mlops");
    }

    @Test
    void infra_판별() {
        Flavor f = FlavorDetector.detect("시스템 엔지니어", List.of("리눅스", "컨테이너"), List.of("서버 관리"));
        assertThat(f.key()).isEqualTo("infra");
    }

    @Test
    void platform_판별() {
        Flavor f = FlavorDetector.detect("AI 플랫폼 엔지니어", List.of("Kubernetes", "OpenShift"), List.of());
        assertThat(f.key()).isEqualTo("platform");
    }

    @Test
    void ops_판별() {
        Flavor f = FlavorDetector.detect("정산 운영지원", List.of(), List.of("데이터 관리"));
        assertThat(f.key()).isEqualTo("ops");
    }

    @Test
    void 미매칭시_general() {
        Flavor f = FlavorDetector.detect("바리스타", List.of("커피"), List.of());
        assertThat(f.key()).isEqualTo("general");
    }
}
