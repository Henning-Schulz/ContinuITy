package org.continuity.api.entities.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderGoal {

	CREATE_SESSION_LOGS, CREATE_WORKLOAD_MODEL(CREATE_SESSION_LOGS), CREATE_LOAD_TEST(CREATE_WORKLOAD_MODEL), EXECUTE_LOAD_TEST(CREATE_LOAD_TEST);

	private static final Map<String, OrderGoal> prettyStringToGoal = new HashMap<>();

	static {
		for (OrderGoal goal : values()) {
			prettyStringToGoal.put(goal.toPrettyString(), goal);
		}
	}

	private final Optional<OrderGoal> required;

	private OrderGoal() {
		this.required = Optional.empty();
	}

	private OrderGoal(OrderGoal required) {
		this.required = Optional.of(required);
	}

	public Optional<OrderGoal> getRequired() {
		return required;
	}

	@JsonCreator
	public static OrderGoal fromPrettyString(String key) {
		return prettyStringToGoal.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().replace("_", "-").toLowerCase();
	}

}
