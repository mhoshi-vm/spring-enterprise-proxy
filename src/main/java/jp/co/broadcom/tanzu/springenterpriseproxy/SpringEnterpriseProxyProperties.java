package jp.co.broadcom.tanzu.springenterpriseproxy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@ConfigurationProperties(prefix = "spring.enterprise.proxy")
public record SpringEnterpriseProxyProperties(
//@formatter:off
		String remoteRepoUrl,
		String remoteRepoUsername,
		String remoteRepoPassword,
		Long expiry,
		boolean oauthEnabled,
		RSAPrivateKey jwtPrivateKey,
		RSAPublicKey jwtPublicKey,
		boolean statsdMetricsEnabled
		//@formatter:on
) {}
