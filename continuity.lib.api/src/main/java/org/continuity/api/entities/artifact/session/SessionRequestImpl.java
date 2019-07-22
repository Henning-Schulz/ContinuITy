package org.continuity.api.entities.artifact.session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Represents a request within a session.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "id", "endpoint", "start-micros", "end-micros" })
@JsonView(SessionView.Simple.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionRequestImpl extends AbstractSessionRequest implements SessionRequest {

	private String id;

	private String endpoint;

	private long startMicros;

	private long endMicros;

	private String sessionId;

	private ExtendedRequestInformation extendedInformation;

	@JsonIgnore
	public static boolean isPrePostProcessing(String endpoint) {
		return (endpoint != null) && (endpoint.startsWith(PREFIX_PRE_PROCESSING) || endpoint.startsWith(PREFIX_POST_PROCESSING));
	}

	@JsonIgnore
	public static boolean isPreProcessing(String endpoint) {
		return (endpoint != null) && endpoint.startsWith(PREFIX_PRE_PROCESSING);
	}

	@JsonIgnore
	public static boolean isPostProcessing(String endpoint) {
		return (endpoint != null) && endpoint.startsWith(PREFIX_POST_PROCESSING);
	}

	@Override
	public boolean isPrePostProcessing() {
		return isPrePostProcessing(endpoint);
	}

	@Override
	public boolean isPreProcessing() {
		return isPreProcessing(endpoint);
	}

	@Override
	public boolean isPostProcessing() {
		return isPostProcessing(endpoint);
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public long getStartMicros() {
		return startMicros;
	}

	public void setStartMicros(long startMicros) {
		this.startMicros = startMicros;
	}

	@Override
	public long getEndMicros() {
		return endMicros;
	}

	public void setEndMicros(long endMicros) {
		this.endMicros = endMicros;
	}

	@Override
	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public ExtendedRequestInformation getExtendedInformation() {
		return extendedInformation;
	}

	public void setExtendedInformation(ExtendedRequestInformation extendedInformation) {
		this.extendedInformation = extendedInformation;
	}

}
