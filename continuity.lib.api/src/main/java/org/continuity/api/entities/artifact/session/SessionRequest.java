package org.continuity.api.entities.artifact.session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

public interface SessionRequest extends Comparable<SessionRequest> {

	String PREFIX_PRE_PROCESSING = "PRE_PROCESSING#";
	String PREFIX_POST_PROCESSING = "POST_PROCESSING#";

	@JsonIgnore
	boolean isPrePostProcessing();

	@JsonIgnore
	boolean isPreProcessing();

	@JsonIgnore
	boolean isPostProcessing();

	String getId();

	String getEndpoint();

	@JsonProperty("start-micros")
	long getStartMicros();

	@JsonProperty("end-micros")
	long getEndMicros();

	@JsonIgnore
	String getSessionId();

	@JsonProperty("extended-information")
	@JsonView(SessionView.Extended.class)
	ExtendedRequestInformation getExtendedInformation();

	@JsonIgnore
	String toSimpleLog();

	@JsonIgnore
	String toExtensiveLog();

	@Override
	int compareTo(SessionRequest other);

	@Override
	int hashCode();

	@Override
	boolean equals(Object obj);

	@JsonIgnore
	default int prePostIndex() {
		return isPrePostProcessing() ? -1 : (isPostProcessing() ? 1 : 0);
	}

}