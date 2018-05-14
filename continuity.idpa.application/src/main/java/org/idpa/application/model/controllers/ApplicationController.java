package org.idpa.application.model.controllers;

import static org.continuity.api.rest.RestApi.IdpaApplication.Application.ROOT;
import static org.continuity.api.rest.RestApi.IdpaApplication.Application.Paths.GET_DELTA;
import static org.continuity.api.rest.RestApi.IdpaApplication.Application.Paths.GET;
import static org.continuity.api.rest.RestApi.IdpaApplication.Application.Paths.UPDATE;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.EnumSet;

import org.continuity.commons.format.CommonFormats;
import org.continuity.idpa.application.Application;
import org.idpa.application.model.config.RabbitMqConfig;
import org.idpa.application.model.entities.SystemChangeReport;
import org.idpa.application.model.entities.SystemChangeType;
import org.idpa.application.model.entities.SystemModelLink;
import org.idpa.application.model.repository.SystemModelRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Offers a REST API for controlling the stored system models.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class ApplicationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationController.class);

	private static final DateFormat DATE_FORMAT = CommonFormats.DATE_FORMAT;

	private final AmqpTemplate amqpTemplate;

	@Value("${spring.application.name}")
	private String applicationName;

	private final SystemModelRepositoryManager manager;

	@Autowired
	public ApplicationController(SystemModelRepositoryManager manager, AmqpTemplate amqpTemplate) {
		this.manager = manager;
		this.amqpTemplate = amqpTemplate;
	}

	/**
	 * Retrieves the current application model.
	 *
	 * @param tag
	 *            The tag of the application.
	 * @return The current application model.
	 */
	@RequestMapping(path = GET, method = RequestMethod.GET)
	public ResponseEntity<Application> getApplication(@PathVariable String tag) {
		try {
			return ResponseEntity.ok(manager.read(tag));
		} catch (IOException e) {
			LOGGER.error("En exception occured during reading!", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Gets a report holding the difference between the current application model and a date in the
	 * past.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param since
	 *            The date to compare against in the format {@link CommonFormats#DATE_FORMAT}.
	 * @return The delta report.
	 */
	@RequestMapping(path = GET_DELTA, method = RequestMethod.GET)
	public ResponseEntity<SystemChangeReport> getDeltaSince(@PathVariable String tag, @RequestParam("since") String since) {
		Date date;
		try {
			date = DATE_FORMAT.parse(since);
		} catch (ParseException e) {
			LOGGER.error("Could not parse since date {}.", since);
			LOGGER.error("Exception:", e);
			return ResponseEntity.badRequest().build();
		}

		return ResponseEntity.ok(manager.getDeltaSince(tag, date));
	}

	/**
	 * Stores a new application model if it differs from existing ones, possibly ignoring specific
	 * change types.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param system
	 *            The system model to be stored.
	 * @param ignoreInterfaceChanged
	 *            Ignore {@link SystemChangeType#INTERFACE_CHANGED}.
	 * @param ignoreInterfaceRemoved
	 *            Ignore {@link SystemChangeType#INTERFACE_REMOVED}.
	 * @param ignoreInterfaceAdded
	 *            Ignore {@link SystemChangeType#INTERFACE_ADDED}.
	 * @param ignoreParameterRemoved
	 *            Ignore {@link SystemChangeType#PARAMETER_REMOVED}.
	 * @param ignoreParameterAdded
	 *            Ignore {@link SystemChangeType#PARAMETER_ADDED}.
	 * @return A report holding the differences between the passed model and the next older one.
	 */
	@RequestMapping(path = UPDATE, method = RequestMethod.POST)
	public ResponseEntity<String> updateApplication(@PathVariable String tag, @RequestBody Application system,
			@RequestParam(name = "ignore-interface-changed", defaultValue = "false") boolean ignoreInterfaceChanged,
			@RequestParam(name = "ignore-interface-removed", defaultValue = "false") boolean ignoreInterfaceRemoved,
			@RequestParam(name = "ignore-interface-added", defaultValue = "false") boolean ignoreInterfaceAdded,
			@RequestParam(name = "ignore-parameter-changed", defaultValue = "false") boolean ignoreParameterChanged,
			@RequestParam(name = "ignore-parameter-removed", defaultValue = "false") boolean ignoreParameterRemoved,
			@RequestParam(name = "ignore-parameter-added", defaultValue = "false") boolean ignoreParameterAdded) {

		EnumSet<SystemChangeType> ignoredChangeTypes = changeTypesFromBooleans(ignoreInterfaceChanged, ignoreInterfaceRemoved, ignoreInterfaceAdded, ignoreParameterChanged, ignoreParameterRemoved,
				ignoreParameterAdded);

		SystemChangeReport report = manager.saveOrUpdate(tag, system, ignoredChangeTypes);

		if (report.changed()) {
			try {
				amqpTemplate.convertAndSend(RabbitMqConfig.SYSTEM_MODEL_CHANGED_EXCHANGE_NAME, tag, new SystemModelLink(applicationName, tag, report.getBeforeChange()));
			} catch (AmqpException e) {
				LOGGER.error("Could not send the system model with tag {} to the {} exchange!", tag, RabbitMqConfig.SYSTEM_MODEL_CHANGED_EXCHANGE_NAME);
				LOGGER.error("Exception:", e);
			}
		}

		return ResponseEntity.ok().body(report.toString());
	}

	/**
	 * Stores a new application model if it differs from existing ones. None of the change types
	 * will be ignored.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param system
	 *            The application model to be stored.
	 * @return A report holding the differences between the passed model and the next older one.
	 */
	public ResponseEntity<String> updateApplication(@PathVariable String tag, @RequestBody Application system) {
		return updateApplication(tag, system, false, false, false, false, false, false);
	}

	private EnumSet<SystemChangeType> changeTypesFromBooleans(boolean ignoreInterfaceChanged, boolean ignoreInterfaceRemoved, boolean ignoreInterfaceAdded, boolean ignoreParameterChanged,
			boolean ignoreParameterRemoved,
			boolean ignoreParameterAdded) {
		EnumSet<SystemChangeType> set = EnumSet.noneOf(SystemChangeType.class);

		if (ignoreInterfaceChanged) {
			set.add(SystemChangeType.INTERFACE_CHANGED);
		}

		if (ignoreInterfaceRemoved) {
			set.add(SystemChangeType.INTERFACE_REMOVED);
		}

		if (ignoreInterfaceAdded) {
			set.add(SystemChangeType.INTERFACE_ADDED);
		}

		if (ignoreParameterChanged) {
			set.add(SystemChangeType.PARAMETER_CHANGED);
		}

		if (ignoreParameterRemoved) {
			set.add(SystemChangeType.PARAMETER_REMOVED);
		}

		if (ignoreParameterAdded) {
			set.add(SystemChangeType.PARAMETER_ADDED);
		}

		return set;
	}

}
