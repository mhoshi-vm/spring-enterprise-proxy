package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJdbcTest
@Import(ArtifactConfig.class)
class ArtifactRepositoryTest {

	private final String ARTIFACT_PATH = "/com/example/lib/2.0/lib-2.0.jar";

	private final byte[] ARTIFACT_CONTENT = "test-artifact-data".getBytes();

	private final String CONTENT_TYPE = "application/java-archive";

	@Autowired
	private ArtifactRepository artifactRepository;

	private Artifact testArtifact;

	@BeforeEach
	void setUp() {

		testArtifact = new Artifact(ARTIFACT_PATH, ARTIFACT_CONTENT, CONTENT_TYPE, LocalDateTime.now());
	}

	@Test
	void testSaveAndFindArtifact() {
		// Save the artifact
		artifactRepository.save(testArtifact);

		// Find it by ID
		Optional<Artifact> foundArtifact = artifactRepository
			.findById(UUID.nameUUIDFromBytes(ARTIFACT_PATH.getBytes()));

		assertThat(foundArtifact).isPresent();
		assertThat(foundArtifact.get().path()).isEqualTo(ARTIFACT_PATH);
		assertThat(foundArtifact.get().content()).isEqualTo(ARTIFACT_CONTENT);
		assertThat(foundArtifact.get().contentType()).isEqualTo(CONTENT_TYPE);
	}

	@Test
	void testFindByPath() {
		// Save the artifact
		artifactRepository.save(testArtifact);

		// Find by path
		Optional<Artifact> foundArtifact = artifactRepository.findByPath(ARTIFACT_PATH);

		assertThat(foundArtifact).isPresent();
		assertThat(foundArtifact.get().path()).isEqualTo(ARTIFACT_PATH);
	}

	@Test
	void testFindByPath_NotFound() {
		// No artifact saved
		Optional<Artifact> foundArtifact = artifactRepository.findByPath("non/existent/path.jar");
		assertThat(foundArtifact).isNotPresent();
	}

	@Test
	void testSaveDuplicatePath_ThrowsException() {
		// Save the first artifact
		artifactRepository.save(testArtifact);

		// Try to save another artifact with the same path
		Artifact duplicateArtifact = new Artifact(ARTIFACT_PATH, "different-content".getBytes(), "text/plain",
				LocalDateTime.now());

		assertThrows(DbActionExecutionException.class, () -> artifactRepository.save(duplicateArtifact));
	}

	@Test
	void testSaveDuplicatePath_update() {
		// Save the first artifact
		artifactRepository.save(testArtifact);

		// Save another artifact with the same path but with id
		Artifact duplicateArtifact = new Artifact(UUID.nameUUIDFromBytes(ARTIFACT_PATH.getBytes()), ARTIFACT_PATH,
				"different-content".getBytes(), "text/plain", LocalDateTime.now());
		artifactRepository.save(duplicateArtifact);

		// Clarify contents has been updated
		Optional<Artifact> foundArtifact = artifactRepository.findByPath(ARTIFACT_PATH);

		assertThat(foundArtifact).isPresent();
		assertThat(foundArtifact.get().path()).isEqualTo(ARTIFACT_PATH);
		assertThat(foundArtifact.get().content()).isEqualTo("different-content".getBytes());
		assertThat(foundArtifact.get().contentType()).isEqualTo("text/plain");
	}

}