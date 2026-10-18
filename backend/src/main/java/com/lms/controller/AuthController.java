package com.lms.controller;

import com.lms.config.AdminUserDetails;
import com.lms.dto.AuthDTOs;
import com.lms.service.JwtService;
import com.lms.repository.AdminUserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;

    @Value("${app.security.password-hash-endpoint-enabled:true}")
    private boolean passwordHashEndpointEnabled;

    /** Fails with 401 (via GlobalExceptionHandler) for unknown user or wrong password. */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDTOs.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        AdminUserDetails user = (AdminUserDetails) authentication.getPrincipal();
        // the role is informational in the token; authorization always uses the role stored in the database
        String jwt = jwtService.generateToken(Map.of("role", user.getRole()), user.getUsername());
        return ResponseEntity.ok(new AuthDTOs.LoginResponse(jwt, user.getRole(), user.getUsername()));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody AuthDTOs.ChangePasswordRequest req,
                                             Authentication auth) {
        return adminUserRepository.findByUsername(auth.getName())
                .filter(u -> passwordEncoder.matches(req.getOldPassword(), u.getPassword()))
                .map(u -> {
                    u.setPassword(passwordEncoder.encode(req.getNewPassword()));
                    adminUserRepository.save(u);
                    return ResponseEntity.ok("Password changed successfully");
                })
                .orElse(ResponseEntity.status(400).build());
    }

    @PostMapping("/getPassword")
    public ResponseEntity<?> getPassword(@RequestBody AuthDTOs.getPasswordRequest request) {
        if (!passwordHashEndpointEnabled) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(passwordEncoder.encode(request.getPassword()));
    }
}
