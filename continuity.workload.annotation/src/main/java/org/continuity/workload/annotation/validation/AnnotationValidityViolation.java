package org.continuity.workload.annotation.validation;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationValidityViolation {

	private AnnotationValidityViolationType type;

	private ModelElementReference referenced;

	public AnnotationValidityViolation(AnnotationValidityViolationType type, ModelElementReference referenced) {
		this.type = type;
		this.referenced = referenced;
	}

	public AnnotationValidityViolation(AnnotationValidityViolationType type) {
		this(type, null);
	}

	/**
	 * Gets {@link #type}.
	 *
	 * @return {@link #type}
	 */
	public AnnotationValidityViolationType getType() {
		return this.type;
	}

	/**
	 * Gets {@link #referenced}.
	 *
	 * @return {@link #referenced}
	 */
	public ModelElementReference getReferenced() {
		return this.referenced;
	}

	/**
	 * Sets {@link #referenced}.
	 *
	 * @param referenced
	 *            New value for {@link #referenced}
	 */
	public void setReferencedId(ModelElementReference referenced) {
		this.referenced = referenced;
	}

}
