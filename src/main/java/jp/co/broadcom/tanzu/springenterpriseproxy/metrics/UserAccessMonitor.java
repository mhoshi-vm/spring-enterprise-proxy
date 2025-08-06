package jp.co.broadcom.tanzu.springenterpriseproxy.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
class UserAccessMonitor {

	private static final Logger log = LoggerFactory.getLogger(UserAccessMonitor.class);

	private static final String METRIC_NAME = "spring.enterprise.user.access.monitor";

	private final MeterRegistry meterRegistry;

	UserAccessMonitor(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	/**
	 * Records a new event by incrementing a tagged counter. Micrometer efficiently
	 * manages counter-instances: - If a counter with the exact name and tag combination
	 * already exists, it's reused. - Otherwise, a new counter-instance is created and
	 * registered.
	 * @param userAccess The path or resource associated with the event.
	 */
	@Async
	@EventListener
	void recordEvent(UserAccess userAccess) {
		// Use Counter.builder to create or retrieve a counter-instance with specific
		// tags.
		// Micrometer ensures that if a counter with this exact name and tag combination
		// already exists, it is returned. Otherwise, a new one is created.
		Counter eventCounter = Counter.builder(METRIC_NAME)
			.tag("user", userAccess.user()) // Add 'user' as a tag
			.tag("path", userAccess.path()) // Add 'path' as a tag
			.register(meterRegistry); // Register with the MeterRegistry

		eventCounter.increment(); // Increment the counter for this specific user/path
									// combination

		log.debug("Event recorded for user: '{}', path: '{}'. Current count for this combination: {}",
				userAccess.user(), userAccess.path(), (long) eventCounter.count());
	}

}
