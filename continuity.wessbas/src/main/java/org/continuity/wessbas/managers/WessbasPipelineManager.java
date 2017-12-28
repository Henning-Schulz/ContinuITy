package org.continuity.wessbas.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.continuity.commons.wessbas.WessbasModelParser;
import org.continuity.wessbas.entities.MonitoringData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;
import net.sf.markov4jmeter.behaviormodelextractor.BehaviorModelExtractor;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.M4jdslModelGenerator;

/**
 * Manages the WESSBAS pipeline from the input data to the output WESSBAS DSL
 * instance.
 *
 * @author Henning Schulz, Alper Hi
 *
 */
public class WessbasPipelineManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(WessbasPipelineManager.class);

	private final Consumer<WorkloadModel> onModelCreatedCallback;

	private RestTemplate restTemplate;

	private final Path workingDir;

	/**
	 * Constructor.
	 *
	 * @param onModelCreatedCallback
	 *            The function to be called when the model was created.
	 */
	public WessbasPipelineManager(Consumer<WorkloadModel> onModelCreatedCallback, RestTemplate restTemplate) {
		this.onModelCreatedCallback = onModelCreatedCallback;
		this.restTemplate = restTemplate;

		Path tmpDir;
		try {
			tmpDir = Files.createTempDirectory("wessbas");
		} catch (IOException e) {
			LOGGER.error("Could not create a temp directory!");
			e.printStackTrace();
			tmpDir = Paths.get("wessbas");
		}

		workingDir = tmpDir;

		LOGGER.info("Set working directory to {}", workingDir);
	}

	/**
	 * Runs the pipeline and calls the callback when the model was created.
	 *
	 *
	 * @param data
	 *            Input monitoring data to be transformed into a WESSBAS DSL
	 *            instance.
	 */
	public void runPipeline(MonitoringData data) {

		String sessionLog = getSessionLog(data);

		try {
			convertSessionLogIntoWessbasDSLInstance(sessionLog);
		} catch (SecurityException | IOException | GeneratorException e) {
			LOGGER.error("Could not create a WESSBAS workload model!");
			e.printStackTrace();
			return;
		}

		WessbasModelParser parser = new WessbasModelParser();
		Path workloadModelPath = workingDir.resolve("modelgenerator").resolve("workloadmodel.xmi");
		WorkloadModel workloadModel;

		try {
			workloadModel = parser.readWorkloadModel(workloadModelPath.toString());
		} catch (IOException e) {
			LOGGER.error("Could not read the created WESSBAS workload model from path {}!", workloadModelPath);
			e.printStackTrace();
			return;
		}

		onModelCreatedCallback.accept(workloadModel);
	}

	/**
	 * This method converts a session log into a Wessbas DSL instance.
	 *
	 * @param sessionLog
	 * @throws IOException
	 * @throws GeneratorException
	 * @throws SecurityException
	 */
	private void convertSessionLogIntoWessbasDSLInstance(String sessionLog) throws IOException, SecurityException, GeneratorException {
		Path sessionLogsPath = writeSessionLogIntoFile(sessionLog);
		createWorkloadIntensity(100); // TODO: read from somewhere
		createBehaviorModel(sessionLogsPath);
		generateWessbasModel();
	}

	private Path writeSessionLogIntoFile(String sessionLog) throws IOException {
		Path sessionLogsPath = workingDir.resolve("sessions.dat");
		Files.write(sessionLogsPath, Collections.singletonList(sessionLog), StandardOpenOption.CREATE);
		return sessionLogsPath;
	}

	private void createWorkloadIntensity(int numberOfUsers) throws IOException {
		List<String> properties = Arrays.asList("workloadIntensity.type=constant", "wl.type.value=" + numberOfUsers);
		Files.write(workingDir.resolve("workloadIntensity.properties"), properties, StandardOpenOption.CREATE);
	}

	private void createBehaviorModel(Path sessionLogsPath) {
		Path outputDir = workingDir.resolve("behaviormodelextractor");
		outputDir.toFile().mkdir();

		BehaviorModelExtractor behav = new BehaviorModelExtractor();
		behav.createBehaviorModel(sessionLogsPath.toString(), outputDir.toString());
	}

	private void generateWessbasModel() throws FileNotFoundException, SecurityException, GeneratorException {
		M4jdslModelGenerator generator = new M4jdslModelGenerator();
		generator.generate(workingDir.toString());
	}

	/**
	 * Sends request to Session Logs webservice and gets Session Log
	 *
	 * @param data
	 * @return
	 */
	public String getSessionLog(MonitoringData data) {
		String urlString = "http://session-logs?link=" + data.getLink();
		String sessionLog = this.restTemplate.getForObject(urlString, String.class);

		LOGGER.debug("Got session logs: {}", sessionLog);

		return sessionLog;
	}
}