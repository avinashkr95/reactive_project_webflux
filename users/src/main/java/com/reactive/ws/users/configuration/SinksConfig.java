package com.reactive.ws.users.configuration;

import com.reactive.ws.users.model.UserRest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Sinks;

@Configuration
public class SinksConfig {

    @Bean
    public Sinks.Many<UserRest> userSinks() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }
}
