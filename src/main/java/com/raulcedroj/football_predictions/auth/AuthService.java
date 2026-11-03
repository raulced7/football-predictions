package com.raulcedroj.football_predictions.auth;

import com.raulcedroj.football_predictions.security.JwtProperties;
import com.raulcedroj.football_predictions.security.TokenService;
import com.raulcedroj.football_predictions.user.UserEntity;
import com.raulcedroj.football_predictions.user.UserRepository;
import com.raulcedroj.football_predictions.auth.dto.LoginRequest;
import com.raulcedroj.football_predictions.auth.dto.RegisterRequest;
import com.raulcedroj.football_predictions.auth.dto.TokenResponse;
import com.raulcedroj.football_predictions.auth.exception.EmailAlreadyRegisteredException;
import com.raulcedroj.football_predictions.auth.exception.InvalidCredentialsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public UserEntity register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        try {
            return userRepository.saveAndFlush(new UserEntity(email, request.displayName().trim(),
                    passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return new TokenResponse(tokenService.createToken(user), "Bearer",
                jwtProperties.expiration().toSeconds());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}