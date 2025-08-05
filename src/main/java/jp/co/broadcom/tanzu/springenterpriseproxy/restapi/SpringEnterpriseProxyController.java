package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import jp.co.broadcom.tanzu.springenterpriseproxy.metrics.UserAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/spring-enterprise-proxy/") // All proxy requests will come through /maven/
class SpringEnterpriseProxyController {

	private static final Logger logger = LoggerFactory.getLogger(SpringEnterpriseProxyController.class);

	private final ArtifactService artifactService;

	private final ApplicationEventPublisher publisher;

	SpringEnterpriseProxyController(ArtifactService artifactService, ApplicationEventPublisher publisher) {
		this.artifactService = artifactService;
		this.publisher = publisher;
	}

	/**
	 * Handles requests for Maven artifacts. The `{*artifactPath}` captures the entire
	 * remaining path as a single variable.
	 * <p>
	 * Example: Request for
	 * /maven/org/springframework/spring-core/6.1.6/spring-core-6.1.6.jar will have
	 * artifactPath = "/org/springframework/spring-core/6.1.6/spring-core-6.1.6.jar"
	 */
	@GetMapping("{*artifactPath}")
	ResponseEntity<Resource> proxyMavenArtifact(@PathVariable String artifactPath,
			@AuthenticationPrincipal Jwt jwt) {
		if (jwt != null) {
			Instant expiration = jwt.getExpiresAt();
			if (jwt != null && expiration != null && expiration.isBefore(Instant.now())) {
				throw new JwtValidationException("Token has expired", List.of(new OAuth2Error("expired_token")));
			}
			publisher.publishEvent(new UserAccess(jwt.getSubject(), artifactPath));
		}
		try {
			Resource artifact = artifactService.retrieveArtifact(artifactPath);
			MediaType contentType = MediaTypeUtil.getMediaTypeForFileName(artifactPath);

			return ResponseEntity.ok().contentType(contentType).body(artifact);
		}
		catch (RestClientResponseException e) {
			// Handle HTTP client errors from the remote repository (e.g., 404 Not Found)
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				logger.debug("Artifact not found in remote repository: {}", artifactPath);
				return ResponseEntity.notFound().build();
			}
			logger.error("Client error accessing remote repository for {}: {}", artifactPath, e.getStatusCode(), e);
			return ResponseEntity.status(e.getStatusCode()).build();
		}
		catch (IOException e) {
			// Handle I/O errors (e.g., issues during network transfer)
			logger.error("Internal server error while processing artifact {}: {}", artifactPath, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
		catch (Exception e) {
			// Catch any other unexpected exceptions
			logger.error("An unexpected error occurred for artifact {}: {}", artifactPath, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}