package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import jp.co.broadcom.tanzu.springenterpriseproxy.SpringEnterpriseProxyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Initializes Mockito mocks
class ArtifactServiceTest {

	private final String ARTIFACT_PATH = "org/example/library/1.0.0/library-1.0.0.jar";

	private final String REMOTE_URL = "https://remote.maven.org/maven2/" + ARTIFACT_PATH;

	private final byte[] ARTIFACT_CONTENT = "some-jar-content".getBytes();

	@Mock
	private SpringEnterpriseProxyProperties springEnterpriseProxyProperties; // Mock configuration

	@Mock
	private RestClient.Builder restClientBuilder; // Mock RestClient builder

	@Mock
	private RestClient restClient; // Mock the actual RestClient

	@Mock
	private ArtifactRepository artifactRepository; // Mock the JPA repository

	@InjectMocks // Injects the mocks into ArtifactProxyService
	private ArtifactService artifactService;

	@BeforeEach
	void setUp() {
		// Common setup for MavenProxyConfig
		lenient().when(springEnterpriseProxyProperties.remoteRepoUrl()).thenReturn("http://remote.maven.org/maven2/");
		lenient().when(springEnterpriseProxyProperties.remoteRepoUsername()).thenReturn(null); // No
																					// auth
																					// by
																					// default
																					// for
																					// these
																					// tests
		lenient().when(springEnterpriseProxyProperties.remoteRepoPassword()).thenReturn(null);

		// Common setup for RestClient builder
		// We need to mock the entire fluent API chain:
		// restClientBuilder.build().get().uri().retrieve().toEntity()
		when(restClientBuilder.build()).thenReturn(restClient);

		// Reinitialize the service to ensure mocks are applied correctly for each test,
		// This is important because the RestClient is built in the constructor
		artifactService = new ArtifactService(springEnterpriseProxyProperties, restClientBuilder, artifactRepository);
	}

	@Test
	void retrieveArtifact_foundInCache() throws IOException {

		// Given: Artifact exists in database
		Artifact cachedArtifact = new Artifact(ARTIFACT_PATH, ARTIFACT_CONTENT, "application/octet-stream",
				java.time.LocalDateTime.now());
		when(artifactRepository.findByPath(ARTIFACT_PATH)).thenReturn(Optional.of(cachedArtifact));

		// When
		Resource result = artifactService.retrieveArtifact(ARTIFACT_PATH);

		// Then
		assertThat(result.getContentAsByteArray()).isEqualTo(ARTIFACT_CONTENT);
		verify(artifactRepository).findByPath(ARTIFACT_PATH); // Verify cache check
		verify(restClient, never()).get(); // Verify remote call was NOT made
		verify(artifactRepository, never()).save(any(Artifact.class)); // Verify no new
																		// save
	}

	@Test
	void retrieveArtifact_notFoundInCache_fetchedFromRemoteAndCached() throws IOException {
		RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
		RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
		RestClient.RequestBodyUriSpec requestBodyUriSpecHeaders = mock(RestClient.RequestBodyUriSpec.class);
		RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
		when(restClient.get()).thenReturn(requestHeadersUriSpec);

		when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpecHeaders);
		when(requestBodyUriSpecHeaders.retrieve()).thenReturn(responseSpec);

		// Given: Artifact not in database
		when(artifactRepository.findByPath(ARTIFACT_PATH)).thenReturn(Optional.empty());

		// And: Remote call succeeds
		ResponseEntity<byte[]> successResponse = new ResponseEntity<>(ARTIFACT_CONTENT, HttpStatus.OK);
		when(restClient.get().uri(REMOTE_URL).headers(any()).retrieve().toEntity(byte[].class)).thenReturn(successResponse);

		// Capture the argument passed to save
		ArgumentCaptor<Artifact> artifactCaptor = ArgumentCaptor.forClass(Artifact.class);

		// When
		Resource result = artifactService.retrieveArtifact(ARTIFACT_PATH);

