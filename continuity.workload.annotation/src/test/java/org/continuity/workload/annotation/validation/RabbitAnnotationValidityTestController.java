package org.continuity.workload.annotation.validation;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.test.ContinuityModelTestInstance;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("test/validation/rabbit")
public class RabbitAnnotationValidityTestController {

	@RequestMapping(value = "first/system", method = RequestMethod.GET)
	public SystemModel getFirstSystemModel() {
		return ContinuityModelTestInstance.SIMPLE.getSystemModel();
	}

	@RequestMapping(value = "first/annotation", method = RequestMethod.GET)
	public SystemAnnotation getFirstAnnotation() {
		return ContinuityModelTestInstance.SIMPLE.getAnnotation();
	}

	@RequestMapping(value = "second/system", method = RequestMethod.GET)
	public SystemModel getSecondSystemModel() {
		SystemModel model = ContinuityModelTestInstance.SIMPLE.getSystemModel();
		HttpInterface interf = new HttpInterface();
		interf.setDomain("mydomain");
		interf.setId("logout");
		model.addInterface(interf);
		return model;
	}

	@RequestMapping(value = "third/system", method = RequestMethod.GET)
	public SystemModel getThirdSystemModel() {
		SystemModel model = ContinuityModelTestInstance.SIMPLE.getSystemModel();
		model.getInterfaces().get(0).getParameters().clear();
		HttpInterface interf = new HttpInterface();
		interf.setDomain("mydomain");
		interf.setId("logout");
		model.addInterface(interf);
		return model;
	}

}
