package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(SecurityConfig.class)
@TestPropertySource(properties = { "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://test.issuer.com/realm",
		"spring.enterprise.proxy.oauth-enabled=true" })
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean // This will replace the actual JwtDecoder bean in our test context
	private JwtDecoder jwtDecoder;

	@MockitoBean // Creates a Mockito mock and adds it to the Spring application context
	private ArtifactService artifactService;

	// We don't need to mock JwtAuthenticationConverter as it's a simple conversion
	// and its logic is tied to the Jwt object itself in our config.
	// If you had more complex custom logic there you might mock parts of it.

	private final String MOCK_VALID_JWT_STRING = "mock.valid.jwt";

	private final String MOCK_EXPIRED_JWT_STRING = "mock.expired.jwt";

	private final String MOCK_INVALID_JWT_STRING = "mock.invalid.aud.jwt";

	// --- Test Cases ---
	@Test
	void shouldDenyAccessToSecuredEndpointWithoutToken() throws Exception {
		mockMvc.perform(get("/spring-enterprise-proxy/aaa")).andExpect(status().isUnauthorized()); // 401
																					// Unauthorized
																					// for
																					// missing
																					// token
		mockMvc.perform(get("/spring-enterprise-proxy/org/aod/artifact/1.0.0/artifact.jar")).andExpect(status().isUnauthorized()); // 401
																													// Unauthorized
																													// for
																													// missing
																													// token
	}

	@Test
	void shouldAllowAccessToSecuredEndpointWithValidToken() throws Exception {
		Instant now = Instant.now();
		Instant expiry = now.plusSeconds(3600);
		Jwt mockJwt = Jwt.withTokenValue(MOCK_VALID_JWT_STRING)
			.issuedAt(now)
			.expiresAt(expiry)
			.header("typ", "JWT")
			.header("alg", "RS256")
			.claim("iss", "https://test.issuer.com/realm")
			.claim("sub", "test-user")
			.build();
		Mockito.when(jwtDecoder.decode(MOCK_VALID_JWT_STRING)).thenReturn(mockJwt);
		mockMvc
			.perform(get("/spring-enterprise-proxy/org/aod/artifact/1.0.0/artifact.jar").header("Authorization",
					"Bearer " + MOCK_VALID_JWT_STRING))
			.andExpect(status().isOk());
		// .andExpect(content().string("Serving Maven artifact:
		// org:aod:artifact/1.0.0/artifact.jar for user: user123"));
	}

	@Test
	void shouldDenyAccessWithExpiredToken() throws Exception {
		Instant now = Instant.now();
		Instant iat = now.minusSeconds(7200);
		Instant expiry = now.minusSeconds(3600);
		Jwt mockJwt = Jwt.withTokenValue(MOCK_EXPIRED_JWT_STRING)
			.issuedAt(iat)
			.expiresAt(expiry)
			.header("typ", "JWT")
			.header("alg", "RS256")
			.claim("iss", "https://test.issuer.com/realm")
			.claim("sub", "test-user")
			.build();
		Mockito.when(jwtDecoder.decode(MOCK_EXPIRED_JWT_STRING)).thenReturn(mockJwt);

		ServletException exception = assertThrows(ServletException.class, () -> {
			mockMvc.perform(get("/spring-enterprise-proxy/org/aod/artifact/1.0.0/artifact.jar").header("Authorization",
					"Bearer " + MOCK_EXPIRED_JWT_STRING));
		});

		assertEquals("Token has expired", exception.getCause().getMessage());
	}

}