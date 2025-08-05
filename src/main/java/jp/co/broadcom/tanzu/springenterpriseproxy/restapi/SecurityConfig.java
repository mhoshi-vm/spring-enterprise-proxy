package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@EnableWebSecurity
class SecurityConfig {

	RSAPublicKey key;
	RSAPrivateKey priv;

	public SecurityConfig(@Value("${spring.enterprise.proxy.jwt-public-key}") RSAPublicKey key,
						  @Value("${spring.enterprise.proxy.jwt-private-key}") RSAPrivateKey priv) {
		this.key = key;
		this.priv = priv;
	}

	@Bean
	@Order(1)
	public SecurityFilterChain tokenFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/token", "/oauth2/**", "/login/**")
			.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults())
			.oauth2Login(Customizer.withDefaults())
			.oauth2Client(Customizer.withDefaults());
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/actuator/**").authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
		return http.build();
	}

	@Bean
	@ConditionalOnProperty(value = "spring.enterprise.proxy.oauth-enabled", havingValue = "true")
	SecurityFilterChain oidcSecurityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
			.oauth2ResourceServer((jwt) -> jwt.jwt(Customizer.withDefaults()));
		return http.build();
	}

	@Bean
	@ConditionalOnProperty(value = "spring.enterprise.proxy.oauth-enabled", havingValue = "false",
			matchIfMissing = true)
	SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
			.oauth2ResourceServer((jwt) -> jwt.jwt(Customizer.withDefaults()));
		return http.build();
	}

	@Bean
	@ConditionalOnProperty(value = "spring.enterprise.proxy.oauth-enabled", havingValue = "false",
			matchIfMissing = true)
	UserDetailsService users() {
		return new InMemoryUserDetailsManager(
				User.withUsername("user").password("{noop}password").authorities("app").build());
	}

	@Bean
	JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withPublicKey(this.key).build();
	}

	@Bean
	JwtEncoder jwtEncoder() {
		JWK jwk = new RSAKey.Builder(this.key).privateKey(this.priv).build();
		JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
		return new NimbusJwtEncoder(jwks);
	}

}