package com.subscriptiontracker.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    private String secret;
    private long expirationMs = 86400000;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalStateException(
                    "Invalid JWT configuration: 'jwt.secret' (JWT_SECRET) must be set and contain at least 32 characters (256 bits) for HMAC-SHA algorithms. Provided length: "
                            + (secret == null ? 0 : secret.trim().length()));
        }
    }
}
