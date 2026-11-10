package com.raulcedroj.football_predictions.auth;

import com.raulcedroj.football_predictions.auth.dto.*;
import com.raulcedroj.football_predictions.user.dto.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Registration and login")
@SecurityRequirements
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(authService.register(request));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verify-email")
    public TokenResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/verify-email/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerificationCode(@Valid @RequestBody ResendCodeRequest request) {
        authService.resendVerificationCode(request);
    }

    @GetMapping("/display-name-availability")
    public DisplayNameAvailability displayNameAvailability(@RequestParam String name) {
        return new DisplayNameAvailability(authService.isDisplayNameAvailable(name));
    }
}