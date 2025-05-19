package com.reactive.ws.users.service;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface AuthenticationService {

    Mono<Map<String, String>> authenticate(String username, String password); // It will return JSON access token and may be username
}
