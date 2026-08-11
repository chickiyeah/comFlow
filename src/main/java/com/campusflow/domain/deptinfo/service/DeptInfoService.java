package com.campusflow.domain.deptinfo.service;

import com.campusflow.domain.deptinfo.dto.DeptInfoRequest;
import com.campusflow.domain.deptinfo.dto.DeptInfoResponse;
import com.campusflow.domain.deptinfo.entity.DeptInfo;
import com.campusflow.domain.deptinfo.entity.DeptInfoCategory;
import com.campusflow.domain.deptinfo.repository.DeptInfoRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeptInfoService {

    private static final int MAX_CONTEXT_ENTRIES = 4;

    private final DeptInfoRepository repository;

    // ── 관리자 CRUD ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DeptInfoResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(DeptInfoResponse::from).toList();
    }

    @Transactional
    public DeptInfoResponse create(DeptInfoRequest req) {
        DeptInfo saved = repository.save(DeptInfo.builder()
                .category(req.category()).title(req.title()).content(req.content())
                .keywords(req.keywords()).active(req.active()).build());
        return DeptInfoResponse.from(saved);
    }

    @Transactional
    public DeptInfoResponse update(Long id, DeptInfoRequest req) {
        DeptInfo d = repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        d.update(req.category(), req.title(), req.content(), req.keywords(), req.active());
        return DeptInfoResponse.from(d);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new BusinessException(ErrorCode.NOT_FOUND);
        repository.deleteById(id);
    }

    /**
     * 질문과 관련된 학과 내부정보를 마크다운 컨텍스트로 반환. 매칭 없으면 빈 문자열.
     * 매칭: 제목 포함 / 분류 키워드(terms) 포함 / 등록 keywords 포함.
     */
    @Transactional(readOnly = true)
    public String buildContext(String query) {
        if (query == null || query.isBlank()) return "";
        String q = query.toLowerCase();
        List<DeptInfo> matched = new ArrayList<>();
        for (DeptInfo d : repository.findByActiveTrueOrderByCategoryAsc()) {
            if (isMatch(d, q)) matched.add(d);
            if (matched.size() >= MAX_CONTEXT_ENTRIES) break;
        }
        if (matched.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (DeptInfo d : matched) {
            sb.append("## [").append(d.getCategory().getLabel()).append("] ")
              .append(d.getTitle()).append('\n')
              .append(d.getContent()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private boolean isMatch(DeptInfo d, String lowerQuery) {
        if (d.getTitle() != null && lowerQuery.contains(d.getTitle().toLowerCase())) return true;
        for (String term : d.getCategory().getTerms()) {
            if (lowerQuery.contains(term.toLowerCase())) return true;
        }
        if (d.getKeywords() != null) {
            for (String kw : d.getKeywords().split(",")) {
                String k = kw.trim().toLowerCase();
                if (!k.isEmpty() && lowerQuery.contains(k)) return true;
            }
        }
        return false;
    }

    // ── 초기 샘플 시드 (관리자가 실제 내용으로 교체) ──────────
    @PostConstruct
    @Transactional
    public void seed() {
        if (repository.count() > 0) return;
        repository.save(DeptInfo.builder()
                .category(DeptInfoCategory.ADMISSION)
                .title("컴퓨터정보과 입시 안내 (샘플)")
                .content("※ 샘플 데이터입니다. 관리자 페이지에서 실제 입시 정보로 수정하세요.\n"
                        + "- 모집 인원/전형 유형/지원 자격/일정 등을 여기에 입력하면 컴정이 챗이 이 내용을 근거로 답합니다.")
                .keywords("수시,정시,경쟁률")
                .active(true).build());
        repository.save(DeptInfo.builder()
                .category(DeptInfoCategory.FACULTY)
                .title("컴퓨터정보과 교수진 안내 (샘플)")
                .content("※ 샘플 데이터입니다. 관리자 페이지에서 실제 교수진 정보로 수정하세요.\n"
                        + "- 교수님 성함/담당 과목/연구분야/연락처 등을 입력하세요.")
                .keywords("학과장,연구실")
                .active(true).build());
        log.info("[DeptInfo] 샘플 학과 내부정보 2건 시드 완료");
    }
}
