package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

public record Artifact(@Id String id, String path, byte[] content, String contentType, LocalDateTime lastModified) {

	Artifact(String path, byte[] content, String contentType, LocalDateTime lastModified) {
		this(null, path, content, contentType, lastModified);
	}
}
