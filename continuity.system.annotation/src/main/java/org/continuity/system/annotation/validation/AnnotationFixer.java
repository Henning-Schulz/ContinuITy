package org.continuity.system.annotation.validation;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.continuity.idpa.annotation.ExtractedInput;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.RegExExtraction;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.visitor.IdpaByClassSearcher;
import org.continuity.system.annotation.entities.AnnotationValidityReport;
import org.continuity.system.annotation.entities.AnnotationViolation;
import org.continuity.system.annotation.entities.AnnotationViolationType;

/**
 * Fixes broken annotations.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationFixer {

	/**
	 * Tries to fix the passed annotation based on the passed report. <br>
	 * <b>Note: The returned annotation can still be broken!</b>
	 *
	 * @param brokenAnnotation
	 *            The annotation to be fixed.
	 * @param report
	 *            The report holding the violations.
	 * @return An annotation that might be fixed.
	 */
	public ApplicationAnnotation createFixedAnnotation(ApplicationAnnotation brokenAnnotation, AnnotationValidityReport report) {
		if (!report.isBreaking()) {
			return brokenAnnotation;
		}

		return removeUnknownInterfaceReferences(brokenAnnotation, report);
	}

	private ApplicationAnnotation removeUnknownInterfaceReferences(ApplicationAnnotation brokenAnnotation, AnnotationValidityReport report) {
		Set<String> removedInterfaces = new HashSet<>();

		for (AnnotationViolation systemChange : report.getSystemChanges()) {
			if (systemChange.getType() == AnnotationViolationType.INTERFACE_REMOVED) {
				removedInterfaces.add(systemChange.getChangedElement().getId());
			}
		}

		ApplicationAnnotation fixedAnnotation = new ApplicationAnnotation();
		fixedAnnotation.setId(brokenAnnotation.getId() + "-wouir");
		fixedAnnotation.setInputs(brokenAnnotation.getInputs().stream().filter(input -> !(input instanceof ExtractedInput)).collect(Collectors.toList()));
		fixedAnnotation.setOverrides(brokenAnnotation.getOverrides());

		IdpaByClassSearcher<EndpointAnnotation> interfSearcher = new IdpaByClassSearcher<>(EndpointAnnotation.class,
				ann -> addInterfaceAnnotationIfNotRemoved(ann, fixedAnnotation, removedInterfaces));
		interfSearcher.visit(brokenAnnotation);

		IdpaByClassSearcher<ExtractedInput> inputSearcher = new IdpaByClassSearcher<>(ExtractedInput.class,
				input -> addExtracedInputIfNoReferenceRemoved(input, fixedAnnotation, removedInterfaces));
		inputSearcher.visit(brokenAnnotation);

		return fixedAnnotation;
	}

	private void addInterfaceAnnotationIfNotRemoved(EndpointAnnotation annotation, ApplicationAnnotation fixedAnnotation, Set<String> removedInterfaces) {
		if (!removedInterfaces.contains(annotation.getAnnotatedEndpoint().getId())) {
			fixedAnnotation.getEndpointAnnotations().add(annotation);
		}
	}

	private void addExtracedInputIfNoReferenceRemoved(ExtractedInput input, ApplicationAnnotation fixedAnnotation, Set<String> removedInterfaces) {
		boolean broken = false;

		for (RegExExtraction extraction : input.getExtractions()) {
			if (removedInterfaces.contains(extraction.getFrom().getId())) {
				broken = true;
				break;
			}
		}

		if (!broken) {
			fixedAnnotation.addInput(input);
		}
	}

}
