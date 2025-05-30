package com.reactive.ws.users.service;

import reactor.core.publisher.Mono;

public interface JwtService {

    String generateJwt(String userId);

    Mono<Boolean> validateJwt(String token);

    String extractTokenSubject(String token);
}
