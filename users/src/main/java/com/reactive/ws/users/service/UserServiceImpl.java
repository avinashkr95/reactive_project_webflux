package com.reactive.ws.users.service;

import com.reactive.ws.users.model.UserEntity;
import com.reactive.ws.users.repository.UserRepository;
import com.reactive.ws.users.model.CreateUserRequest;
import com.reactive.ws.users.model.UserRest;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<UserRest> createUser(Mono<CreateUserRequest> createUserRequestMono) {

        return createUserRequestMono
                .flatMap(createUserRequest -> convertToEntity(createUserRequest))// Instead of mapNotNull we used flat map because save method is also returning Mono so this will become a nested Mono
                .flatMap(userEntity -> userRepository.save(userEntity))
                .mapNotNull(userEntity -> convertToRest(userEntity));
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
    public Mono<UserRest> getUser(String userId) {
        return userRepository.findById(userId)
                .mapNotNull(userEntity -> convertToRest(userEntity));
    }

    @Override
    public Flux<UserRest> findAll(int page, int limit) {
        if (page>0) page = page-1;
        Pageable pageable = PageRequest.of(page, limit);
        return userRepository.findAllBy(pageable)
                .map(userEntity -> convertToRest(userEntity));
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
}
