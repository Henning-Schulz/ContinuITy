package org.continuity.commons.idpa;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.visitor.IdpaVisitor;

/**
 * Utility calss for extracting initial annotations from system models.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationExtractor {

	/**
	 * Extracts the annotations from the specified system model.
	 *
	 * @param system
	 *            The system model.
	 * @return The extracted annotations.
	 */
	public ApplicationAnnotation extractAnnotation(Application system) {
		SystemToAnnotationTransformer transformer = new SystemToAnnotationTransformer();
		IdpaVisitor visitor = new IdpaVisitor(transformer::onModelElementVisited);
		visitor.visit(system);
		return transformer.getExtractedAnnotation();
	}

}
