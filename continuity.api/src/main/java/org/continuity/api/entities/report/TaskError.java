package org.continuity.api.entities.report;

public enum TaskError {

	MISSING_SOURCE("Required source information is missing."), INTERNAL_ERROR("An internal error occured");

	private final String message;

	private TaskError(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return name() + ": " + message;
	}

}
