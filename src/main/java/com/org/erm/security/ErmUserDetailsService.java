package com.org.erm.security;

import com.org.erm.model.ErmRole;
import com.org.erm.model.ErmUser;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ErmUserDetailsService implements UserDetailsService {

    private final ErmUserRepository ermUserRepository;

    public ErmUserDetailsService(ErmUserRepository ermUserRepository) {
        this.ermUserRepository = ermUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ErmUser ermUser = ermUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Set<GrantedAuthority> authorities = ermUser.getRoles()
                .stream()
                .map(ErmRole::getName)
                .map(RoleAuthorityMapper::toAuthority)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        return User.builder()
                .username(ermUser.getUsername())
                .password(ermUser.getPasswordHash())
                .disabled(!ermUser.isActive())
                .authorities(authorities)
                .build();
    }
}
