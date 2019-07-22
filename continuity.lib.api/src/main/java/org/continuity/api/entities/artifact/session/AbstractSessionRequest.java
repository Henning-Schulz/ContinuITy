package org.continuity.api.entities.artifact.session;

import java.util.Objects;

public abstract class AbstractSessionRequest implements SessionRequest {

	private static final String DELIM = ":";

	@Override
	public String toSimpleLog() {
		return new StringBuilder().append("\"").append(getEndpoint()).append("\"").append(DELIM).append(getStartMicros() * 1000).append(DELIM).append(getEndMicros() * 1000).toString();
	}

	@Override
	public String toExtensiveLog() {
		if (getExtendedInformation() == null) {
			throw new IllegalStateException("Cannot generate extensive log! There is no extended information present!");
		}

		StringBuilder log = new StringBuilder().append("\"").append(getEndpoint()).append("\"").append(DELIM).append(getStartMicros() * 1000).append(DELIM).append(getEndMicros() * 1000);

		ExtendedRequestInformation extendedInformation = getExtendedInformation();

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
		int startDiff = Long.signum(this.getEndMicros() - other.getEndMicros());
		int endDiff = Long.signum(this.getStartMicros() - other.getStartMicros());
		int prePostDiff = Integer.signum(this.prePostIndex() - other.prePostIndex());
		int endpointDiff = Integer.signum(compareRespectingNull(this.getEndpoint(), other.getEndpoint()));
		int idDiff = Long.signum(compareRespectingNull(this.getId(), other.getId()));

		return (16 * startDiff) + (8 * endDiff) + (4 * prePostDiff) + (2 * endpointDiff) + idDiff;
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
		return Objects.hash(getEndMicros(), getEndpoint(), getId(), getStartMicros());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if ((obj == null) || !(obj instanceof SessionRequestImpl)) {
			return false;
		}

		SessionRequestImpl other = (SessionRequestImpl) obj;
		return (getEndMicros() == other.getEndMicros()) && Objects.equals(getEndpoint(), other.getEndpoint()) && (getId() == other.getId()) && (getStartMicros() == other.getStartMicros());
	}

	@Override
	public String toString() {
		return new StringBuilder().append(getEndpoint()).append(" [").append(getId()).append("] (").append(getStartMicros()).append(" - ").append(getEndMicros()).append(") @")
				.append(getClass().getSimpleName()).toString();
	}

}
