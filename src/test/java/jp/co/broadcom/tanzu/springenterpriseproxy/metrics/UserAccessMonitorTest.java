package jp.co.broadcom.tanzu.springenterpriseproxy.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jp.co.broadcom.tanzu.springenterpriseproxy.metrics.UserAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat; // Recommended for fluent assertions

class UserAccessMonitorTest {

	private UserAccessMonitor userAccessMonitor;

	private MeterRegistry meterRegistry; // This will be our in-memory registry for
											// testing

	private static final String EVENT_COUNTER_NAME = "user.access.monitor";

	@BeforeEach
	void setUp() {
		// Initialize SimpleMeterRegistry before each test.
		// This ensures a clean state for each test method.
		meterRegistry = new SimpleMeterRegistry();
		userAccessMonitor = new UserAccessMonitor(meterRegistry);
	}

	@Test
	@DisplayName("Should create and increment a new counter for a unique user and path combination")
	void recordEvent_shouldCreateAndIncrementNewCounter() {
		// Given
		String user = "testUser1";
		String path = "/testPath1";
		UserAccess userAccess = new UserAccess(user, path);

		// When
		userAccessMonitor.recordEvent(userAccess);

		// Then
		// Find the counter using its name and tags
		Counter counter = meterRegistry.find(EVENT_COUNTER_NAME).tag("user", user).tag("path", path).counter();

		// Assert that the counter exists and its value is 1.0
		assertThat(counter).isNotNull();
		assertThat(counter.count()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Should increment an existing counter for the same user and path combination")
	void recordEvent_shouldIncrementExistingCounter() {
		// Given
		String user = "testUser2";
		String path = "/testPath2";

		// When - Call it multiple times for the same combination
		UserAccess userAccess = new UserAccess(user, path);

		userAccessMonitor.recordEvent(userAccess);
		; // First call
		userAccessMonitor.recordEvent(userAccess); // Second call
		userAccessMonitor.recordEvent(userAccess); // Third call

		// Then
		Counter counter = meterRegistry.find(EVENT_COUNTER_NAME).tag("user", user).tag("path", path).counter();

		// Assert that the counter exists and its value is 3.0
		assertThat(counter).isNotNull();
		assertThat(counter.count()).isEqualTo(3.0);
	}

	@Test
	@DisplayName("Should create separate counters for different user and path combinations")
	void recordEvent_shouldCreateSeparateCountersForDifferentCombinations() {
		// Given
		String user1 = "userA";
		String path1 = "/pathA";
		UserAccess userAccess = new UserAccess(user1, path1);

		String user2 = "userB";
		String path2 = "/pathB";

		String user3 = "userA"; // Same user, different path
		String path3 = "/pathC";

		UserAccess userAccess1 = new UserAccess(user1, path1);
		UserAccess userAccess2 = new UserAccess(user2, path2);
		UserAccess userAccess3 = new UserAccess(user3, path3);

		// When
		userAccessMonitor.recordEvent(userAccess1);
		userAccessMonitor.recordEvent(userAccess2);
		userAccessMonitor.recordEvent(userAccess3); // New combination

		// Then - Verify each counter individually
		Counter counter1 = meterRegistry.find(EVENT_COUNTER_NAME).tag("user", user1).tag("path", path1).counter();
		assertThat(counter1).isNotNull();
		assertThat(counter1.count()).isEqualTo(1.0);

		Counter counter2 = meterRegistry.find(EVENT_COUNTER_NAME).tag("user", user2).tag("path", path2).counter();
		assertThat(counter2).isNotNull();
		assertThat(counter2.count()).isEqualTo(1.0);

		Counter counter3 = meterRegistry.find(EVENT_COUNTER_NAME).tag("user", user3).tag("path", path3).counter();
		assertThat(counter3).isNotNull();
		assertThat(counter3.count()).isEqualTo(1.0);
	}

}