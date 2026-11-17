package dev.logplatform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class Security {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity h, @Value("${OIDC_JWK_URL:http://localhost:8180/realms/logs/protocol/openid-connect/certs}") String url) throws Exception {
        return h.csrf(c -> c.disable()).authorizeHttpRequests(a -> a.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll().requestMatchers("/actuator/health", "/api/v1/logs/ingest", "/v1/logs").permitAll().anyRequest().authenticated()).oauth2ResourceServer(o -> o.jwt(j -> j.decoder(decoder(url)))).build();
    }

    JwtDecoder decoder(String url) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(url).build();
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), token -> token.getAudience().contains("log-platform") ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success() : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token")), new JwtIssuerValidator(System.getenv().getOrDefault("OIDC_ISSUER", "http://localhost:8180/realms/logs"))));
        return decoder;
    }
}
