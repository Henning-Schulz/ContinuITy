package org.continuity.workload.annotation.validation;

/**
 * @author Henning Schulz
 *
 */
public enum AnnotationValidityViolationType {

	// System changed

	INTERFACE_CHANGED("The interface has changed."), INTERFACE_REMOVED("The interface has been removed."), INTERFACE_ADDED("A new interface has been added."), PARAMETER_REMOVED(
			"The parameter has been removed."), PARAMETER_ADDED(
			"A new parameter has been added."),

	// Annotation changed

	ILLEAL_INTERFACE_REFERENCE("The reference to the interface is not valid."), ILLEGAL_PARAMETER_REFERENCE("The reference to the parameter is not valid.");

	private final String prettyName;

	private final String description;

	private AnnotationValidityViolationType(String description) {
		String tmp = name().replace("_", " ").toLowerCase();
		this.prettyName = tmp.substring(0, 1).toUpperCase() + tmp.substring(1);
		this.description = description;
	}

	/**
	 * Gets {@link #prettyName}.
	 *
	 * @return {@link #prettyName}
	 */
	public String prettyName() {
		return this.prettyName;
	}

	/**
	 * Gets {@link #description}.
	 *
	 * @return {@link #description}
	 */
	public String description() {
		return this.description;
	}

	public String getMessage() {
		return prettyName() + ": " + description();
	}

}
