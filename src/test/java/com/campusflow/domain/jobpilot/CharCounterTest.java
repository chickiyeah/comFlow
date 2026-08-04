package com.campusflow.domain.jobpilot;

import com.campusflow.domain.jobpilot.util.CharCounter;
import com.campusflow.domain.jobpilot.util.CharCounter.LengthVerdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 글자수 카운터 — 공백포함/제외, 85~98% 가드 (참조구현 counter.py 동작 검증). */
class CharCounterTest {

    @Test
    void 공백포함_제외_카운트() {
        String t = "안녕 하세요\n반갑";   // 글자 7 + 공백 1 + 개행 1 = 9
        assertThat(CharCounter.count(t, true)).isEqualTo(9);   // 전체 문자 수
        assertThat(CharCounter.count(t, false)).isEqualTo(7);  // 공백·개행 제외
    }

    @Test
    void charLimitType_해석() {
        assertThat(CharCounter.resolveIncludeSpaces("공백제외")).isFalse();
        assertThat(CharCounter.resolveIncludeSpaces("공백포함")).isTrue();
        assertThat(CharCounter.resolveIncludeSpaces(null)).isTrue();   // 미명시 → 공백포함
    }

    @Test
    void 초과_적정_미달_판정() {
        // 제한 100자, 적정 구간 85~98
        assertThat(CharCounter.check("가".repeat(101), 100, "공백포함").status()).isEqualTo("over");
        assertThat(CharCounter.check("가".repeat(90), 100, "공백포함").status()).isEqualTo("ok");
        assertThat(CharCounter.check("가".repeat(50), 100, "공백포함").status()).isEqualTo("short");
    }

    @Test
    void 목표구간_85_98퍼센트() {
        LengthVerdict v = CharCounter.check("", 1000, null);
        assertThat(v.targetMin()).isEqualTo(850);
        assertThat(v.targetMax()).isEqualTo(980);
        assertThat(v.needsRetry()).isTrue();   // 0자 → short
    }
}
