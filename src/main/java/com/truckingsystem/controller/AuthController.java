package com.truckingsystem.controller;

import com.truckingsystem.pojo.SignUpRequest;
import com.truckingsystem.pojo.SignUpResponse;
import com.truckingsystem.service.AuthService;
import jakarta.validation.Valid;
import com.truckingsystem.entity.MemberStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpResponse response = authService.signUp(request);
        HttpStatus status = response.membershipStatus() == MemberStatus.ACTIVE
                ? HttpStatus.CREATED
                : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(response);
    }
}
