package com.artpizza.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "admin")
@Validated
@Getter
@Setter
public class AdminProperties {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
