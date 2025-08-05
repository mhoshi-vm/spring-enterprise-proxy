package jp.co.broadcom.tanzu.springenterpriseproxy.restapi;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.convert.converter.Converter;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@TestConfiguration
class ArtifactRepositoryTestConfig {

	@TestComponent
	@ConfigurationPropertiesBinding
	static class MyPrivateKeyConverter implements Converter<String, RSAPrivateKey> {
		@Override
		public RSAPrivateKey convert(String from) {
			return null;
		}
	}

	@TestComponent
	@ConfigurationPropertiesBinding
	static class MyPublicKeyConverter implements Converter<String, RSAPublicKey> {
		@Override
		public RSAPublicKey convert(String from) {
			return null;
		}
	}
}
