package org.continuity.idpa.annotation.controllers;

import static org.continuity.api.rest.RestApi.IdpaAnnotation.Dummy.GET_ANNOTATION_PATH;
import static org.continuity.api.rest.RestApi.IdpaAnnotation.Dummy.GET_APPLICATION_PATH;
import static org.continuity.api.rest.RestApi.IdpaAnnotation.Dummy.ROOT;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.test.IdpaTestInstance;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class DummyAnnotationController {

	@RequestMapping(path = GET_ANNOTATION_PATH, method = RequestMethod.GET)
	public ApplicationAnnotation getDvdStoreAnnotation() {
		return IdpaTestInstance.DVDSTORE_PARSED.getAnnotation();
	}

	@RequestMapping(path = GET_APPLICATION_PATH, method = RequestMethod.GET)
	public Application getDvdStoreSystem() {
		return IdpaTestInstance.DVDSTORE_PARSED.getApplication();
	}

}
