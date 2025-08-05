package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.enterprise.proxy")
public record SpringEnterpriseProxyProperties(String remoteRepoUrl, String remoteRepoUsername, String remoteRepoPassword, boolean oauthEnabled) {
}
