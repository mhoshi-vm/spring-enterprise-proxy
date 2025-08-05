package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;

import java.util.UUID;

@Configuration
class ArtifactConfig {

	@Bean
	BeforeConvertCallback<Artifact> beforeConvertCallback() {

		return (artifact) -> {
			if (artifact.id() == null) {
				artifact = new Artifact(UUID.nameUUIDFromBytes(artifact.path().getBytes()), artifact.path(),
						artifact.content(), artifact.contentType(), artifact.lastModified());
			}
			return artifact;
		};
	}

}
