package com.fundmanager.security;

import com.fundmanager.domain.entity.SysUser;
import com.fundmanager.repository.SysUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserPrincipalService implements UserDetailsService {

    private final SysUserRepository userRepository;

    public UserPrincipalService(SysUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new User(user.getUsername(), user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
