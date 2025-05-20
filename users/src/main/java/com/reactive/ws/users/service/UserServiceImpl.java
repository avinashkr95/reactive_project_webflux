package com.reactive.ws.users.service;

import com.reactive.ws.users.model.AlbumRest;
import com.reactive.ws.users.model.UserEntity;
import com.reactive.ws.users.repository.UserRepository;
import com.reactive.ws.users.model.CreateUserRequest;
import com.reactive.ws.users.model.UserRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final Sinks.Many<UserRest> usersSink;

    private final WebClient webClient;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, Sinks.Many<UserRest> usersSink, WebClient webClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.usersSink = usersSink;
        this.webClient = webClient;
    }

    @Override
    public Mono<UserRest> createUser(Mono<CreateUserRequest> createUserRequestMono) {

        return createUserRequestMono
                .flatMap(createUserRequest -> convertToEntity(createUserRequest))// Instead of mapNotNull we used flat map because save method is also returning Mono so this will become a nested Mono
                .flatMap(userEntity -> userRepository.save(userEntity))
                .mapNotNull(userEntity -> convertToRest(userEntity))
                .doOnSuccess(savedUser -> usersSink.tryEmitNext(savedUser)); // this will publish saved user to all subscriber of a sink


        //Below lines are commented becuase now this is being handled by GlobalExceptionHandler class
//                .onErrorMap(throwable -> {
//                    if (throwable instanceof DuplicateKeyException) {
//                        return new ResponseStatusException(HttpStatus.CONFLICT, throwable.getMessage());
//                    } else if (throwable instanceof DataIntegrityViolationException) {
//                        return new ResponseStatusException(HttpStatus.BAD_REQUEST, throwable.getMessage());
//                    } else {
//                        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, throwable.getMessage());
//                    }
//                });
    }

    @Override
    public Mono<UserRest> getUser(String userId, String include, String jwt) {
        return userRepository.findById(userId)
                .mapNotNull(userEntity -> convertToRest(userEntity))
                .flatMap(user -> {
                    if (include != null && include.equals("albums")) {
                        //fetch user's photo albums and add them to the user object
                        return includeUserAlbums(user, jwt);
                    }
                    return Mono.just(user);
                });
    }

    @Override
    public Flux<UserRest> findAll(int page, int limit) {
        if (page>0) page = page-1;
        Pageable pageable = PageRequest.of(page, limit);
        return userRepository.findAllBy(pageable)
                .map(userEntity -> convertToRest(userEntity));
    }

    @Override
    public Flux<UserRest> streamUser() {
        return usersSink.asFlux()
                .publish()
                .autoConnect(1); // start publishing data immediately as soon as there is at least one subscriber

    }

    private Mono<UserEntity> convertToEntity(CreateUserRequest createUserRequest) {
        return Mono.fromCallable(() -> { // this function is used to wrap potentially blocking operations like password encoding
            UserEntity userEntity = new UserEntity();
            BeanUtils.copyProperties(createUserRequest, userEntity);
            userEntity.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));
            return userEntity;
        }).subscribeOn(Schedulers.boundedElastic()); // By combining fromCallable with subscribeOn,
        // we ensure that the blocking operation(here password encoding) is executed on a separate thread
    }

    private UserRest convertToRest(UserEntity userEntity) {
        UserRest userRest = new UserRest();
        BeanUtils.copyProperties(userEntity, userRest);
        return userRest;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByEmail(username)
                .map(userEntity -> User.withUsername(userEntity.getEmail())
                        .password(userEntity.getPassword())
                        .authorities((new ArrayList<>()))
                        .build());
    }

    private Mono<UserRest> includeUserAlbums(UserRest user, String jwt) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .port(8084)
                        .path("/albums")
                        .queryParam("userId", user.getId())
                        .build())
                .header("Authorization", jwt)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                     return Mono.error(new RuntimeException("Albums not found for userId: " + user.getId()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    return Mono.error(new RuntimeException("Server error while fetching albums"));
                })
                .bodyToFlux(AlbumRest.class)
                .collectList()
                .map(albums -> {
                    user.setAlbums(albums);
                    return user;
                })
                .onErrorResume(e -> {
                    logger.error("Error while fetching albums for user: " + user.getId(), e);
                    return Mono.just(user);
                });
    }
}
