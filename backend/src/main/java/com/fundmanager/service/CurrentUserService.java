package com.fundmanager.service;

import com.fundmanager.common.BusinessException;
import com.fundmanager.domain.entity.SysUser;
import com.fundmanager.repository.SysUserRepository;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final SysUserRepository userRepository;

    public CurrentUserService(SysUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public SysUser getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    public Long getUserId(String username) {
        return getByUsername(username).getId();
    }
}
