package com.org.erm.security;

import com.org.erm.repository.ErmTokenBlacklistRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final ErmUserDetailsService ermUserDetailsService;
    private final ErmTokenBlacklistRepository ermTokenBlacklistRepository;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   ErmUserDetailsService ermUserDetailsService,
                                   ErmTokenBlacklistRepository ermTokenBlacklistRepository) {
        this.jwtService = jwtService;
        this.ermUserDetailsService = ermUserDetailsService;
        this.ermTokenBlacklistRepository = ermTokenBlacklistRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.debug("No Authorization header or not Bearer for request {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            if (jwtService.isTokenExpired(token)) {
                log.debug("JWT token is expired for request {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            String tokenHash = jwtService.hashToken(token);
            if (ermTokenBlacklistRepository.existsByTokenHashAndExpiresAtAfter(tokenHash, LocalDateTime.now())) {
                log.debug("JWT token is blacklisted for request {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtService.extractUsername(token);
            if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = ermUserDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.debug("Authenticated request {} as user {}", request.getRequestURI(), username);
                } else {
                    log.debug("JWT token failed validation for user {} on request {}", username, request.getRequestURI());
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            log.warn("JWT processing error for request {}: {}", request.getRequestURI(), ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
