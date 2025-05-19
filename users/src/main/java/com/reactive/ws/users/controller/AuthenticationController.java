package com.reactive.ws.users.controller;

import com.reactive.ws.users.model.AuthenticationRequest;
import com.reactive.ws.users.service.AuthenticationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }


    @PostMapping("/login")
    public Mono<ResponseEntity<Object>> login(@RequestBody Mono<AuthenticationRequest> authenticationRequestMono) {
        return authenticationRequestMono
                .flatMap(authenticationRequest ->
                        authenticationService.authenticate(authenticationRequest.getEmail(),
                                authenticationRequest.getPassword()))
                .map(authenticationMap -> ResponseEntity.ok()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "
                                + authenticationMap.get("token"))
                        .header("UserId", authenticationMap.get("userId"))
                        .build());
//                .onErrorReturn(BadCredentialsException.class,
//                        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                                .body("Invalid credentials"))
//                .onErrorReturn(Exception.class,
//                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}
