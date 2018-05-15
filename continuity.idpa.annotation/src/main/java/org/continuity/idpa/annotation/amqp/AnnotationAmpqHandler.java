package org.continuity.idpa.annotation.amqp;

import java.io.IOException;

import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.config.RabbitMqConfig;
import org.continuity.idpa.annotation.entities.AnnotationValidityReport;
import org.continuity.idpa.annotation.entities.SystemAnnotationLink;
import org.continuity.idpa.annotation.storage.AnnotationStorageManager;
import org.continuity.idpa.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Henning Schulz
 *
 */
@Component
public class AnnotationAmpqHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationAmpqHandler.class);

	private final AnnotationStorageManager storageManager;

	private final RestTemplate restTemplate;

	private final AmqpTemplate amqpTemplate;

	@Autowired
	public AnnotationAmpqHandler(AnnotationStorageManager storageManager, RestTemplate restTemplate, AmqpTemplate amqpTemplate) {
		this.storageManager = storageManager;
		this.restTemplate = restTemplate;
		this.amqpTemplate = amqpTemplate;
	}

	@RabbitListener(queues = RabbitMqConfig.MODEL_CREATED_QUEUE_NAME)
	public void onAnnotationModelCreated(SystemAnnotationLink link) {
		LOGGER.info("Received system annotation link: {}", link);

		ResponseEntity<ApplicationAnnotation> annResponse;
		try {
			annResponse = restTemplate.getForEntity(WebUtils.addProtocolIfMissing(link.getAnnotationLink()), ApplicationAnnotation.class);
		} catch (RestClientResponseException e) {
			LOGGER.error("Received error response! Ignoring the new annotation.", e);
			return;
		}

		try {
			storageManager.saveAnnotationIfNotPresent(link.getTag(), annResponse.getBody());
		} catch (IOException e) {
			LOGGER.error("Error during storing the new annotation with tag {}!", link.getTag());
			LOGGER.error("Exception: ", e);
			return;
		}
	}

	@RabbitListener(queues = RabbitMqConfig.SYSTEM_MODEL_CHANGED_QUEUE_NAME)
	public void onSystemModelCreated(SystemAnnotationLink link) {
		LOGGER.info("Received system annotation link: {}", link);

		ResponseEntity<Application> systemResponse;
		try {
			systemResponse = restTemplate.getForEntity(WebUtils.addProtocolIfMissing(link.getApplicationLink()), Application.class);
		} catch (RestClientResponseException e) {
			LOGGER.error("Received error response! Ignoring the new system model.", e);
			return;
		}

		ResponseEntity<AnnotationValidityReport> reportResponse;
		AnnotationValidityReport report;
		try {
			reportResponse = restTemplate.getForEntity(WebUtils.addProtocolIfMissing(link.getDeltaLink()), AnnotationValidityReport.class);
			report = reportResponse.getBody();
		} catch (RestClientResponseException e) {
			LOGGER.error("Received error response! Assuming there is no difference in the system models.", e);
			report = AnnotationValidityReport.empty();
		}

		try {
			report = storageManager.updateSystemModel(link.getTag(), systemResponse.getBody(), report);
		} catch (IOException e) {
			LOGGER.error("Error during storing the new system model with tag {}!", link.getTag());
			LOGGER.error("Exception: ", e);
			return;
		}

		if (!report.isOk()) {
			amqpTemplate.convertAndSend(RabbitMqConfig.CLIENT_MESSAGE_EXCHANGE_NAME, "report", report);
		}
	}

}
