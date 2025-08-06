package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import jp.co.broadcom.tanzu.springenterpriseproxy.SpringEnterpriseProxyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
class ArtifactService {

	private static final Logger logger = LoggerFactory.getLogger(ArtifactService.class);

	private final SpringEnterpriseProxyProperties config;

	private final RestClient restClient;

	private final ArtifactRepository artifactRepository;

	ArtifactService(SpringEnterpriseProxyProperties config, RestClient.Builder restClientBuilder,
			ArtifactRepository artifactRepository) {
		this.config = config;
		this.artifactRepository = artifactRepository;

		// Configure RestClient with Basic Authentication if credentials are provided
		String username = config.remoteRepoUsername();
		String password = config.remoteRepoPassword();

		if (username != null && password != null) {
			// https://stackoverflow.com/questions/17970633/header-values-overwritten-on-redirect-in-httpclient
			HttpClient httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NEVER)
				.authenticator(new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password.toCharArray());
					}
				})
				.build();
			this.restClient = restClientBuilder.requestFactory(new JdkClientHttpRequestFactory(httpClient)).build();
		}
		else {
			this.restClient = restClientBuilder.build();
		}

	}

	/**
	 * Retrieves an artifact, either from the database cache or the remote repository. If
	 * fetched from remote, it's saved to the database.
	 * @param artifactPath The relative path of the artifact (e.g.,
	 * /org/apache/maven/maven-model/3.8.6/maven-model-3.8.6.pom)
	 * @return A Spring Resource representing the artifact content.
	 * @throws IOException If there's an issue with I/O (e.g., network problems during
	 * remote fetch).
	 * @throws RestClientResponseException If the remote repository returns an HTTP error
	 * (e.g., 404 Not Found).
	 */
	Resource retrieveArtifact(String artifactPath) throws IOException, RestClientResponseException {
		// 1. Try to serve from database cache
		Optional<Artifact> cachedArtifact = artifactRepository.findByPath(artifactPath);
		if (cachedArtifact.isPresent()) {
			logger.debug("Serving artifact from DB cache: {}", artifactPath);
			// Optionally, log the summary using the new record

			return new ByteArrayResource(cachedArtifact.get().content());
		}

		// 2. If not in cache, fetch from remote repository
		String remoteUrl = config.remoteRepoUrl() + artifactPath;
		logger.debug("Fetching artifact from remote: {}", remoteUrl);

		try {
			// Using RestClient's fluent API.
			// .retrieve() handles non-2xx responses by throwing
			// RestClientResponseException.
			HttpHeaders headers = new HttpHeaders();
			MediaType contentType = MediaTypeUtil.getMediaTypeForFileName(artifactPath);

			headers.setContentType(contentType);
			ResponseEntity<byte[]> response = restClient.get()
				.uri(remoteUrl)
				.headers(httpHeaders -> httpHeaders.addAll(headers))
				.retrieve()
				.toEntity(byte[].class);

			if (response.getStatusCode() == HttpStatus.FOUND || response.getStatusCode() == HttpStatus.MOVED_PERMANENTLY
					|| response.getStatusCode() == HttpStatus.SEE_OTHER) {

				String redirectUrl = Objects.requireNonNull(response.getHeaders().getLocation()).toString();
				if (redirectUrl != null) {
					logger.debug("Received 302/303");

					URI encodedRedirectUrl = UriComponentsBuilder.fromUriString(redirectUrl).build(true).toUri();

					// Step 2: Make a new request to the redirected URL *without* the
					// Authorization header
					ResponseEntity<byte[]> redirectedResponse = restClient.get()
						.uri(encodedRedirectUrl)
						// DO NOT add Authorization header here
						.retrieve()
						.toEntity(byte[].class);

					logger.debug("Redirected response status: {}", redirectedResponse.getStatusCode());
					if (redirectedResponse.getStatusCode().is2xxSuccessful() && redirectedResponse.getBody() != null) {
						byte[] content = redirectedResponse.getBody();

						// 3. Cache the fetched artifact in the database
						Artifact newArtifact = new Artifact(artifactPath, content, contentType.toString(),
								LocalDateTime.now());
						artifactRepository.save(newArtifact); // Save to a database
						logger.info("Artifact cached in DB successfully: {}", artifactPath);

						return new ByteArrayResource(content);
					}
					else {
						// This path is less likely to be hit if retrieve() throws for
						// non-2xx
						// statuses,
						// but kept for explicit clarity in case of unexpected successful
						// but
						// empty responses.
						logger.debug("Failed to fetch artifact from remote. Status: {}, Path: {}",
								redirectedResponse.getStatusCode(), artifactPath);
						throw new IOException("Unexpected status from remote: " + redirectedResponse.getStatusCode());
					}
				}
				else {
					logger.warn("Received 302/303 but no Location header found.");
					throw new IOException("Unexpected status from remote: " + response.getStatusCode());
				}

			}
			else if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				logger.debug("Initial response status: {}", response.getStatusCode());
				logger.debug("Initial response headers: {}", response.getHeaders());
				byte[] content = response.getBody();
				Artifact newArtifact = new Artifact(artifactPath, content, contentType.toString(), LocalDateTime.now());
				artifactRepository.save(newArtifact); // Save to a database
				logger.info("Artifact cached in DB successfully: {}", artifactPath);

				return new ByteArrayResource(content);

			}
		}
		catch (RestClientResponseException e) {
			// Throw RestClientResponseException (e.g., 404, 403 from remote) to be
			// handled by the controller
			logger.debug("Remote repository error for {}: {} - {}", artifactPath, e.getStatusCode(), e.getStatusText());
			throw e;

		}
		catch (Exception e) {
			// Catch any other exceptions during remote fetch or local caching
			logger.debug("Error fetching or caching artifact {}: {}", artifactPath, e.getMessage(), e);
			throw new IOException("Failed to fetch or cache artifact: " + artifactPath, e);
		}
		return null;
	}

}
