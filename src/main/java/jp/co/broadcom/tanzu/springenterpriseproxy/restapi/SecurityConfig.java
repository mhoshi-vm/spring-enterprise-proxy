package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(value = "spring.enterprise.proxy.oauth-enabled", havingValue = "true")
class SecurityConfig {

	@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
	private String issuerUri;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
			// Require authentication for all requests under /maven
			.requestMatchers("/spring-enterprise-proxy/**")
			.authenticated()
			// Optional: You can further restrict based on roles/authorities
			// .requestMatchers("/maven/**").hasAnyRole("MAVEN_ACCESS", "ADMIN")
			// Allow all other requests (or configure as needed for other endpoints)
			.anyRequest()
			.permitAll())
			// Enable OAuth2 Resource Server support for JWT tokens
			.oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(Customizer.withDefaults()));
		return http.build();
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

		// Build a list of validators
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		// Default validator checks issuer and expiration
		validators.add(JwtValidators.createDefaultWithIssuer(issuerUri));

		// Compose all validators
		OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> validator = new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
				validators);

		jwtDecoder.setJwtValidator(validator);
		return jwtDecoder;
	}

}