package com.campusflow.domain.portfolio.controller;

import com.campusflow.domain.portfolio.dto.GithubGenerateRequest;
import com.campusflow.domain.portfolio.dto.PortfolioAiDraft;
import com.campusflow.domain.portfolio.dto.PortfolioRequest;
import com.campusflow.domain.portfolio.dto.PortfolioResponse;
import com.campusflow.domain.portfolio.service.FileParserService;
import com.campusflow.domain.portfolio.service.GitHubService;
import com.campusflow.domain.portfolio.service.PortfolioAiGeneratorService;
import com.campusflow.domain.portfolio.service.PortfolioService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final GitHubService gitHubService;
    private final FileParserService fileParserService;
    private final PortfolioAiGeneratorService aiGeneratorService;

    @GetMapping
    public ApiResponse<List<PortfolioResponse>> getMyPortfolios(@AuthenticationPrincipal UserDetails user) {
        return ApiResponse.ok(portfolioService.getMyPortfolios(user.getUsername()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PortfolioResponse> create(@AuthenticationPrincipal UserDetails user,
                                                  @Valid @RequestBody PortfolioRequest request) {
        return ApiResponse.ok(portfolioService.create(user.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PortfolioResponse> update(@AuthenticationPrincipal UserDetails user,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody PortfolioRequest request) {
        return ApiResponse.ok(portfolioService.update(user.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        portfolioService.delete(user.getUsername(), id);
    }

    /**
     * GitHub URL → AI 포트폴리오 초안 생성
     * POST /api/portfolios/generate/github
     * Body: { "githubUrl": "https://github.com/owner/repo" }
     */
    @PostMapping("/generate/github")
    public ApiResponse<PortfolioAiDraft> generateFromGithub(
            @Valid @RequestBody GithubGenerateRequest request) {
        String context = gitHubService.extractRepoContext(request.githubUrl());
        PortfolioAiDraft draft = aiGeneratorService.generateFromContext(
                context, "GITHUB", request.githubUrl());
        return ApiResponse.ok(draft);
    }

    /**
     * 파일(PPTX/PDF) → AI 포트폴리오 초안 생성
     * POST /api/portfolios/generate/file
     * Multipart: file
     */
    @PostMapping("/generate/file")
    public ApiResponse<PortfolioAiDraft> generateFromFile(
            @RequestParam MultipartFile file) throws IOException {
        String context = fileParserService.extractText(file);
        PortfolioAiDraft draft = aiGeneratorService.generateFromContext(
                context, "FILE", null);
        return ApiResponse.ok(draft);
    }
}
