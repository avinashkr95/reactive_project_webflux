package com.reactive.ws.users.controller;

import com.reactive.ws.users.model.CreateUserRequest;
import com.reactive.ws.users.model.UserRest;
import com.reactive.ws.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<UserRest>> createUser(@RequestBody @Valid Mono<CreateUserRequest> createUserRequest) {

        return userService.createUser(createUserRequest)
                .map(user -> ResponseEntity
                .status(HttpStatus.CREATED)
                .location(URI.create("/users/" + user.getId()))
                .body(user));
    }

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<UserRest>> getUser(@PathVariable String userId) {
        return userService.getUser(userId)
                .map(userRest -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(userRest))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build())); //If user is not found then use switchIfEmpty
    }

    @GetMapping
    public Flux<UserRest> getUsers(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                   @RequestParam(value = "limit", defaultValue = "50") int limit) {

        return userService.findAll(offset, limit);
//                .map(userRest -> ResponseEntity
//                        .status(HttpStatus.OK)
//                        .body(userRest));
    }
}
