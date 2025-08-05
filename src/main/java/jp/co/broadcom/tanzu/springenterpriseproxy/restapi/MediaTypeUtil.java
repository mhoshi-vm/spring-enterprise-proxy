package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

class MediaTypeUtil {

	private static final Map<String, MediaType> mediaTypeMap = new HashMap<>();

	static {
		// Common Maven artifact media types
		mediaTypeMap.put("jar", MediaType.APPLICATION_OCTET_STREAM); // application/java-archive
		// is not a
		// standard Spring
		// MediaType
		mediaTypeMap.put("pom", MediaType.TEXT_XML);
		mediaTypeMap.put("xml", MediaType.TEXT_XML);
		mediaTypeMap.put("md5", MediaType.TEXT_PLAIN);
		mediaTypeMap.put("sha1", MediaType.TEXT_PLAIN);
		mediaTypeMap.put("sha256", MediaType.TEXT_PLAIN);
		mediaTypeMap.put("sha512", MediaType.TEXT_PLAIN);
		mediaTypeMap.put("asc", MediaType.APPLICATION_OCTET_STREAM); // GPG signature
		mediaTypeMap.put("war", MediaType.APPLICATION_OCTET_STREAM);
		mediaTypeMap.put("ear", MediaType.APPLICATION_OCTET_STREAM);
		mediaTypeMap.put("zip", MediaType.APPLICATION_OCTET_STREAM);
		mediaTypeMap.put("txt", MediaType.TEXT_PLAIN);
		// Add more as needed, default to octet-stream for unknown
	}

	static MediaType getMediaTypeForFileName(String fileName) {
		String extension = "";
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
			extension = fileName.substring(dotIndex + 1).toLowerCase();
		}
		return mediaTypeMap.getOrDefault(extension, MediaType.APPLICATION_OCTET_STREAM);
	}

}
