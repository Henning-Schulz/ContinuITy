package org.continuity.jmeter.amqp;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RestApi.IdpaAnnotation;
import org.continuity.api.rest.RestApi.IdpaApplication;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.jmeter.config.RabbitMqConfig;
import org.continuity.jmeter.transform.JMeterAnnotator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class TestPlanCreationAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestPlanCreationAmqpHandler.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void createTestPlan(TaskDescription task) {
		TaskReport report;

		if (task.getSource().getWorkloadLink() == null) {
			report = TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			JMeterTestPlanBundle bundle = createAndGetLoadTest(task.getSource().getWorkloadLink(), task.getTag());

			// TODO: store bundle in storage

			report = TaskReport.successful(task.getTaskId(), new LinkExchangeModel().setJmeterLink("TODO"));
		}

		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);
	}

	/**
	 * Transforms a workload model into a JMeter test and returns it. The workload model is
	 * specified by a link, i.e., {@code TYPE/model/ID}.
	 *
	 * @param workloadModelLink
	 *            The link pointing to the workload model. When called, it is supposed to return an
	 *            object containing a field {@code jmeter-link} which holds a link to the
	 *            corresponding JMeter test plan.
	 * @param tag
	 *            The tag to be used to retrieve the annotation.
	 * @return The transformed JMeter test plan.
	 */
	private JMeterTestPlanBundle createAndGetLoadTest(String workloadModelLink, String tag) {
		LOGGER.debug("Creating a load test from {}.", workloadModelLink);

		LinkExchangeModel workloadLinks = restTemplate.getForObject(WebUtils.addProtocolIfMissing(workloadModelLink), LinkExchangeModel.class);

		if ((workloadLinks == null) || (workloadLinks.getJmeterLink() == null)) {
			throw new IllegalArgumentException("The workload model at " + workloadModelLink + " cannot be transformed into JMeter!");
		}

		JMeterTestPlanBundle testPlanPack = restTemplate.getForObject(WebUtils.addProtocolIfMissing(workloadLinks.getJmeterLink()), JMeterTestPlanBundle.class);

		ListedHashTree annotatedTestPlan = createAnnotatedTestPlan(testPlanPack, tag);

		if (annotatedTestPlan == null) {
			LOGGER.error("Could not annotate the test plan! Ignoring the annotation.");
			annotatedTestPlan = testPlanPack.getTestPlan();
		}

		return new JMeterTestPlanBundle(annotatedTestPlan, testPlanPack.getBehaviors());
	}

	private ListedHashTree createAnnotatedTestPlan(JMeterTestPlanBundle testPlanPack, String tag) {
		ApplicationAnnotation annotation;
		try {
			annotation = restTemplate.getForObject(IdpaAnnotation.Annotation.GET.requestUrl(tag).get(), ApplicationAnnotation.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.error("Received a non-200 response: {} ({}) - {}", e.getStatusCode(), e.getStatusCode().getReasonPhrase(), e.getResponseBodyAsString());
			return null;
		}

		if (annotation == null) {
			LOGGER.error("Annotation with tag {} is null! Aborting.", tag);
			return null;
		}

		Application application = restTemplate.getForObject(IdpaApplication.Application.GET.requestUrl(tag).get(), Application.class);

		if (application == null) {
			LOGGER.error("Application with tag {} is null! Aborting.", tag);
			return null;
		}

		ListedHashTree testPlan = testPlanPack.getTestPlan();
		JMeterAnnotator annotator = new JMeterAnnotator(testPlan, application);
		annotator.addAnnotations(annotation);

		return testPlan;
	}

}
