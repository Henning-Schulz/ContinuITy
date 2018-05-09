package continuity.workload.dsl.annotation;

import java.io.IOException;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.test.ContinuityModelTestInstance;
import org.continuity.idpa.yaml.IdpaYamlSerializer;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationYamlTest {

	@Test
	public void testAnnotation() throws JsonGenerationException, JsonMappingException, IOException {
		testAnnotationWith(ContinuityModelTestInstance.SIMPLE.getAnnotation());
		testAnnotationWith(ContinuityModelTestInstance.DVDSTORE_PARSED.getAnnotation());
	}

	private void testAnnotationWith(ApplicationAnnotation annotation) throws JsonGenerationException, JsonMappingException, IOException {
		IdpaYamlSerializer<ApplicationAnnotation> serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);
		String yaml = serializer.writeToYamlString(annotation);
		ApplicationAnnotation read = serializer.readFromYamlString(yaml);
		String yaml2 = serializer.writeToYamlString(read);

		Assert.assertEquals("Re-serialized yaml should be equal to original one!", yaml, yaml2);
	}

	@Test
	public void testSystem() throws JsonGenerationException, JsonMappingException, IOException {
		testSystemWith(ContinuityModelTestInstance.SIMPLE.getSystemModel());
		testSystemWith(ContinuityModelTestInstance.DVDSTORE_PARSED.getSystemModel());
	}

	private void testSystemWith(Application systemModel) throws JsonGenerationException, JsonMappingException, IOException {
		IdpaYamlSerializer<Application> serializer = new IdpaYamlSerializer<>(Application.class);
		String yaml = serializer.writeToYamlString(systemModel);
		Application read = serializer.readFromYamlString(yaml);
		String yaml2 = serializer.writeToYamlString(read);

		Assert.assertEquals("Re-serialized yaml should be equal to original one!", yaml, yaml2);
	}

}
