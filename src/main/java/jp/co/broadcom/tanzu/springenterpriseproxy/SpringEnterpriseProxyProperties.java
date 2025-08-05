package jp.co.broadcom.tanzu.springenterpriseproxy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@ConfigurationProperties(prefix = "spring.enterprise.proxy")
public record SpringEnterpriseProxyProperties(String remoteRepoUrl, String remoteRepoUsername,
		String remoteRepoPassword, boolean oauthEnabled, RSAPrivateKey jwtPrivateKey, RSAPublicKey jwtPublicKey) {
}
