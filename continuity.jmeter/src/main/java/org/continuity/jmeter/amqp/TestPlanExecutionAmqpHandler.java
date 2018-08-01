package org.continuity.jmeter.amqp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.testelement.TestStateListener;
import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.api.entities.config.PropertySpecification;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.commons.jmeter.JMeterPropertiesCorrector;
import org.continuity.commons.jmeter.TestPlanWriter;
import org.continuity.commons.utils.JMeterUtils;
import org.continuity.jmeter.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Henning Schulz
 *
 */
@Component
public class TestPlanExecutionAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestPlanExecutionAmqpHandler.class);

	@Autowired
	private TestPlanWriter testPlanWriter;

	@Autowired
	private AmqpTemplate amqpTemplate;

	private JMeterPropertiesCorrector jmeterPropertiesCorrector = new JMeterPropertiesCorrector();

	private final ConcurrentHashMap<Integer, Boolean> runningTests = new ConcurrentHashMap<>();

	private final AtomicInteger testCounter = new AtomicInteger(0);

	/**
	 * Listens to the {@link RabbitMqConfig#TASK_EXECUTE_QUEUE_NAME} queue and executes the JMeter
	 * test plan.
	 *
	 * @param task
	 *            Task to be processed.
	 */
	@RabbitListener(queues = RabbitMqConfig.TASK_EXECUTE_QUEUE_NAME)
	public void executeTestPlan(TaskDescription task) {
		JMeterTestPlanBundle testPlanBundle = null; // TODO: get from storage

		PropertySpecification properties = task.getProperties();

		if ((properties.getNumUsers() != null) && (properties.getDuration() != null) && (properties.getRampup() != null)) {
			jmeterPropertiesCorrector.setRuntimeProperties(testPlanBundle.getTestPlan(), properties.getNumUsers(), properties.getDuration(), properties.getRampup());
			LOGGER.info("Set JMeter properties num-users = {}, duration = {}, rampup = {}.", properties.getNumUsers(), properties.getDuration(), properties.getRampup());
		} else {
			LOGGER.warn("Could not set JMeter properties, as some of them are null: num-users = {}, duration = {}, rampup = {}.", properties.getNumUsers(), properties.getDuration(),
					properties.getRampup());
		}

		Path tmpPath;

		try {
			tmpPath = Files.createTempDirectory("jmeter-test-plan");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Path resultsPath = tmpPath.resolve("results.csv");

		jmeterPropertiesCorrector.correctPaths(testPlanBundle.getTestPlan(), tmpPath);
		jmeterPropertiesCorrector.configureResultFile(testPlanBundle.getTestPlan(), resultsPath);
		jmeterPropertiesCorrector.prepareForHeadlessExecution(testPlanBundle.getTestPlan());
		Path testPlanPath = testPlanWriter.write(testPlanBundle.getTestPlan(), testPlanBundle.getBehaviors(), tmpPath);
		LOGGER.info("Created a test plan at {}.", testPlanPath);

		final int testId = testCounter.getAndIncrement();
		runningTests.put(testId, true);

		JMeter jmeter = new JMeter();
		String[] arguments = { "-n", "-t", testPlanPath.toAbsolutePath().toString() };

		StandardJMeterEngine.register(new TestStateListener() {

			@Override
			public void testStarted(String arg0) {
				// do nothing
			}

			@Override
			public void testStarted() {
				// do nothing
			}

			@Override
			public void testEnded(String arg0) {
				testEnded();
			}

			@Override
			public void testEnded() {
				try {
					String appendix = "";

					if ((runningTests.get(testId) == null) || !runningTests.get(testId)) {
						LOGGER.warn("The test {} has been aborted with force!", testId);
						appendix = "\nWARNING: The test has been aborted with force!\n";
					}

					runningTests.put(testId, false);

					// TODO: Report storage and send to finished exchange
					amqpTemplate.convertAndSend(AmqpApi.LoadTest.REPORT_AVAILABLE.name(), AmqpApi.LoadTest.REPORT_AVAILABLE.formatRoutingKey().of("jmeter"),
							FileUtils.readFileToString(resultsPath.toFile(), Charset.defaultCharset()) + appendix);
					LOGGER.info("JMeter test finished. Results are stored to {}.", resultsPath);
				} catch (AmqpException | IOException e) {
					LOGGER.error("Error when pushing the test results to the queue!", e);
				}
			}
		});
		jmeter.start(arguments);

		LOGGER.info("Test {} started.", testId);
		new StopEngineAfterTime(testId, JMeterUtils.getDuration(testPlanBundle.getTestPlan())).start();
	}

	private class StopEngineAfterTime extends Thread {

		private final int testId;

		private final long endTime;

		private StopEngineAfterTime(int testId, long duration) {
			this.testId = testId;
			this.endTime = System.currentTimeMillis() + Math.min((duration * 1000) + 1800000, Math.max(300000, 2000 * duration));
		}

		@Override
		public void run() {
			while (System.currentTimeMillis() < endTime) {
				try {
					sleep(60000);
				} catch (InterruptedException e) {
					LOGGER.warn("StopEngineAfterTime was interrupted!", e);
				}
			}

			if (runningTests.get(testId) == null) {
				LOGGER.error("There was not entry for test {}!", testId);
				return;
			}

			if (runningTests.get(testId)) {
				runningTests.put(testId, false);
				StandardJMeterEngine.stopEngineNow();
				LOGGER.warn("Test {} was still running. Aborted it.", testId);
			}

			runningTests.remove(testId);
		}

	}

}
