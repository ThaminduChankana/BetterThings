package org.project.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration // Maintain configuration about web client class
public class WebClientConfig {

    @Bean
    public WebClient webClient(){ // Spring web client is not part of the spring MVC project but it is a part of spring web flux project
        return WebClient.builder().build();

    }
}
