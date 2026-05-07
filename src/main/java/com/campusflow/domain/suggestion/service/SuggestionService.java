package com.campusflow.domain.suggestion.service;

import com.campusflow.domain.suggestion.dto.SuggestionRequest;
import com.campusflow.domain.suggestion.dto.SuggestionResponse;
import com.campusflow.domain.suggestion.entity.Suggestion;
import com.campusflow.domain.suggestion.entity.SuggestionStatus;
import com.campusflow.domain.suggestion.repository.SuggestionRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;

    @Transactional
    public SuggestionResponse submit(SuggestionRequest request) {
        Suggestion s = Suggestion.builder()
                .category(request.category())
                .content(request.content())
                .build();
        return SuggestionResponse.from(suggestionRepository.save(s));
    }

    public Page<SuggestionResponse> getAll(Pageable pageable) {
        return suggestionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(SuggestionResponse::from);
    }

    @Transactional
    public SuggestionResponse reply(Long id, String reply, SuggestionStatus status) {
        Suggestion s = suggestionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        s.reply(reply, status);
        return SuggestionResponse.from(s);
    }
}
