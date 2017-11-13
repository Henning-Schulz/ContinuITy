package org.continuity.wessbas.amqp;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.wessbas.config.RabbitMqConfig;
import org.continuity.wessbas.entities.JMeterTestPlanPack;
import org.continuity.wessbas.entities.LoadTestSpecification;
import org.continuity.wessbas.storage.SimpleModelStorage;
import org.continuity.wessbas.transform.jmeter.WessbasToJmeterConverter;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;

/**
 * @author Henning Schulz
 *
 */
@Component
public class JMeterAmqpHandler {

	private static final String UNKNOWN_ID = "UNKNOWN";

	private WessbasToJmeterConverter jmeterConverter = new WessbasToJmeterConverter();

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@RabbitListener(queues = RabbitMqConfig.LOAD_TEST_NEEDED_QUEUE_NAME)
	public void createTestPlan(LoadTestSpecification specification) {
		String id = extractIdFromLink(specification.getWorkloadModelLink());

		if (id == UNKNOWN_ID) {
			// TODO: log error message
		} else {
			WorkloadModel workloadModel = SimpleModelStorage.instance().get(id).getWorkloadModel();
			SystemAnnotation annotation = restTemplate.getForObject(specification.getAnnotationLink(), SystemAnnotation.class);
			JMeterTestPlanPack testPlanPack = jmeterConverter.convertToLoadTest(workloadModel, annotation);

			amqpTemplate.convertAndSend(RabbitMqConfig.LOAD_TEST_CREATED_EXCHANGE_NAME, "wessbas.jmeter", testPlanPack);
		}
	}

	private String extractIdFromLink(String workloadModelLink) {
		int start = workloadModelLink.lastIndexOf("/") + 1;
		int end = workloadModelLink.length();

		if (start >= end) {
			return UNKNOWN_ID;
		} else {
			return workloadModelLink.substring(start, end);
		}
	}

}
