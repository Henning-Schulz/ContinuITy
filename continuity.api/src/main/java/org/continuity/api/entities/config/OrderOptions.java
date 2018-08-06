package org.continuity.api.entities.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class OrderOptions {

	@JsonProperty("workload-model-type")
	@JsonInclude(Include.NON_NULL)
	private WorkloadModelType workloadModelType;

	@JsonProperty("load-test-type")
	@JsonInclude(Include.NON_NULL)
	private LoadTestType loadTestType;

	@JsonProperty("num-users")
	@JsonInclude(Include.NON_NULL)
	private Integer numUsers;

	@JsonInclude(Include.NON_NULL)
	private Long duration;

	@JsonInclude(Include.NON_NULL)
	private Integer rampup;

	public WorkloadModelType getWorkloadModelType() {
		return workloadModelType;
	}

	public void setWorkloadModelType(WorkloadModelType workloadModelType) {
		this.workloadModelType = workloadModelType;
	}

	public LoadTestType getLoadTestType() {
		return loadTestType;
	}

	public void setLoadTestType(LoadTestType loadTestType) {
		this.loadTestType = loadTestType;
	}

	public Integer getNumUsers() {
		return numUsers;
	}

	public void setNumUsers(int numUsers) {
		this.numUsers = numUsers;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public Integer getRampup() {
		return rampup;
	}

	public void setRampup(int rampup) {
		this.rampup = rampup;
	}

	public PropertySpecification toProperties() {
		PropertySpecification props = new PropertySpecification();

		props.setDuration(duration);
		props.setNumUsers(numUsers);
		props.setRampup(rampup);

		return props;
	}

	public static enum WorkloadModelType {
		WESSBAS;

		@JsonCreator
		public static OrderGoal fromPrettyString(String key) {
			return key == null ? null : OrderGoal.valueOf(key.toUpperCase());
		}

		@JsonValue
		public String toPrettyString() {
			return toString().toLowerCase();
		}
	}

	public static enum LoadTestType {
		JMETER(true), BENCHFLOW(false);

		private final boolean canExecute;

		private LoadTestType(boolean canExecute) {
			this.canExecute = canExecute;
		}

		public boolean canExecute() {
			return canExecute;
		}

		@JsonCreator
		public static OrderGoal fromPrettyString(String key) {
			return key == null ? null : OrderGoal.valueOf(key.toUpperCase());
		}

		@JsonValue
		public String toPrettyString() {
			return toString().toLowerCase();
		}
	}

}
