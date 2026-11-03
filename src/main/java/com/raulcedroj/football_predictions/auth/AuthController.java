package com.raulcedroj.football_predictions.auth;

import com.raulcedroj.football_predictions.auth.dto.LoginRequest;
import com.raulcedroj.football_predictions.auth.dto.RegisterRequest;
import com.raulcedroj.football_predictions.auth.dto.TokenResponse;
import com.raulcedroj.football_predictions.user.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
}