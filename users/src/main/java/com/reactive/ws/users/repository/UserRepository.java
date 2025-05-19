package com.reactive.ws.users.repository;

import com.reactive.ws.users.model.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface UserRepository extends ReactiveCrudRepository<UserEntity, String> {

    Flux<UserEntity> findAllBy(Pageable pageable);

    Mono<UserEntity> findByEmail(String email);
}
