package org.continuity.api.entities.artifact.session;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Represents a request within a session.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "id", "endpoint", "start-micros", "end-micros" })
public class SessionRequest implements Comparable<SessionRequest> {

	private static final String DELIM = ":";

	private String id;

	private String endpoint;

	@JsonProperty("start-micros")
	private long startMicros;

	@JsonProperty("end-micros")
	private long endMicros;

	@JsonIgnore
	private String sessionId;

	@JsonProperty("extended-information")
	@JsonView(ExtendedView.class)
	private ExtendedRequestInformation extendedInformation;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public long getStartMicros() {
		return startMicros;
	}

	public void setStartMicros(long startMicros) {
		this.startMicros = startMicros;
	}

	public long getEndMicros() {
		return endMicros;
	}

	public void setEndMicros(long endMicros) {
		this.endMicros = endMicros;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public ExtendedRequestInformation getExtendedInformation() {
		return extendedInformation;
	}

	public void setExtendedInformation(ExtendedRequestInformation extendedInformation) {
		this.extendedInformation = extendedInformation;
	}

	@JsonIgnore
	public String toSimpleLog() {
		return new StringBuilder().append("\"").append(endpoint).append("\"").append(DELIM).append(startMicros * 1000).append(DELIM).append(endMicros).toString();
	}

	@JsonIgnore
	public String toExtensiveLog() {
		if (extendedInformation == null) {
			throw new IllegalStateException("Cannot generate extensive log! There is no extended information present!");
		}

		StringBuilder log = new StringBuilder().append("\"").append(endpoint).append("\"").append(DELIM).append(startMicros * 1000).append(DELIM).append(endMicros);

		log.append(DELIM).append(extendedInformation.getUri());
		log.append(DELIM).append(extendedInformation.getPort());
		log.append(DELIM).append(extendedInformation.getHost());
		log.append(DELIM).append(extendedInformation.getProtocol());
		log.append(DELIM).append(extendedInformation.getMethod());
		log.append(DELIM).append(extendedInformation.getParameters());
		log.append(DELIM).append(extendedInformation.getEncoding());

		return log.toString();
	}

	@Override
	public int compareTo(SessionRequest other) {
		int startDiff = Long.signum(this.endMicros - other.endMicros);
		int endDiff = Long.signum(this.startMicros - other.startMicros);
		int endpointDiff = Integer.signum(compareRespectingNull(this.endpoint, other.endpoint));
		int idDiff = Long.signum(compareRespectingNull(this.id, other.id));

		return (8 * startDiff) + (4 * endDiff) + (2 * endpointDiff) + idDiff;
	}

	private <T> int compareRespectingNull(Comparable<T> first, T second) {
		if (first == second) {
			return 0;
		} else if (first == null) {
			return -1;
		} else {
			return first.compareTo(second);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(endMicros, endpoint, id, startMicros);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if ((obj == null) || !(obj instanceof SessionRequest)) {
			return false;
		}

		SessionRequest other = (SessionRequest) obj;
		return (endMicros == other.endMicros) && Objects.equals(endpoint, other.endpoint) && (id == other.id) && (startMicros == other.startMicros);
	}

	@Override
	public String toString() {
		return new StringBuilder().append(endpoint).append(" [").append(id).append("] (").append(startMicros).append(" - ").append(endMicros).append(")").toString();
	}

	public static class ExtendedView {

	}

}
