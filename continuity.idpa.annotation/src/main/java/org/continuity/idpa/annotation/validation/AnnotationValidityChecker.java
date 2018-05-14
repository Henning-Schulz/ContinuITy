package org.continuity.idpa.annotation.validation;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.annotation.RegExExtraction;
import org.continuity.idpa.annotation.entities.AnnotationValidityReport;
import org.continuity.idpa.annotation.entities.AnnotationViolation;
import org.continuity.idpa.annotation.entities.AnnotationViolationType;
import org.continuity.idpa.annotation.entities.ModelElementReference;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.visitor.IdpaByClassSearcher;

/**
 * Compares system models and annotations against a base system model. E.g., can be used to
 * determine the differences of an old system model an the new one or to compare an annotation
 * against the new system model.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationValidityChecker {

	private final Application newSystemModel;

	private final AnnotationValidationReportBuilder reportBuilder = new AnnotationValidationReportBuilder();

	/**
	 * Creates an instance with the current system model as base.
	 *
	 * @param newSystemModel
	 *            The current system model.
	 */
	public AnnotationValidityChecker(Application newSystemModel) {
		this.newSystemModel = newSystemModel;
	}

	public void registerSystemChanges(AnnotationValidityReport systemChangeReport) {
		reportBuilder.addViolations(systemChangeReport.getSystemChanges());
	}

	/**
	 * Compares an annotation to the stored system model and reports broken references.
	 *
	 * @param annotation
	 *            An annotation.
	 */
	public void checkAnnotation(ApplicationAnnotation annotation) {
		checkAnnotationInternally(annotation);
		checkAnnotationForExternalReferences(annotation);
	}

	private void checkAnnotationInternally(ApplicationAnnotation annotation) {
		IdpaByClassSearcher<ParameterAnnotation> paramSearcher = new IdpaByClassSearcher<>(ParameterAnnotation.class, ann -> {
			List<Input> inputs = annotation.getInputs();
			boolean inputNotPresent = inputs.stream().map(Input::getId).filter(id -> Objects.equals(id, ann.getInput().getId())).collect(Collectors.toList()).isEmpty();

			if (inputNotPresent) {
				ModelElementReference inputRef = new ModelElementReference(ann.getInput());
				ModelElementReference annRef = new ModelElementReference(ann);
				reportBuilder.addViolation(annRef, new AnnotationViolation(AnnotationViolationType.ILLEGAL_INTERNAL_REFERENCE, inputRef));
			}
		});

		paramSearcher.visit(annotation);
	}

	private void checkAnnotationForExternalReferences(ApplicationAnnotation annotation) {
		IdpaByClassSearcher<EndpointAnnotation> interfaceSearcher = new IdpaByClassSearcher<>(EndpointAnnotation.class, ann -> {
			Endpoint<?> interf = ann.getAnnotatedEndpoint().resolve(newSystemModel);

			if (interf == null) {
				ModelElementReference interfRef = new ModelElementReference(ann.getAnnotatedEndpoint());
				ModelElementReference annRef = new ModelElementReference(ann);
				reportBuilder.addViolation(annRef, new AnnotationViolation(AnnotationViolationType.ILLEAL_INTERFACE_REFERENCE, interfRef));
			}

			reportBuilder.resolveInterfaceAnnotation(ann);
		});

		interfaceSearcher.visit(annotation);

		IdpaByClassSearcher<ParameterAnnotation> paramSearcher = new IdpaByClassSearcher<>(ParameterAnnotation.class, ann -> {
			Parameter param = ann.getAnnotatedParameter().resolve(newSystemModel);

			if (param == null) {
				ModelElementReference paramRef = new ModelElementReference(ann.getAnnotatedParameter());
				ModelElementReference annRef = new ModelElementReference(ann);
				reportBuilder.addViolation(annRef, new AnnotationViolation(AnnotationViolationType.ILLEGAL_PARAMETER_REFERENCE, paramRef));
			}

			reportBuilder.resolveParameterAnnotation(ann);
		});

		paramSearcher.visit(annotation);

		IdpaByClassSearcher<RegExExtraction> extractionSearcher = new IdpaByClassSearcher<>(RegExExtraction.class, extraction -> {
			Endpoint<?> interf = extraction.getFrom().resolve(newSystemModel);

			if (interf == null) {
				ModelElementReference interfRef = new ModelElementReference(extraction.getFrom());
				ModelElementReference annRef = new ModelElementReference(extraction);
				reportBuilder.addViolation(annRef, new AnnotationViolation(AnnotationViolationType.ILLEAL_INTERFACE_REFERENCE, interfRef));
			}
		});

		extractionSearcher.visit(annotation);
	}

	/**
	 * Gets a report based on the evaluations done before.
	 *
	 * @return The report.
	 */
	public AnnotationValidityReport getReport() {
		return reportBuilder.buildReport();
	}
}