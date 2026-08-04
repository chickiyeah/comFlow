package com.campusflow.domain.career.dto;

import java.util.List;

public record KeywordSuggestionResponse(String defaultKeyword, List<String> suggestions) {}
