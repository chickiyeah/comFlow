package com.campusflow.domain.user.service;

import com.campusflow.domain.user.entity.GitHubToken;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.GitHubTokenRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GitHubTokenService {

    private final GitHubTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public boolean hasToken(String username) {
        Long uid = getUser(username).getId();
        return tokenRepository.findByUserId(uid).isPresent();
    }

    public Optional<String> getToken(String username) {
        Long uid = getUser(username).getId();
        return tokenRepository.findByUserId(uid).map(GitHubToken::getToken);
    }

    @Transactional
    public void saveToken(String username, String token) {
        User user = getUser(username);
        tokenRepository.findByUserId(user.getId()).ifPresentOrElse(
                t -> t.updateToken(token),
                () -> tokenRepository.save(GitHubToken.builder().user(user).token(token).build())
        );
    }

    @Transactional
    public void deleteToken(String username) {
        Long uid = getUser(username).getId();
        tokenRepository.findByUserId(uid).ifPresent(tokenRepository::delete);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
