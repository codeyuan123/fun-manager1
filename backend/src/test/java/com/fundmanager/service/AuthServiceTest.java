package com.fundmanager.service;

import com.fundmanager.common.BusinessException;
import com.fundmanager.domain.dto.ChangePasswordRequest;
import com.fundmanager.domain.entity.SysUser;
import com.fundmanager.repository.SysUserRepository;
import com.fundmanager.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final SysUserRepository userRepository = Mockito.mock(SysUserRepository.class);
    private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    private final JwtService jwtService = Mockito.mock(JwtService.class);
    private final AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);

    @Test
    void shouldRejectWhenCurrentPasswordIncorrect() {
        SysUser user = user();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(BusinessException.class, () ->
                authService.changePassword("admin", new ChangePasswordRequest("wrong", "Newpass123", "Newpass123")));
    }

    @Test
    void shouldRejectWhenNewPasswordInvalid() {
        SysUser user = user();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", "encoded")).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                authService.changePassword("admin", new ChangePasswordRequest("admin123", "short", "short")));
    }

    @Test
    void shouldRejectWhenPasswordUnchanged() {
        SysUser user = user();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin1234", "encoded")).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                authService.changePassword("admin", new ChangePasswordRequest("admin1234", "admin1234", "admin1234")));
    }

    @Test
    void shouldUpdatePasswordHash() {
        SysUser user = user();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("Newpass123")).thenReturn("new-encoded");

        assertDoesNotThrow(() ->
                authService.changePassword("admin", new ChangePasswordRequest("admin123", "Newpass123", "Newpass123")));

        verify(userRepository).save(any(SysUser.class));
    }

    private SysUser user() {
        SysUser user = new SysUser();
        user.setUsername("admin");
        user.setPasswordHash("encoded");
        user.setNickname("Administrator");
        user.setStatus((byte) 1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
