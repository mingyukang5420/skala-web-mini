package com.kokbaejeong.service;

import com.kokbaejeong.dto.AdminLoginRequest;
import com.kokbaejeong.dto.TokenResponse;
import com.kokbaejeong.entity.Admin;
import com.kokbaejeong.exception.BusinessException;
import com.kokbaejeong.exception.ErrorCode;
import com.kokbaejeong.repository.AdminRepository;
import com.kokbaejeong.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(AdminRepository adminRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public TokenResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FAILED));

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }

        return new TokenResponse(jwtTokenProvider.generateToken(admin.getUsername()));
    }
}
