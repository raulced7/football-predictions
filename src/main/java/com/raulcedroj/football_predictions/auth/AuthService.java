package com.raulcedroj.football_predictions.auth;

import com.raulcedroj.football_predictions.auth.exception.DisplayNameTakenException;
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
        String displayName = request.displayName().trim();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        if (userRepository.existsByDisplayName(displayName)) {
            throw new DisplayNameTakenException();
        }
        try {
            return userRepository.saveAndFlush(new UserEntity(email, displayName,
                    passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException e) {
            if (isDisplayNameConflict(e)) {
                throw new DisplayNameTakenException();
            }
            throw new EmailAlreadyRegisteredException();
        }
    }

    @Transactional(readOnly = true)
    public boolean isDisplayNameAvailable(String displayName) {
        return !userRepository.existsByDisplayName(displayName.trim());
    }

    private boolean isDisplayNameConflict(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains("uq_app_user_display_name");
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