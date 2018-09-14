package org.continuity.request.rates.model;

import java.util.Map;

import org.continuity.idpa.application.Endpoint;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A simple workload model holding a request rate per minute and a frequency of called endpoints.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "request-per-minute", "endpoints" })
public class RequestRatesModel {

	@JsonProperty("request-per-minute")
	private double requestPerMinute;

	private Map<Double, Endpoint<?>> endpoints;

	public double getRequestPerMinute() {
		return requestPerMinute;
	}

	public void setRequestPerMinute(double requestPerMinute) {
		this.requestPerMinute = requestPerMinute;
	}

	public Map<Double, Endpoint<?>> getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(Map<Double, Endpoint<?>> endpoints) {
		this.endpoints = endpoints;
	}

}
