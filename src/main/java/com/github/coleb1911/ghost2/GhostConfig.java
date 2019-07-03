package com.github.coleb1911.ghost2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource(value = {"classpath:ghost.properties"})
class GhostConfig {
    @Value("${ghost.token}")
    private transient String token;

    String getToken() {
        return token;
    }
}
