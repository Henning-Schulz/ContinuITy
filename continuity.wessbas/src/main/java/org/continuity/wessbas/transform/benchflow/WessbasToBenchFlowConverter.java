package org.continuity.wessbas.transform.benchflow;

import org.continuity.commons.exceptions.AnnotationNotSupportedException;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.custom.CustomAnnotation;

import m4jdsl.WorkloadModel;

/**
 * @author Henning Schulz
 *
 */
public class WessbasToBenchFlowConverter {

	/**
	 * Converts the passed workload model and annotations to an executable load test. The annotation
	 * models are to be linked.
	 *
	 * @param workloadModel
	 *            The workload model.
	 * @param system
	 *            The system representation.
	 * @param annotation
	 *            The system annotation.
	 * @param extension
	 *            An extension of the annotation. May cause an
	 *            {@link AnnotationNotSupportedException}.
	 * @return An executable load test corresponding to the load represented by the workload model.
	 * @throws AnnotationNotSupportedException
	 *             if the passed {@link CustomAnnotation} cannot be converted to the load test.
	 */
	public Object convertToWorkload(WorkloadModel workloadModel, Application system, ApplicationAnnotation annotation, CustomAnnotation extension) throws AnnotationNotSupportedException {
		// cloud.benchflow.dsl.definition.BenchFlowTest
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Converts the passed workload model and annotations to an executable load test. The annotation
	 * models are to be linked.
	 *
	 * @param workloadModel
	 *            The workload model.
	 * @param system
	 *            The system representation.
	 * @param annotation
	 *            The system annotation.
	 * @return An executable load test corresponding to the load represented by the workload model.
	 */
	public Object convertToWorkload(WorkloadModel workloadModel, Application system, ApplicationAnnotation annotation) {
		// cloud.benchflow.dsl.definition.BenchFlowTest
		return null;
	}

}
