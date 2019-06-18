package org.continuity.idpa;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;

/**
 * Represents an IDPA consisting of an application and an annotation.
 *
 * @author Henning Schulz
 *
 */
public class Idpa {

	private Application application;

	private ApplicationAnnotation annotation;

	public Idpa() {
	}

	public Idpa(Application application, ApplicationAnnotation annotation) {
		this.application = application;
		this.annotation = annotation;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	public ApplicationAnnotation getAnnotation() {
		return annotation;
	}

	public void setAnnotation(ApplicationAnnotation annotation) {
		this.annotation = annotation;
	}

}
