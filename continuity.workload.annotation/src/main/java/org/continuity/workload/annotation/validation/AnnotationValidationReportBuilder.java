package org.continuity.workload.annotation.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.continuity.annotation.dsl.WeakReference;
import org.continuity.annotation.dsl.ann.InterfaceAnnotation;
import org.continuity.annotation.dsl.ann.ParameterAnnotation;
import org.continuity.annotation.dsl.system.Parameter;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationValidationReportBuilder {

	/**
	 * Affected annotation --> Set of violations
	 */
	private final Map<ModelElementReference, Set<AnnotationValidityViolation>> violations = new HashMap<>();

	/**
	 * Referenced --> Violation
	 */
	private final Map<ModelElementReference, AnnotationValidityViolation> violationsPerReferenced = new HashMap<>();

	public void addViolation(AnnotationValidityViolation violation) {
		violationsPerReferenced.put(violation.getReferenced(), violation);
	}

	public void addViolation(ModelElementReference affected, AnnotationValidityViolation violation) {
		getViolationSet(affected).add(violation);
	}

	public void resolveParameterAnnotation(ParameterAnnotation annotation) {
		AnnotationValidityViolation violation = violationsPerReferenced.get(new ModelElementReference(annotation.getAnnotatedParameter()));

		if (violation != null) {
			ModelElementReference ref = new ModelElementReference(annotation);
			getViolationSet(ref).add(violation);
		}
	}

	/**
	 * Resolves the violations affecting the specified interface annotation. <br>
	 * <b>Note:</b> The annotated interface has to be resolved (call
	 * {@link WeakReference#resolve(org.continuity.annotation.dsl.ContinuityModelElement)}.
	 *
	 * @param annotation
	 *            The interface annotation.
	 */
	public void resolveInterfaceAnnotation(InterfaceAnnotation annotation) {
		AnnotationValidityViolation violation = violationsPerReferenced.get(new ModelElementReference(annotation.getAnnotatedInterface()));
		ModelElementReference ref = new ModelElementReference(annotation);

		if (violation != null) {
			getViolationSet(ref).add(violation);
		}

		for (Parameter parameter : annotation.getAnnotatedInterface().getReferred().getParameters()) {
			violation = violationsPerReferenced.get(new ModelElementReference(parameter));

			if (violation != null) {
				getViolationSet(ref).add(violation);
			}
		}
	}

	private Set<AnnotationValidityViolation> getViolationSet(ModelElementReference reference) {
		Set<AnnotationValidityViolation> violationSet = violations.get(reference);

		if (violationSet == null) {
			violationSet = new HashSet<>();
			violations.put(reference, violationSet);
		}

		return violationSet;
	}

	public Map<ModelElementReference, Set<AnnotationValidityViolation>> buildReport() {
		Map<ModelElementReference, Set<AnnotationValidityViolation>> report = new HashMap<>();
		report.putAll(violations);

		if (!violationsPerReferenced.isEmpty()) {
			Set<AnnotationValidityViolation> violationSet = getViolationSet(new ModelElementReference("", "System changes"));
			violationSet.addAll(violationsPerReferenced.values());
		}

		return report;
	}

}
