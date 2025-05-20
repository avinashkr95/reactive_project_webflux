package com.reactive.ws.users.service;

public interface JwtService {

    String generateJwt(String userId);
}
