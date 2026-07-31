package com.campusflow.domain.resume.util;

/** reasoning 모델이 앞뒤에 설명을 붙여도 JSON 본문만 뽑아낸다(PortfolioAiGenerator 패턴). */
public final class JsonExtract {
    private JsonExtract() {}

    public static String array(String raw) { return between(raw, '[', ']'); }
    public static String object(String raw) { return between(raw, '{', '}'); }

    private static String between(String raw, char open, char close) {
        if (raw == null) return "";
        int s = raw.indexOf(open), e = raw.lastIndexOf(close);
        return (s >= 0 && e > s) ? raw.substring(s, e + 1) : raw;
    }
}
