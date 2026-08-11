package com.campusflow.domain.jobpilot.util;

/**
 * 글자수 카운터 (참조구현 generator/counter.py 포팅).
 *
 * 한국 자소서는 '공백 포함'과 '공백 제외' 기준이 갈린다.
 * 목표 분량 정책:
 *   - 제한의 85~98% 사이를 '적정'으로 본다.
 *   - 100% 초과는 무조건 압축, 85% 미만은 확장.
 *   - 기준이 명시 안 되면 공백포함으로 맞춘다(더 안전).
 */
public final class CharCounter {

    public static final double GOOD_MIN_RATIO = 0.85;
    public static final double GOOD_MAX_RATIO = 0.98;

    private CharCounter() {}

    /** 글자수 계산. includeSpaces=false 면 공백·개행 제외. */
    public static int count(String text, boolean includeSpaces) {
        if (text == null) return 0;
        if (includeSpaces) return text.length();
        return text.replaceAll("\\s", "").length();
    }

    /** charLimitType('공백포함'|'공백제외'|null) → includeSpaces 불리언. */
    public static boolean resolveIncludeSpaces(String charLimitType) {
        // '제외'가 명시된 경우만 공백 제외, 그 외(미명시/'공백포함')는 공백 포함
        return charLimitType == null || !charLimitType.contains("제외");
    }

    /** 텍스트가 글자수 제한에 맞는지 판정. */
    public static LengthVerdict check(String text, int limit, String charLimitType) {
        boolean includeSpaces = resolveIncludeSpaces(charLimitType);
        int count = count(text, includeSpaces);
        int targetMin = (int) (limit * GOOD_MIN_RATIO);
        int targetMax = (int) (limit * GOOD_MAX_RATIO);

        String status;
        if (count > limit)            status = "over";
        else if (count < targetMin)   status = "short";
        else                          status = "ok";

        return new LengthVerdict(count, limit, includeSpaces, status, targetMin, targetMax);
    }

    /** 글자수 판정 결과. */
    public record LengthVerdict(
            int count,
            int limit,
            boolean includeSpaces,
            String status,      // 'ok' | 'over' | 'short'
            int targetMin,
            int targetMax
    ) {
        public boolean needsRetry() { return !"ok".equals(status); }
    }
}
