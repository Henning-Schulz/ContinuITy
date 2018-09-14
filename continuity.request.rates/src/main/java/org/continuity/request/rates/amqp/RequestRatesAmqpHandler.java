package org.continuity.request.rates.amqp;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.ExternalDataLinkType;
import org.continuity.api.entities.links.ExternalDataLinks;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.application.Application;
import org.continuity.request.rates.config.RabbitMqConfig;
import org.continuity.request.rates.entities.RequestRecord;
import org.continuity.request.rates.entities.WorkloadModelPack;
import org.continuity.request.rates.model.RequestRatesModel;
import org.continuity.request.rates.transform.RequestRatesCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RequestRatesAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestRatesAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private MixedStorage<RequestRatesModel> storage;

	@Value("${spring.application.name}")
	private String applicationName;

	/**
	 * Listener to the RabbitMQ {@link RabbitMqConfig#TASK_CREATE_QUEUE_NAME}. Creates a new request
	 * rates model based on the specified request logs.
	 *
	 * @param task
	 *            The description of the task to be done.
	 */
	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void onMonitoringDataAvailable(TaskDescription task) {
		LOGGER.info("Task {}: Received new task to be processed for tag '{}'", task.getTaskId(), task.getTag());

		TaskReport report;
		ExternalDataLinks link = task.getSource().getExternalDataLinks();

		if (link.getLinkType() != ExternalDataLinkType.CSV) {
			LOGGER.error("Task {}: Cannot process external data of type {}!", task.getTaskId(), link.getLinkType());
			report = TaskReport.error(task.getTaskId(), TaskError.ILLEGAL_TYPE);
		} else {
			String csvRecords = restTemplate.getForObject(WebUtils.addProtocolIfMissing(link.getLink()), String.class);
			List<RequestRecord> records = Arrays.stream(csvRecords.split("\\n")).map(RequestRecord::fromCsvLine).collect(Collectors.toList());

			Application application = restTemplate.getForObject(WebUtils.addProtocolIfMissing(task.getSource().getIdpaLinks().getApplicationLink()), Application.class);
			RequestRatesModel model = new RequestRatesCalculator(application).calculate(records);

			String storageId = storage.put(model, task.getTag(), task.isLongTermUse());

			LOGGER.info("Task {}: Created a new request rates model with id '{}'.", task.getTaskId(), storageId);

			WorkloadModelPack responsePack = new WorkloadModelPack(applicationName, storageId, task.getTag());
			report = TaskReport.successful(task.getTaskId(), responsePack);
		}



		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);
	}

}
