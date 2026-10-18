package com.lms.filter;

import com.lms.config.AdminUserDetailsService;
import com.lms.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates a request from its "Authorization: Bearer <jwt>" header.
 *
 * - The user and role are re-loaded from the database on every request, so a deleted user or a
 *   changed role takes effect immediately instead of when the token expires.
 * - A missing, expired, tampered or otherwise invalid token never throws: the request simply stays
 *   unauthenticated, and Spring Security answers 401 (protected URLs) or lets it through (public URLs).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final AdminUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, AdminUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String jwt = authHeader.substring(BEARER.length()).trim();
            try {
                String username = jwtService.extractUsername(jwt);
                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (ExpiredJwtException e) {
                log.debug("Rejected expired JWT for {}", request.getRequestURI());
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("Rejected invalid JWT for {}: {}", request.getRequestURI(), e.getMessage());
            } catch (UsernameNotFoundException e) {
                log.debug("Rejected JWT for a user that no longer exists ({})", request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response);
    }
}
