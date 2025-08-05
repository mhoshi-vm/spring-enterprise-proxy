package jp.co.broadcom.tanzu.springenterpriseproxy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@ConfigurationProperties(prefix = "spring.enterprise.proxy")
public record SpringEnterpriseProxyProperties(
//@formatter:off
		@DefaultValue("https://packages.broadcom.com/artifactory/tanzu-maven")
		String remoteRepoUrl,
		String remoteRepoUsername,
		String remoteRepoPassword,
		@DefaultValue("false")
		boolean oauthEnabled,
		@DefaultValue("classpath:private-key.pem")
		RSAPrivateKey jwtPrivateKey,
		@DefaultValue("classpath:public-key.pem")
		RSAPublicKey jwtPublicKey,
		@DefaultValue("false")
		boolean statsdMetricsEnabled
		//@formatter:on
) {
}
