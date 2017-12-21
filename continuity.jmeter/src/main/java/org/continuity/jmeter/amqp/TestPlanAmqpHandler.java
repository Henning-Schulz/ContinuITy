package org.continuity.jmeter.amqp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.jmeter.engine.JMeterEngineException;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.continuity.commons.jmeter.JMeterPropertiesCorrector;
import org.continuity.commons.jmeter.TestPlanWriter;
import org.continuity.jmeter.config.RabbitMqConfig;
import org.continuity.jmeter.controllers.TestPlanController;
import org.continuity.jmeter.entities.LoadTestSpecification;
import org.continuity.jmeter.entities.TestPlanBundle;
import org.lpe.common.jmeter.config.JMeterWorkloadConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Henning Schulz
 *
 */
@Component
public class TestPlanAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestPlanAmqpHandler.class);

	@Autowired
	private TestPlanController testPlanController;

	@Autowired
	private TestPlanWriter testPlanWriter;

	private JMeterPropertiesCorrector behaviorPathsCorrector = new JMeterPropertiesCorrector();

	/**
	 * Listens to the {@link RabbitMqConfig#EXECUTE_LOAD_TEST_QUEUE_NAME} queue, annotates the test
	 * plan and executes the test.
	 *
	 * @param specification
	 *            The specification of the test plan.
	 */
	@RabbitListener(queues = RabbitMqConfig.EXECUTE_LOAD_TEST_QUEUE_NAME)
	public void executeTestPlan(LoadTestSpecification specification) {
		LOGGER.debug("Received test plan specification.");

		TestPlanBundle testPlanPack = testPlanController.createAndGetLoadTest(specification.getWorkloadModelType(), specification.getWorkloadModelId(), specification.getTag());

		LOGGER.debug("Got an annotated test plan pack.");

		Path tmpPath;

		try {
			tmpPath = Files.createTempDirectory("jmeter-test-plan");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Path resultsPath = tmpPath.resolve("results.csv");

		behaviorPathsCorrector.correctPaths(testPlanPack.getTestPlan(), tmpPath);
		behaviorPathsCorrector.configureResultFile(testPlanPack.getTestPlan(), resultsPath);
		behaviorPathsCorrector.prepareForHeadlessExecution(testPlanPack.getTestPlan());
		Path testPlanPath = testPlanWriter.write(testPlanPack.getTestPlan(), testPlanPack.getBehaviors(), tmpPath);
		LOGGER.info("Created a test plan at {}.", testPlanPath);

		// Theoretically, this should work directly from code, but the results only contain errors.
		StandardJMeterEngine jmeterEngine = new StandardJMeterEngine();
		jmeterEngine.configure(testPlanPack.getTestPlan());
		try {
			jmeterEngine.runTest();
		} catch (JMeterEngineException e) {
			LOGGER.error("Error during running the JMeter test!");
			e.printStackTrace();
		}

		// try {
		// // JMeterWrapper.getInstance().startLoadTest(getJMeterConfig(testPlanPath));
		// startLoadTest(testPlanPath);
		// } catch (IOException e) {
		// LOGGER.error("Error during running the JMeter test!");
		// e.printStackTrace();
		// }

		// try {
		// JMeterWrapper.getInstance().waitForLoadTestFinish();
		// } catch (InterruptedException e) {
		// LOGGER.error("Interrupted during waiting for the end of the load test!");
		// e.printStackTrace();
		// }

		LOGGER.info("JMeter test finished. Results are stored to {}.", resultsPath);
	}

	private void startLoadTest(Path loadTestPath) throws IOException {

		List<String> cmd = new ArrayList<String>();

		cmd.add("java");
		cmd.add("-jar");
		cmd.add("bin/ApacheJMeter.jar");
		cmd.add("-n"); // JMeter in non-gui mode
		cmd.add("-t"); // load script fiel path
		cmd.add("\"" + loadTestPath.toString() + "\"");

		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(new File("C:\\apache-jmeter-3.0\\apache-jmeter-3.0"));
		// output needs to be redirected
		pb.redirectOutput(new File("./jmeter-output.txt"));
		// the error stream must be piped, otherwise noone takes the messages and JMeter waits to
		// infinity
		// till someone receives the messages!
		pb.redirectErrorStream(true);
		Process jmeterProcess = pb.start();
	}

	private JMeterWorkloadConfig getJMeterConfig(Path testPlanPath) {
		JMeterWorkloadConfig config = new JMeterWorkloadConfig();

		// TODO: fill
		config.setPathToJMeterBinFolder("C:\\apache-jmeter-3.0\\apache-jmeter-3.0\\bin");
		config.setPathToScript(testPlanPath.toString());

		return config;
	}

}
