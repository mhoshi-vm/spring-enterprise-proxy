package jp.co.broadcom.tanzu.springenterpriseproxy.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@ConditionalOnProperty(value = "spring.enterprise.proxy.stats-metrics-enabled")
public class StatsdConsoleConfig {

	@Bean
	public MeterRegistry datadogStatsdConsoleRegistry() {

		StatsdConfig config = new StatsdConfig() {
			@Override
			public String get(String key) {
				return null;
			}

			@Override
			public StatsdFlavor flavor() {
				return StatsdFlavor.DATADOG;
			}
		};

		Consumer<String> consolePrinter = System.out::println;

		return StatsdMeterRegistry.builder(config).clock(Clock.SYSTEM).lineSink(consolePrinter).build();
	}

}
