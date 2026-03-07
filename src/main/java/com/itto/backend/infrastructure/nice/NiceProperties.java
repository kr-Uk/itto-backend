package com.itto.backend.infrastructure.nice;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nice")
@Getter
@Setter
public class NiceProperties {
    private String clientId;
    private String clientSecret;
    private String productId;
    private String accessTokenUrl;
    private String cryptoTokenUrl;
    private String returnUrl;
    private String errorUrl;
}
