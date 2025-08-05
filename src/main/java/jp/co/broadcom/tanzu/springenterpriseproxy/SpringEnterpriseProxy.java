package jp.co.broadcom.tanzu.springenterpriseproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SpringEnterpriseProxyProperties.class)
class SpringEnterpriseProxy {

	public static void main(String[] args) {
		SpringApplication.run(SpringEnterpriseProxy.class, args);
	}

}
