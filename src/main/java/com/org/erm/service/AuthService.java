package com.org.erm.service;

import com.org.erm.dto.request.LoginRequest;
import com.org.erm.dto.response.LoginResponse;
import com.org.erm.model.ErmRole;
import com.org.erm.model.ErmTokenBlacklist;
import com.org.erm.model.ErmUser;
import com.org.erm.repository.ErmTokenBlacklistRepository;
import com.org.erm.repository.ErmUserRepository;
import com.org.erm.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final ErmUserRepository ermUserRepository;
    private final ErmTokenBlacklistRepository ermTokenBlacklistRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       ErmUserRepository ermUserRepository,
                       ErmTokenBlacklistRepository ermTokenBlacklistRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.ermUserRepository = ermUserRepository;
        this.ermTokenBlacklistRepository = ermTokenBlacklistRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        ErmUser ermUser = ermUserRepository.findByUsernameIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        List<String> roles = ermUser.getRoles()
                .stream()
                .map(ErmRole::getName)
                .sorted(Comparator.naturalOrder())
                .toList();

        String token = jwtService.generateToken(userDetails, ermUser.getId(), roles);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs(), ermUser.getUsername(), roles);
    }

    public void logout(String authorizationHeader) {
        String token = resolveToken(authorizationHeader);
        try {
            LocalDateTime expiresAt = jwtService.extractExpiration(token).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            LocalDateTime now = LocalDateTime.now();

            if (expiresAt.isAfter(now)) {
                String tokenHash = jwtService.hashToken(token);
                if (!ermTokenBlacklistRepository.existsByTokenHash(tokenHash)) {
                    ErmTokenBlacklist blacklistEntry = new ErmTokenBlacklist();
                    blacklistEntry.setTokenHash(tokenHash);
                    blacklistEntry.setExpiresAt(expiresAt);
                    ermTokenBlacklistRepository.save(blacklistEntry);
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JWT token");
        }
    }

    private String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Authorization header must be Bearer token");
        }
        return authorizationHeader.substring(7);
    }
}
