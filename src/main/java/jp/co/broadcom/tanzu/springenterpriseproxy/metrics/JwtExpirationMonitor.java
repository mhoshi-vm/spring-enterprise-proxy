package jp.co.broadcom.tanzu.springenterpriseproxy.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;

@Component
class JwtExpirationMonitor {

	private static final Logger log = LoggerFactory.getLogger(JwtExpirationMonitor.class);

	private static final String METRIC_NAME = "spring.enterprise.proxy.password.time.until.expiration.seconds";

	private final ObjectMapper objectMapper;

	private final String jwtToken;

	private final MeterRegistry meterRegistry;

	/**
	 * Constructs a new JwtExpirationMonitor.
	 * @param meterRegistry The Micrometer MeterRegistry to register gauges with.
	 * @param jwtToken The JWT token string whose expiration is to be monitored, typically
	 * sourced from a Spring property like `maven.proxy.remote-repo-password`.
	 */
	JwtExpirationMonitor(MeterRegistry meterRegistry,
			@Value("${spring.enterprise.proxy.remote-repo-password}") String jwtToken) {
		this.objectMapper = new ObjectMapper();
		this.meterRegistry = meterRegistry;
		this.jwtToken = jwtToken;
		registerJwtExpirationGauge();
	}

	/**
	 * Decodes a JWT token by base64-decoding its payload and extracting the expiration
	 * timestamp. This method *does not* validate the token's signature or integrity. It
	 * merely parses the payload part of the token. Use with caution.
	 * @return The expiration timestamp (Unix epoch seconds) if found, otherwise
	 * {@code null}.
	 * @throws JwtDecodingException if the token format is invalid or payload cannot be
	 * parsed.
	 */
	public Long getExpirationFromJwt() {
		String[] parts = this.jwtToken.split("\\.");
		if (parts.length < 2) {
			throw new JwtDecodingException("Invalid JWT token format. Must have at least two parts (header.payload).");
		}
		try {
			// Base64Url decode the payload part (the second part of the JWT)
			String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
			JsonNode rootNode = objectMapper.readTree(payloadJson);
			if (rootNode.has("exp")) {
				Long exp = rootNode.get("exp").asLong();
				return rootNode.get("exp").asLong() - Instant.now().getEpochSecond();
			}
			return null; // 'exp' claim isn't found in the payload
		}
		catch (IOException e) {
			throw new JwtDecodingException("Failed to parse JWT payload as JSON.", e);
		}
		catch (IllegalArgumentException e) {
			throw new JwtDecodingException("Failed to base64-decode JWT payload. Invalid encoding.", e);
		}
		catch (Exception e) {
			throw new JwtDecodingException("An unexpected error occurred while decoding JWT.", e);
		}
	}

	/**
	 * Custom exception thrown when there is an issue decoding or parsing a JWT token.
	 */
	public static class JwtDecodingException extends RuntimeException {

		public JwtDecodingException(String message) {
			super(message);
		}

		public JwtDecodingException(String message, Throwable cause) {
			super(message, cause);
		}

	}

	/**
	 * Registers a Micrometer Gauge that reports the time until the JWT token expires in
	 * seconds. The gauge periodically queries the JWT's expiration timestamp and
	 * calculates the difference with the current time. If the 'exp' claim is not found or
	 * token decoding fails, it reports 0 (indicating no valid expiration time or already
	 * expired/invalid).
	 */
	private void registerJwtExpirationGauge() {
		// The gauge will periodically execute the provided lambda to determine its
		// current value.
		meterRegistry.gauge(METRIC_NAME, Tags.of("token.source", "maven.proxy"), this,
				JwtExpirationMonitor::getExpirationFromJwt);
		log.info("Registered Actuator metric: '{}'. Monitoring 'maven.proxy' JWT expiration.", METRIC_NAME);
	}

}