package com.kokbaejeong.controller;

import com.kokbaejeong.dto.AdminLoginRequest;
import com.kokbaejeong.dto.TokenResponse;
import com.kokbaejeong.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return authService.login(request);
    }
}
