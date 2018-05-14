package org.continuity.idpa.annotation.controllers;

import static org.continuity.api.rest.RestApi.IdpaAnnotation.Annotation.GET_BASE_PATH;
import static org.continuity.api.rest.RestApi.IdpaAnnotation.Annotation.GET_PATH;
import static org.continuity.api.rest.RestApi.IdpaAnnotation.Annotation.ROOT;
import static org.continuity.api.rest.RestApi.IdpaAnnotation.Annotation.UPDATE_PATH;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.entities.AnnotationValidityReport;
import org.continuity.idpa.annotation.storage.AnnotationStorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class AnnotationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationController.class);

	@Value("${spring.application.name}")
	private String applicationName;

	private final AnnotationStorageManager storageManager;

	@Autowired
	public AnnotationController(AnnotationStorageManager storageManager) {
		this.storageManager = storageManager;
	}

	/**
	 * Retrieves the specified annotation if present and if it is not broken.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @returnA {@link ResponseEntity} holding the annotation or specifying the error if one
	 *          occurred. If there is no annotation for the tag, the status 404 (Not Found) will be
	 *          returned. If the annotation is broken, a 423 (Locked) will be returned with a link
	 *          to retrieve the annotation, anyway.
	 */
	@RequestMapping(path = GET_PATH, method = RequestMethod.GET)
	public ResponseEntity<?> getAnnotation(@PathVariable("tag") String tag) {
		ApplicationAnnotation annotation;

		try {
			annotation = storageManager.getAnnotation(tag);
		} catch (IOException e) {
			LOGGER.error("Error during getting annotation with tag {}!", tag);
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (annotation == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else if (storageManager.isBroken(tag)) {
			Map<String, String> message = new HashMap<>();
			message.put("message", "The requested annotation is broken. Get the base via the redirect.");
			message.put("redirect", applicationName + "/ann/" + tag + "/annotation/base");
			return new ResponseEntity<>(message, HttpStatus.LOCKED);
		}

		return new ResponseEntity<>(annotation, HttpStatus.OK);
	}

	/**
	 * Retrieves the base of the specified annotation if present.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @returnA {@link ResponseEntity} holding the annotation or specifying the error if one
	 *          occurred. If there is no base annotation for the tag, the status 404 (Not Found)
	 *          will be returned.
	 */
	@RequestMapping(path = GET_BASE_PATH, method = RequestMethod.GET)
	public ResponseEntity<ApplicationAnnotation> getBaseAnnotation(@PathVariable("tag") String tag) {
		ApplicationAnnotation annotation;

		try {
			annotation = storageManager.getBaseAnnotation(tag);
		} catch (IOException e) {
			LOGGER.error("Error during getting base annotation with tag {}!", tag);
			LOGGER.error("Exception:", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (annotation == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(annotation, HttpStatus.OK);
	}

	/**
	 * Updates the annotation stored with the specified tag. If the annotation is invalid with
	 * respect to the system model, it is rejected.
	 *
	 * @param tag
	 * @param annotation
	 * @return
	 */
	@RequestMapping(path = UPDATE_PATH, method = RequestMethod.POST)
	public ResponseEntity<String> updateAnnotation(@PathVariable("tag") String tag, @RequestBody ApplicationAnnotation annotation) {
		AnnotationValidityReport report = null;

		try {
			report = storageManager.updateAnnotation(tag, annotation);
		} catch (IOException e) {
			LOGGER.error("Error during updating annotation with tag {}!", tag);
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (report.isBreaking()) {
			return new ResponseEntity<>(report.toString(), HttpStatus.CONFLICT);
		} else {
			return new ResponseEntity<>(report.toString(), HttpStatus.CREATED);
		}
	}

}
