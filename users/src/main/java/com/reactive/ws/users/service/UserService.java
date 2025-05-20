package com.reactive.ws.users.service;

import com.reactive.ws.users.model.CreateUserRequest;
import com.reactive.ws.users.model.UserRest;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService extends ReactiveUserDetailsService {

    Mono<UserRest> createUser(Mono<CreateUserRequest> createUserRequestMono);

    Mono<UserRest> getUser(String userId, String include, String jwt);

    Flux<UserRest> findAll(int page, int limit);

    Flux<UserRest> streamUser();
}
