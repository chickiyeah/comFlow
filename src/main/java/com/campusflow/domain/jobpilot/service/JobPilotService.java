package com.campusflow.domain.jobpilot.service;

import com.campusflow.domain.jobpilot.dto.*;
import com.campusflow.domain.jobpilot.dto.JobPilotRequests.GenerateRequest;
import com.campusflow.domain.jobpilot.dto.JobPilotRequests.MatchRequest;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * JobPilot 파이프라인 오케스트레이션 facade.
 * profile / matchReport 가 요청에 없으면 서버가 채워 [매칭]→[생성]을 잇는다.
 */
@Service
@RequiredArgsConstructor
public class JobPilotService {

    private final ProfileAssembler profileAssembler;
    private final JobMatcherService matcherService;
    private final CoverLetterGeneratorService generatorService;

    public MatchReport match(String username, MatchRequest req) {
        if (req == null || req.job() == null) throw new BusinessException(ErrorCode.INVALID_INPUT);
        StudentProfileDto profile = req.profile() != null ? req.profile()
                : profileAssembler.assemble(username);
        return matcherService.match(req.job(), profile);
    }

    public GenerateReport generate(String username, GenerateRequest req) {
        if (req == null || req.job() == null) throw new BusinessException(ErrorCode.INVALID_INPUT);
        StudentProfileDto profile = req.profile() != null ? req.profile()
                : profileAssembler.assemble(username);
        MatchReport match = req.matchReport() != null ? req.matchReport()
                : matcherService.match(req.job(), profile);
        return generatorService.generate(req.job(), profile, match);
    }
}
