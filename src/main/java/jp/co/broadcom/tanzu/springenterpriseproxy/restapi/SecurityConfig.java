package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jp.co.broadcom.tanzu.springenterpriseproxy.SpringEnterpriseProxyProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
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

	RSAPrivateKey privKey;

	SecurityConfig(SpringEnterpriseProxyProperties springEnterpriseProxyProperties) {
		this.key = springEnterpriseProxyProperties.jwtPublicKey();
		this.privKey = springEnterpriseProxyProperties.jwtPrivateKey();
	}

	@Bean
	@Order(1)
	@ConditionalOnProperty(value = "spring.enterprise.proxy.oauth-enabled", havingValue = "true")
	SecurityFilterChain tokenFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/token", "/oauth2/**", "/login/**")
			.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults())
			.oauth2Login(Customizer.withDefaults())
			.oauth2Client(Customizer.withDefaults());
		return http.build();
	}

	@Bean
	@Order(1)
	@ConditionalOnProperty(value = "spring.enterprise.proxy.oauth-enabled", havingValue = "false",
			matchIfMissing = true)
	SecurityFilterChain tokenLocalFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/token", "/oauth2/**", "/login/**")
			.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults());
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/actuator/**").authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
		return http.build();
	}

	@Bean
	@Order(3)
	@ConditionalOnProperty(value = "spring.h2.console.enabled", havingValue = "true")
	public SecurityFilterChain h2FilterChain(HttpSecurity http) throws Exception {

		http.securityMatcher(PathRequest.toH2Console())
			.csrf(csrf -> csrf.ignoringRequestMatchers(PathRequest.toH2Console()))
			.authorizeHttpRequests(
					auth -> auth.requestMatchers(PathRequest.toH2Console()).permitAll().anyRequest().authenticated())
			.headers(headers -> headers.frameOptions().sameOrigin());

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
		JWK jwk = new RSAKey.Builder(this.key).privateKey(this.privKey).build();
		JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
		return new NimbusJwtEncoder(jwks);
	}

}