package org.continuity.system.annotation.controllers;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.test.ContinuityModelTestInstance;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("dummy/dvdstore")
public class DummyAnnotationController {

	@RequestMapping(path = "annotation", method = RequestMethod.GET)
	public ApplicationAnnotation getDvdStoreAnnotation() {
		return ContinuityModelTestInstance.DVDSTORE_PARSED.getAnnotation();
	}

	@RequestMapping(path = "system", method = RequestMethod.GET)
	public Application getDvdStoreSystem() {
		return ContinuityModelTestInstance.DVDSTORE_PARSED.getSystemModel();
	}

}
