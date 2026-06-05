package com.zeta.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "change-me";
    private Expiration expiration = new Expiration();

    @Getter
    @Setter
    public static class Expiration {
        /** accessToken 有效期（秒） */
        private long access = 3600;
        /** refreshToken 有效期（秒） */
        private long refresh = 86400;
    }
}
