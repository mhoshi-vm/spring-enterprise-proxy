package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface ArtifactRepository extends ListCrudRepository<Artifact, UUID> {

	Optional<Artifact> findByPath(String path);

}
