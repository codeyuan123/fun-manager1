package com.fundmanager.service;

import com.fundmanager.common.BusinessException;
import com.fundmanager.domain.dto.ChangePasswordRequest;
import com.fundmanager.domain.dto.LoginRequest;
import com.fundmanager.domain.entity.SysUser;
import com.fundmanager.domain.vo.LoginVO;
import com.fundmanager.repository.SysUserRepository;
import com.fundmanager.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(SysUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginVO login(LoginRequest request) {
        SysUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException("Username or password is incorrect"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Username or password is incorrect");
        }
        String token = jwtService.generateToken(user.getId(), user.getUsername());
        return new LoginVO(token, user.getUsername(), user.getNickname());
    }

    public Map<String, Object> me(String username) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User does not exist"));
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname()
        );
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User does not exist"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("New passwords do not match");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new BusinessException("New password must differ from current password");
        }
        if (!PASSWORD_POLICY.matcher(request.newPassword()).matches()) {
            throw new BusinessException("New password must be at least 8 characters and include letters and numbers");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
