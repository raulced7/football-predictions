package com.raulcedroj.football_predictions.auth;

import com.raulcedroj.football_predictions.auth.code.CodePurpose;
import com.raulcedroj.football_predictions.auth.code.EmailCodeService;
import com.raulcedroj.football_predictions.auth.code.InvalidCodeException;
import com.raulcedroj.football_predictions.auth.dto.*;
import com.raulcedroj.football_predictions.auth.exception.DisplayNameTakenException;
import com.raulcedroj.football_predictions.auth.exception.EmailNotVerifiedException;
import com.raulcedroj.football_predictions.security.JwtProperties;
import com.raulcedroj.football_predictions.security.TokenService;
import com.raulcedroj.football_predictions.user.UserEntity;
import com.raulcedroj.football_predictions.user.UserRepository;
import com.raulcedroj.football_predictions.auth.exception.EmailAlreadyRegisteredException;
import com.raulcedroj.football_predictions.auth.exception.InvalidCredentialsException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;
    private final EmailCodeService emailCodeService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, JwtProperties jwtProperties, EmailCodeService emailCodeService, ApplicationEventPublisher events, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
        this.emailCodeService = emailCodeService;
        this.events = events;
        this.clock = clock;
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
        UserEntity user;
        try {
            user = userRepository.saveAndFlush(new UserEntity(email, displayName,
                    passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException e) {
            if (isDisplayNameConflict(e)) {
                throw new DisplayNameTakenException();
            }
            throw new EmailAlreadyRegisteredException();
        }
        sendVerificationCode(user);
        return user;
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

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
        return createToken(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void sendVerificationCode(UserEntity user) {
        String code = emailCodeService.issue(user.getId(), CodePurpose.EMAIL_VERIFICATION);
        events.publishEvent(new VerificationCodeIssuedEvent(user.getEmail(), user.getDisplayName(), code));
    }

    @Transactional(noRollbackFor = InvalidCodeException.class)
    public TokenResponse verifyEmail(VerifyEmailRequest request) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(u -> !u.isEmailVerified())
                .orElseThrow(() -> new InvalidCodeException("El código no es válido. Pide uno nuevo."));

        emailCodeService.consume(user.getId(), CodePurpose.EMAIL_VERIFICATION, request.code());
        user.markEmailVerified(clock.instant());
        return createToken(user);
    }

    @Transactional
    public void resendVerificationCode(ResendCodeRequest request) {
        userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(user -> !user.isEmailVerified())
                .filter(user -> emailCodeService.canIssue(user.getId(), CodePurpose.EMAIL_VERIFICATION))
                .ifPresent(this::sendVerificationCode);
    }

    private TokenResponse createToken(UserEntity user) {
        return new TokenResponse(tokenService.createToken(user), "Bearer",
                jwtProperties.expiration().toSeconds());
    }
}