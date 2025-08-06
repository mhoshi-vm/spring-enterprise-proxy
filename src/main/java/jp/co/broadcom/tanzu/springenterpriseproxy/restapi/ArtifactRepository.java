package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;
import java.util.UUID;


interface ArtifactRepository extends ListCrudRepository<Artifact, UUID> {

	Optional<Artifact> findByPath(String path);

}
