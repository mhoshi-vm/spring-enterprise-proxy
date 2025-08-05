package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpringEnterpriseProxyController.class) // Focuses on Spring MVC components
@AutoConfigureMockMvc(addFilters = false)
class SpringEnterpriseProxyControllerTest {

	private final String ARTIFACT_PATH = "/org/example/artifact/1.0/artifact-1.0.jar";

	private final byte[] ARTIFACT_CONTENT = "dummy jar content".getBytes();

	@Autowired
	private MockMvc mockMvc; // Used to perform HTTP requests

	@MockitoBean // Creates a Mockito mock and adds it to the Spring application context
	private ArtifactService artifactService;

	@BeforeEach
	void setUp() {
		// Reset mocks before each test
		Mockito.reset(artifactService);
	}

	@Test
	void testProxyMavenArtifact_Success() throws Exception {
		Resource mockResource = new ByteArrayResource(ARTIFACT_CONTENT);

		// Configure the mock service to return a resource when retrieveArtifact is called
		Mockito.when(artifactService.retrieveArtifact(ARTIFACT_PATH)).thenReturn(mockResource);

		mockMvc.perform(get("/spring-enterprise-proxy{artifactPath}", ARTIFACT_PATH))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM)) // Default
																					// for
																					// JAR
																					// in
																					// MediaTypeUtil
			.andExpect(content().bytes(ARTIFACT_CONTENT));

		// Verify that the service method was called exactly once with the correct path
		Mockito.verify(artifactService).retrieveArtifact(ARTIFACT_PATH);
	}

	@Test
	void testProxyMavenArtifact_NotFoundRemote() throws Exception {
		// Configure the mock service to throw RestClientResponseException for 404
		Mockito.when(artifactService.retrieveArtifact(ARTIFACT_PATH))
			.thenThrow(new RestClientResponseException("Not Found", HttpStatus.NOT_FOUND.value(), "Not Found", null,
					null, null));

		mockMvc.perform(get("/spring-enterprise-proxy{artifactPath}", ARTIFACT_PATH)).andExpect(status().isNotFound());

		Mockito.verify(artifactService).retrieveArtifact(ARTIFACT_PATH);
	}

	@Test
	void testProxyMavenArtifact_InternalServerError() throws Exception {
		// Configure the mock service to throw an IOException (simulating I/O issue)
		Mockito.when(artifactService.retrieveArtifact(ARTIFACT_PATH)).thenThrow(new IOException("Disk full"));

		mockMvc.perform(get("/spring-enterprise-proxy{artifactPath}", ARTIFACT_PATH))
			.andExpect(status().isInternalServerError());

		Mockito.verify(artifactService).retrieveArtifact(ARTIFACT_PATH);
	}

	@Test
	void testProxyMavenArtifact_OtherRestClientError() throws Exception {
		// Configure the mock service to throw RestClientResponseException for a different
		// error (e.g., 500)
		Mockito.when(artifactService.retrieveArtifact(ARTIFACT_PATH))
			.thenThrow(new RestClientResponseException("Internal Server Error",
					HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", null, null, null));

		mockMvc.perform(get("/spring-enterprise-proxy{artifactPath}", ARTIFACT_PATH))
			.andExpect(status().isInternalServerError());

		Mockito.verify(artifactService).retrieveArtifact(ARTIFACT_PATH);
	}

	@Test
	void testProxyMavenArtifact_PomFileContentType() throws Exception {
		String pomPath = "/org/example/artifact/1.0/artifact-1.0.pom";
		byte[] pomContent = "<project><artifactId>artifact</artifactId></project>".getBytes();
		Resource mockResource = new ByteArrayResource(pomContent);

		Mockito.when(artifactService.retrieveArtifact(pomPath)).thenReturn(mockResource);

		mockMvc.perform(get("/spring-enterprise-proxy{artifactPath}", pomPath))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.TEXT_XML)) // Correct content type
																	// for POM
			.andExpect(content().bytes(pomContent));
	}

}