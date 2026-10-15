package com.artpizza;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class ArtPizzaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtPizzaApplication.class, args);
    }
}
