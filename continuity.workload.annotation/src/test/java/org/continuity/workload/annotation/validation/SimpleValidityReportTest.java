package org.continuity.workload.annotation.validation;

import org.continuity.workload.annotation.entities.AnnotationViolation;
import org.continuity.workload.annotation.entities.AnnotationViolationType;
import org.continuity.workload.annotation.entities.ModelElementReference;
import org.continuity.workload.annotation.validation.AnnotationValidationReportBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Henning Schulz
 *
 */
public class SimpleValidityReportTest {

	@Test
	public void test() {
		AnnotationValidationReportBuilder builder = new AnnotationValidationReportBuilder();
		Assert.assertTrue(builder.buildReport().isOk());
		Assert.assertFalse(builder.buildReport().isBreaking());

		builder.addViolation(new AnnotationViolation(AnnotationViolationType.PARAMETER_ADDED, new ModelElementReference("foo", "bar")));
		Assert.assertFalse(builder.buildReport().isOk());
		Assert.assertFalse(builder.buildReport().isBreaking());

		builder.addViolation(new AnnotationViolation(AnnotationViolationType.INTERFACE_CHANGED, new ModelElementReference("123", "42")));
		Assert.assertFalse(builder.buildReport().isOk());
		Assert.assertFalse(builder.buildReport().isBreaking());

		builder.addViolation(new AnnotationViolation(AnnotationViolationType.ILLEAL_INTERFACE_REFERENCE, new ModelElementReference("xyz", "abc")));
		Assert.assertFalse(builder.buildReport().isOk());
		Assert.assertTrue(builder.buildReport().isBreaking());
	}

}
