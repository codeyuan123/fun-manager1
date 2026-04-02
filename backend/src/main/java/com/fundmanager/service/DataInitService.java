package com.fundmanager.service;

import com.fundmanager.domain.entity.FundInfo;
import com.fundmanager.domain.entity.SysUser;
import com.fundmanager.repository.FundInfoRepository;
import com.fundmanager.repository.SysUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitService implements CommandLineRunner {

    private final SysUserRepository userRepository;
    private final FundInfoRepository fundInfoRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitService(SysUserRepository userRepository,
                           FundInfoRepository fundInfoRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.fundInfoRepository = fundInfoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            SysUser user = new SysUser();
            user.setUsername("admin");
            user.setNickname("Administrator");
            user.setPasswordHash(passwordEncoder.encode("admin123"));
            user.setStatus((byte) 1);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        List<String> sampleCodes = List.of("161725", "110022", "005827");
        for (String code : sampleCodes) {
            if (fundInfoRepository.findByFundCode(code).isEmpty()) {
                FundInfo info = new FundInfo();
                info.setFundCode(code);
                info.setFundName(code);
                info.setFundType("UNKNOWN");
                info.setStatus((byte) 1);
                info.setCreatedAt(LocalDateTime.now());
                info.setUpdatedAt(LocalDateTime.now());
                fundInfoRepository.save(info);
            }
        }
    }
}

