package org.continuity.api.entities.report;

import java.util.Set;

import org.continuity.api.entities.exchange.ArtifactExchangeModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "order-id", "number", "max", "testing-context", "successful", "error", "created-artifacts", "internal-artifacts" })
public class OrderReport {

	@JsonProperty("order-id")
	private String orderId;

	private int number;

	private int max;

	@JsonProperty("testing-context")
	@JsonInclude(Include.NON_EMPTY)
	private Set<String> testingContext;

	@JsonProperty("artifacts")
	private ArtifactExchangeModel artifacts;

	private boolean successful;

	@JsonInclude(Include.NON_NULL)
	private String error;

	public OrderReport() {
	}

	public OrderReport(String orderId, Set<String> testingContext, ArtifactExchangeModel internalArtifacts, boolean successful, String error) {
		this.orderId = orderId;
		this.testingContext = testingContext;
		this.artifacts = internalArtifacts;
		this.successful = successful;
		this.error = error;
	}

	public static OrderReport asSuccessful(String orderId, Set<String> testingContext, ArtifactExchangeModel internalArtifacts) {
		return new OrderReport(orderId, testingContext, internalArtifacts, true, null);
	}

	public static OrderReport asError(String orderId, ArtifactExchangeModel internalArtifacts, String error) {
		return new OrderReport(orderId, null, internalArtifacts, false, error);
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public Set<String> getTestingContext() {
		return testingContext;
	}

	public void setTestingContext(Set<String> testingContext) {
		this.testingContext = testingContext;
	}

	public ArtifactExchangeModel getArtifacts() {
		return artifacts;
	}

	public void setArtifacts(ArtifactExchangeModel artifacts) {
		this.artifacts = artifacts;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	/**
	 * Returns the report number within the order (e.g., 1 of 5)
	 *
	 * @see #getMax()
	 */
	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public int getMax() {
		return max;
	}

	/**
	 * Returns the number of reports to be returned for one order.
	 *
	 * @see #getNumber()
	 */
	public void setMax(int max) {
		this.max = max;
	}

}
