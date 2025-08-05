package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.stream.Collectors;

@RestController
class TokenController {

	JwtEncoder encoder;

	Long expiry;

	public TokenController(JwtEncoder encoder, @Value("${spring.enterprise.proxy.expiry:15552000}") Long expiry) {
		this.encoder = encoder;
		this.expiry = expiry;
	}

	@GetMapping("/token")
	public String token(Authentication authentication) {
		Instant now = Instant.now();

		String scope = authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.joining(" "));
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("self")
			.issuedAt(now)
			.expiresAt(now.plusSeconds(expiry))
			.subject(authentication.getName())
			.claim("scope", scope)
			.build();

		return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}

}