		// Then
		assertThat(result.getContentAsByteArray()).isEqualTo(ARTIFACT_CONTENT);
		verify(artifactRepository).findByPath(ARTIFACT_PATH); // Verify cache check
		verify(restClient.get()).uri(REMOTE_URL); // Verify remote call was made
		verify(restClient.get().uri(REMOTE_URL), times(2)).headers(any());
		verify(restClient.get().uri(REMOTE_URL).headers(any()), times(2)).retrieve();


		// Verify artifact was saved to database
		verify(artifactRepository).save(artifactCaptor.capture());
		Artifact savedArtifact = artifactCaptor.getValue();
		assertThat(savedArtifact.path()).isEqualTo(ARTIFACT_PATH);
		assertThat(savedArtifact.content()).isEqualTo(ARTIFACT_CONTENT);
		assertThat(savedArtifact.contentType()).isEqualTo("application/octet-stream"); // Derived
																						// by
																						// MediaTypeUtil
		assertThat(savedArtifact.lastModified()).isNotNull();
	}

	@Test
	void retrieveArtifact_notFoundInCache_remoteReturnsNotFound() {
		RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
		RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
		RestClient.RequestBodyUriSpec requestBodyUriSpecHeaders = mock(RestClient.RequestBodyUriSpec.class);
		RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
		when(restClient.get()).thenReturn(requestHeadersUriSpec);

		when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpecHeaders);
		when(requestBodyUriSpecHeaders.retrieve()).thenReturn(responseSpec);
		// Given: Artifact not in database
		when(artifactRepository.findByPath(ARTIFACT_PATH)).thenReturn(Optional.empty());

		// And: Remote call returns 404
		when(restClient.get().uri(REMOTE_URL).headers(any()).retrieve().toEntity(byte[].class))
			.thenThrow(new RestClientResponseException("Not Found", HttpStatus.NOT_FOUND.value(), "Not Found", null,
					null, null));

		// When / Then: Expect RestClientResponseException to be rethrown
		assertThrows(RestClientResponseException.class, () -> artifactService.retrieveArtifact(ARTIFACT_PATH));

		verify(artifactRepository).findByPath(ARTIFACT_PATH);
		verify(restClient.get()).uri(REMOTE_URL);
		verify(artifactRepository, never()).save(any(Artifact.class)); // Should not save
																		// on 404
	}

	@Test
	void retrieveArtifact_withRemoteAuth_configuresRestClient() throws IOException {
		RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
		RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
		RestClient.RequestBodyUriSpec requestBodyUriSpecHeaders = mock(RestClient.RequestBodyUriSpec.class);
		RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
		when(restClient.get()).thenReturn(requestHeadersUriSpec);

		when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpecHeaders);
		when(requestBodyUriSpecHeaders.retrieve()).thenReturn(responseSpec);
		// Given: Config has username and password
		when(springEnterpriseProxyProperties.remoteRepoUsername()).thenReturn("testuser");
		when(springEnterpriseProxyProperties.remoteRepoPassword()).thenReturn("testpass");

		when(restClientBuilder.requestFactory(any(JdkClientHttpRequestFactory.class))).thenReturn(restClientBuilder);

		// Reinitialize service to pick up auth config
		ArtifactService authService = new ArtifactService(springEnterpriseProxyProperties, restClientBuilder,
				artifactRepository);

		// Given: Artifact not in database
		when(artifactRepository.findByPath(ARTIFACT_PATH)).thenReturn(Optional.empty());

		// And: Remote call succeeds
		ResponseEntity<byte[]> successResponse = new ResponseEntity<>(ARTIFACT_CONTENT, HttpStatus.OK);
		when(restClient.get().uri(REMOTE_URL).headers(any()).retrieve().toEntity(byte[].class)).thenReturn(successResponse);

		// When
		authService.retrieveArtifact(ARTIFACT_PATH);

		// Then: Verify restClientBuilder.build() was called to apply default headers
		// This implicitly checks if .defaultHeader() was potentially called on the
		// builder
		verify(restClientBuilder, times(3)).build(); // Once in initial setup, once when
														// authService created

		// Verify the remote call still happens
		verify(restClient.get()).uri(REMOTE_URL);
	}

}