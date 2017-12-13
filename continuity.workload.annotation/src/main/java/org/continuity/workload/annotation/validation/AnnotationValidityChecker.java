package org.continuity.workload.annotation.validation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.visitor.ContinuityByClassSearcher;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationValidityChecker {

	private final SystemModel newSystemModel;

	private final AnnotationValidationReportBuilder reportBuilder = new AnnotationValidationReportBuilder();

	public AnnotationValidityChecker(SystemModel newSystemModel) {
		this.newSystemModel = newSystemModel;
	}

	public void checkOldSystemModel(SystemModel oldSystemModel) {
		final Set<ModelElementReference> visited = new HashSet<>();
		ContinuityByClassSearcher<ServiceInterface<?>> searcher = new ContinuityByClassSearcher<>(ServiceInterface.GENERIC_TYPE, inter -> checkInterface(inter, oldSystemModel, visited));
		searcher.visit(newSystemModel);

		searcher = new ContinuityByClassSearcher<>(ServiceInterface.GENERIC_TYPE, inter -> reportRemovedInterface(inter, visited));
		searcher.visit(oldSystemModel);
	}

	private boolean checkInterface(ServiceInterface<?> newInterf, SystemModel oldSystemModel, Set<ModelElementReference> visited) {
		final ModelElementReference ref = new ModelElementReference(newInterf);
		ref.setId(null);
		ContinuityByClassSearcher<ServiceInterface<?>> searcher = new ContinuityByClassSearcher<>(ServiceInterface.GENERIC_TYPE, oldInterf -> {
			if (oldInterf.getId().equals(newInterf.getId())) {
				ref.setId(oldInterf.getId());
			}
		});
		searcher.visit(oldSystemModel);

		if (ref.getId() == null) {
			ref.setId(newInterf.getId());
			reportBuilder.addViolation(new AnnotationValidityViolation(AnnotationValidityViolationType.INTERFACE_ADDED, ref));
		} else {
			// Check if something changed (path, port, ...) --> INTERFACE_CHANGED
			// Check parameters
			// Add interface from oldSystemModel to visited
		}

		return true;
	}

	private boolean reportRemovedInterface(ServiceInterface<?> oldInterf, Set<ModelElementReference> visited) {
		ModelElementReference ref = new ModelElementReference(oldInterf);
		if (!visited.contains(ref)) {
			reportBuilder.addViolation(new AnnotationValidityViolation(AnnotationValidityViolationType.INTERFACE_REMOVED, ref));
		}

		return true;
	}

	public void checkAnnotation(SystemAnnotation annotation) {
		// TODO: check if annotation matches the system
	}

	public Map<ModelElementReference, Set<AnnotationValidityViolation>> getReport() {
		return reportBuilder.buildReport();
	}
}
