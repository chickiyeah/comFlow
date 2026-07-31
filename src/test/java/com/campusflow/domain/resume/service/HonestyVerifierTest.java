package com.campusflow.domain.resume.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HonestyVerifierTest {

    @Mock AiFacadeService ai;
    @InjectMocks HonestyVerifier verifier;

    @Test
    void 근거없는_문장이_감지되면_순화_재생성하고_로그를_남긴다() {
        String evidence = "[자격증] 정보처리기능사";
        String text = "저는 정보처리기사를 취득했고 대기업 3곳에서 근무했습니다.";

        // 1차 검증: 위반 2건 JSON 배열
        when(ai.ask(contains("정직성 검증관"), anyString()))
                .thenReturn("[\"정보처리기사 취득(근거 없음)\", \"대기업 3곳 근무(근거 없음)\"]");
        // 재생성: 근거 기반 순화본
        when(ai.ask(contains("순화"), anyString()))
                .thenReturn("저는 정보처리기능사를 취득하며 기초 역량을 다졌습니다.");

        HonestyVerifier.FixResult res = verifier.verifyAndFix("지원동기", text, evidence);

        assertThat(res.text()).contains("정보처리기능사");
        assertThat(res.text()).doesNotContain("대기업 3곳");
        assertThat(res.fixes()).isNotEmpty();
        assertThat(res.fixes().get(0).reason()).contains("근거 없음");
    }

    @Test
    void 위반이_없으면_원문을_그대로_반환한다() {
        when(ai.ask(contains("정직성 검증관"), anyString())).thenReturn("[]");
        HonestyVerifier.FixResult res = verifier.verifyAndFix("성장과정", "사실만 담긴 글", "[스킬] Java");
        assertThat(res.text()).isEqualTo("사실만 담긴 글");
        assertThat(res.fixes()).isEmpty();
    }
}
