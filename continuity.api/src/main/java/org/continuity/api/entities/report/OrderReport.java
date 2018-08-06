package org.continuity.api.entities.report;

import org.continuity.api.entities.links.LinkExchangeModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class OrderReport {

	private String orderId;

	private LinkExchangeModel artifacts;

	private boolean successful;

	@JsonInclude(Include.NON_NULL)
	private String error;

	public OrderReport() {
	}

	public OrderReport(String orderId, LinkExchangeModel artifacts, boolean successful, String error) {
		this.orderId = orderId;
		this.artifacts = artifacts;
		this.successful = successful;
		this.error = error;
	}

	public static OrderReport asSuccessful(String orderId, LinkExchangeModel artifacts) {
		return new OrderReport(orderId, artifacts, true, null);
	}

	public static OrderReport asError(String orderId, LinkExchangeModel artifacts, String error) {
		return new OrderReport(orderId, artifacts, false, error);
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public LinkExchangeModel getArtifacts() {
		return artifacts;
	}

	public void setArtifacts(LinkExchangeModel artifacts) {
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

}
