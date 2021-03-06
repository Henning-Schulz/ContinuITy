package org.continuity.wessbas.managers;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingDouble;
import static java.util.stream.Collectors.toList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.Range;
import org.continuity.api.entities.artifact.ForecastIntensityRecord;
import org.continuity.api.entities.artifact.SessionsBundlePack;
import org.continuity.api.entities.artifact.SimplifiedSession;
import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.markovbehavior.NormalDistribution;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovTransition;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.order.TailoringApproach;
import org.continuity.commons.utils.IntensityCalculationUtils;
import org.continuity.commons.utils.SimplifiedSessionLogsDeserializer;
import org.continuity.commons.utils.TailoringUtils;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.wessbas.entities.BehaviorModelPack;
import org.continuity.wessbas.entities.WessbasBundle;
import org.continuity.wessbas.entities.WessbasDslInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.M4jdslModelGenerator;
import net.sf.markov4jmeter.testplangenerator.util.CSVHandler;

/**
 * Manages the WESSBAS pipeline from the input data to the output WESSBAS DSL instance.
 *
 * @author Henning Schulz, Alper Hi
 *
 */
public class WessbasPipelineManager {

	public static final String PREFIX_INTENSITY_SERIES = "wl.series.values.";

	public static final String KEY_INTENSITY_RESOLUTION = "wl.series.resolution";

	public static final String PREFIX_BEHAVIOR_MODEL = "gen_behavior_model";

	private static final Logger LOGGER = LoggerFactory.getLogger(WessbasPipelineManager.class);

	private RestTemplate restTemplate;

	private final Path workingDir;

	/**
	 * Constructor.
	 */
	public WessbasPipelineManager(RestTemplate restTemplate) {
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
	 * Constructor.
	 */
	public WessbasPipelineManager(RestTemplate restTemplate, Path workingDir) {
		this.restTemplate = restTemplate;
		this.workingDir = workingDir;

		LOGGER.info("Set working directory to {}", workingDir);
	}

	/**
	 * Runs the whole pipeline by creating a behavior model and transforming it into a workload
	 * model.
	 *
	 *
	 * @param task
	 *            Input monitoring data to be transformed into a WESSBAS DSL instance.
	 * @param interval
	 *            The interval for calculating the intensity.
	 *
	 * @see #createBehaviorModelFromSessions(String, VersionOrTimestamp, long)
	 * @see #transformBehaviorModelToWorkloadModelIncludingTailoring(BehaviorModelPack,
	 *      TaskDescription)
	 *
	 * @return The generated workload model.
	 */
	public WessbasBundle runPipeline(TaskDescription task, long interval) {
		String sessionLogsLink = task.getSource().getSessionLinks().getExtendedLink();
		if ("dummy".equals(sessionLogsLink)) {
			return new WessbasBundle(task.getVersion(), WessbasDslInstance.DVDSTORE_PARSED.get());
		}

		WessbasBundle workloadModel;
		try {
			BehaviorModelPack behaviorModelPack = createBehaviorModelFromSessions(task, interval);
			workloadModel = transformBehaviorModelToWorkloadModelIncludingTailoring(behaviorModelPack, task);
		} catch (Exception e) {
			LOGGER.error("Could not create a WESSBAS workload model!", e);
			return null;
		}

		return workloadModel;
	}

	/**
	 * Creates a behavior model from (extended) session logs.
	 *
	 * @param task
	 *            The task, which is assumed to hold extended session logs.
	 * @param interval
	 *            The interval for calculating the intensity.
	 * @return The behavior model in a {@link BehaviorModelPack}.
	 * @throws IOException
	 */
	public BehaviorModelPack createBehaviorModelFromSessions(TaskDescription task, long interval) throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));

		String sessionLogs;
		try {
			sessionLogs = restTemplate.exchange(WebUtils.addProtocolIfMissing(task.getSource().getSessionLinks().getExtendedLink()), HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
		} catch (RestClientException e) {
			LOGGER.error("Error when retrieving the session logs!", e);
			return null;
		}

		createWorkloadIntensity(sessionLogs, interval);

		BehaviorMixManager behaviorManager = new BehaviorMixManager(task.getVersion(), workingDir);
		SessionsBundlePack sessionsBundles = behaviorManager.runPipeline(sessionLogs);

		return new BehaviorModelPack(sessionsBundles, workingDir);
	}

	/**
	 * Creates a behavior model from an externally created {@link MarkovBehaviorModel}.
	 *
	 * @param task
	 *            The task, which is assumed to hold the {@link MarkovBehaviorModel}.
	 * @return The behavior model in a {@link BehaviorModelPack}.
	 * @throws IOException
	 */
	public BehaviorModelPack createBehaviorModelFromMarkovChains(TaskDescription task) throws IOException {
		Path dir = workingDir.resolve("behaviormodelextractor");
		dir.toFile().mkdir();

		MarkovBehaviorModel markovModel = restTemplate.getForObject(WebUtils.addProtocolIfMissing(task.getSource().getBehaviorModelLinks().getLink()), MarkovBehaviorModel.class);

		for (RelativeMarkovChain chain : markovModel.getMarkovChains()) {
			for (String state : chain.getRequestStates()) {
				double maxProb = chain.getTransitions().entrySet().stream().filter(e -> !state.equals(e.getKey())).map(Entry::getValue).map(map -> map.get(state)).filter(Objects::nonNull)
						.mapToDouble(RelativeMarkovTransition::getProbability).max().orElse(0);

				if (maxProb < RelativeMarkovTransition.PRECISION) {
					LOGGER.info("Markov chain {}: removing state {} because it has no incoming transitions larger than {}.", chain.getId(), state, RelativeMarkovTransition.PRECISION);
					chain.removeState(state, NormalDistribution.ZERO);
				}
			}
		}

		markovModel.synchronizeMarkovChains();

		List<ForecastIntensityRecord> intensities = loadIntensities(task.getSource().getIntensity());
		adjustIntensitiesToGroups(intensities, markovModel);

		createWorkloadIntensity(intensities);
		writeDummySessionsDat();
		writeUsecases(markovModel, dir);
		updateBehaviorMix(markovModel, intensities);
		writeBehaviorMix(markovModel, dir);
		writeBehaviorModels(markovModel, dir);

		return new BehaviorModelPack(null, workingDir);
	}

	private void adjustIntensitiesToGroups(List<ForecastIntensityRecord> intensities, MarkovBehaviorModel markovModel) {
		if ((intensities == null) || intensities.isEmpty() || (markovModel == null) || (markovModel.getMarkovChains() == null)) {
			return;
		}

		if (intensities.stream().map(ForecastIntensityRecord::getContent).filter(m -> m.containsKey(ForecastIntensityRecord.KEY_TOTAL)).count() > 0) {
			return;
		}

		Set<String> groups = markovModel.getMarkovChains().stream().map(RelativeMarkovChain::getId).collect(Collectors.toSet());
		Set<String> allRemoved = new HashSet<>();

		for (ForecastIntensityRecord rec : intensities) {
			Set<String> groupsToRemove = new HashSet<>(rec.getGroups());
			groupsToRemove.removeAll(groups);
			allRemoved.addAll(groupsToRemove);

			if (!groupsToRemove.isEmpty()) {
				double totalIntensity = getTotalIntensity(rec);
				groupsToRemove.forEach(rec.getContent()::remove);
				double newTotal = getTotalIntensity(rec);

				for (Entry<String, Double> entry : rec.getContent().entrySet()) {
					if (!ForecastIntensityRecord.KEY_TIMESTAMP.equals(entry.getKey())) {
						rec.getContent().put(entry.getKey(), (entry.getValue() * totalIntensity) / newTotal);
					}
				}
			}
		}

		if (allRemoved.isEmpty()) {
			LOGGER.info("The intensities fit to the groups contained in the behavior model.");
		} else {
			LOGGER.info("The following groups, which are contained in the intensities, are not part of the workload model: {}."
					+ "Removed them from the intensities and adjusted the remaining to sum to the same total intensity as before.", allRemoved);
		}
	}

	private double getTotalIntensity(ForecastIntensityRecord record) {
		return record.getContent().entrySet().stream().filter(e -> !ForecastIntensityRecord.KEY_TIMESTAMP.equals(e.getKey())).mapToDouble(Entry::getValue).sum();
	}

	/**
	 * Transforms a behavior model into a workload model and also applies tailoring if requested.
	 *
	 * @param behaviorModelPack
	 *            The behavior model as {@link BehaviorModelPack}.
	 * @param task
	 *            The task describing how to generate the workload model, especially regarding
	 *            tailoring.
	 * @return The workload model.
	 * @throws IOException
	 * @throws SecurityException
	 * @throws GeneratorException
	 */
	public WessbasBundle transformBehaviorModelToWorkloadModelIncludingTailoring(BehaviorModelPack behaviorModelPack, TaskDescription task) throws IOException, SecurityException, GeneratorException {
		boolean applyModularization = (task.getOptions() != null) && (task.getOptions().getServiceTailoring() == TailoringApproach.MODEL_BASED)
				&& TailoringUtils.doTailoring(task.getEffectiveServices());

		if (applyModularization) {
			WorkloadModularizationManager modularizationManager = new WorkloadModularizationManager(restTemplate, task.getAppId(), task.getVersion());
			modularizationManager.runPipeline(task.getVersion(), task.getSource(), behaviorModelPack, task.getEffectiveServices());
		}

		Properties intensityProps = new Properties();
		intensityProps.load(Files.newInputStream(workingDir.resolve("workloadIntensity.properties")));

		Properties behaviorProperties = new Properties();
		behaviorProperties.load(Files.newInputStream(workingDir.resolve("behaviormodelextractor").resolve("behaviormix.txt")));

		Map<String, String> intensities = new HashMap<>();
		Enumeration<?> propsEnum = intensityProps.propertyNames();

		while (propsEnum.hasMoreElements()) {
			String prop = propsEnum.nextElement().toString();

			if (prop.startsWith(PREFIX_INTENSITY_SERIES)) {
				intensities.put(prop.substring(PREFIX_INTENSITY_SERIES.length()), intensityProps.getProperty(prop));
			}
		}

		Integer resolution = Optional.ofNullable(intensityProps.getProperty(KEY_INTENSITY_RESOLUTION)).map(Integer::parseInt).orElse(null);

		return new WessbasBundle(task.getVersion(), generateWessbasModel(intensityProps, behaviorProperties), intensities, resolution);
	}

	private List<ForecastIntensityRecord> loadIntensities(String link) {
		if (link == null) {
			return null;
		}

		ForecastIntensityRecord[] records = restTemplate.getForObject(WebUtils.addProtocolIfMissing(link), ForecastIntensityRecord[].class);
		return Arrays.asList(records);
	}

	private Properties createWorkloadIntensity(String sessionLogs, long interval) throws IOException {
		return createWorkloadIntensity(calculateIntensity(sessionLogs, interval));
	}

	private Properties createWorkloadIntensity(List<ForecastIntensityRecord> intensities) throws IOException {
		if ((intensities == null) || (intensities.size() == 0)) {
			LOGGER.warn("Did not get any intensities. Therefore, using the default value 1.");
			return createWorkloadIntensity(1);
		}

		Map<String, List<Double>> intensitiesPerGroup = intensities.stream().flatMap(r -> r.getContent().entrySet().stream().filter(e -> !ForecastIntensityRecord.KEY_TIMESTAMP.equals(e.getKey())))
				.collect(groupingBy(Entry::getKey, mapping(Entry::getValue, toList())));

		int totalMaxIntensity = intensities.stream()
				.mapToDouble(r -> r.getContent().entrySet().stream().filter(e -> !ForecastIntensityRecord.KEY_TIMESTAMP.equals(e.getKey())).mapToDouble(Entry::getValue).sum()).mapToLong(Math::round)
				.mapToInt(Math::toIntExact).max().orElse(1);

		if (intensities.size() == 1) {
			return createWorkloadIntensity(totalMaxIntensity);
		} else {
			int resolution = IntStream.range(0, intensities.size() - 1).mapToLong(i -> intensities.get(i + 1).getTimestamp() - intensities.get(i).getTimestamp()).mapToInt(Math::toIntExact).min()
					.getAsInt();
			return createWorkloadIntensity(totalMaxIntensity, intensitiesPerGroup, resolution);
		}
	}

	private Properties createWorkloadIntensity(int intensity) throws IOException {
		return createWorkloadIntensity(intensity, null, -1);
	}

	private Properties createWorkloadIntensity(int maxIntensity, Map<String, List<Double>> intensities, int resolution) throws IOException {
		Properties properties = new Properties();
		properties.put("workloadIntensity.type", "constant");
		properties.put("wl.type.value", Integer.toString(maxIntensity));

		if (intensities != null) {
			for (Entry<String, List<Double>> groupInt : intensities.entrySet()) {
				properties.put(PREFIX_INTENSITY_SERIES + groupInt.getKey(),
						groupInt.getValue().stream().mapToLong(Math::round).mapToInt(Math::toIntExact).mapToObj(Integer::toString).collect(Collectors.joining(",")));
			}

			properties.put(KEY_INTENSITY_RESOLUTION, Integer.toString(resolution));
		}

		properties.store(Files.newOutputStream(workingDir.resolve("workloadIntensity.properties"), StandardOpenOption.CREATE), null);

		return properties;
	}

	private void writeUsecases(MarkovBehaviorModel markovModel, Path dir) throws IOException {
		List<String> usecases = markovModel.getMarkovChains().get(0).getRequestStates();
		usecases.add(0, "INITIAL");
		Files.write(dir.resolve("usecases.txt"), usecases);
	}

	private void updateBehaviorMix(MarkovBehaviorModel markovModel, List<ForecastIntensityRecord> intensities) {
		if ((intensities == null) || (intensities.size() == 0)) {
			LOGGER.warn("Did not get any intensities. Therefore, using the default behavior mix.");
			return;
		}

		List<String> groups = intensities.stream().map(ForecastIntensityRecord::getGroups).flatMap(Set::stream).collect(Collectors.toList());

		if (groups.contains(ForecastIntensityRecord.KEY_TOTAL)) {
			LOGGER.info("Got the total intensity. Therefore, using the default behavior mix.");
			return;
		}

		LOGGER.info("Adjusting the behavior mix based on the intensities...");

		Map<String, Double> absFreq = intensities.stream().map(ForecastIntensityRecord::getContent).flatMap(map -> map.entrySet().stream())
				.filter(e -> !ForecastIntensityRecord.KEY_TIMESTAMP.equals(e.getKey())).collect(groupingBy(Entry::getKey, summingDouble(Entry::getValue)));
		double total = absFreq.values().stream().mapToDouble(x -> x).sum();

		for (RelativeMarkovChain chain : markovModel.getMarkovChains()) {
			chain.setFrequency(absFreq.get(chain.getId()) / total);
		}
	}

	private void writeBehaviorMix(MarkovBehaviorModel markovModel, Path dir) throws IOException {
		List<String> mix = markovModel.getMarkovChains().stream()
				.map(chain -> new StringBuilder().append(PREFIX_BEHAVIOR_MODEL + chain.getId()).append("; ").append(dir.resolve(toCsvFile(chain, PREFIX_BEHAVIOR_MODEL))).append("; ")
						.append(chain.getFrequency()).append("; ").append(dir.resolve(toCsvFile(chain, "behaviormodel"))).append(", \\").toString())
				.collect(toList());

		mix.set(0, "behaviorModels = " + mix.get(0));
		String last = mix.get(mix.size() - 1);
		mix.set(mix.size() - 1, last.substring(0, last.length() - 3));

		Files.write(dir.resolve("behaviormix.txt"), mix);
	}

	private void writeBehaviorModels(MarkovBehaviorModel markovModel, Path dir) throws FileNotFoundException, SecurityException, NullPointerException, IOException {
		CSVHandler csvHandler = new CSVHandler(CSVHandler.LINEBREAK_TYPE_UNIX);

		for (RelativeMarkovChain chain : markovModel.getMarkovChains()) {
			csvHandler.writeValues(dir.resolve(toCsvFile(chain, "behaviormodel")).toString(), chain.toCsv());
		}
	}

	private String toCsvFile(RelativeMarkovChain chain, String filePrefix) {
		return new StringBuilder().append(filePrefix).append(chain.getId()).append(".csv").toString();
	}

	private void writeDummySessionsDat() throws IOException {
		List<String> content = Collections.singletonList("SID;\"ID\":0:0");
		Files.write(workingDir.resolve("sessions.dat"), content);
	}

	private WorkloadModel generateWessbasModel(Properties workloadIntensityProperties, Properties behaviorModelsProperties) throws FileNotFoundException, SecurityException, GeneratorException {
		M4jdslModelGenerator generator = new M4jdslModelGenerator();
		final String sessionDatFilePath = workingDir.resolve("sessions.dat").toString();

		return generator.generateWorkloadModel(workloadIntensityProperties, behaviorModelsProperties, null, sessionDatFilePath, false);
	}

	/**
	 * Calculate intensity based on the parallel session logs.
	 *
	 * @param sessions
	 *            the session logs
	 * @param interval
	 *            the used interval/ resolution
	 * @return the intensity which represents the number of users.
	 */
	private int calculateIntensity(String sessionLogsString, long interval) {
		List<SimplifiedSession> sessions = SimplifiedSessionLogsDeserializer.parse(sessionLogsString);
		IntensityCalculationUtils.sortSessions(sessions);
		long startTime = sessions.get(0).getStartTime();

		long highestEndTime = 0;

		for (SimplifiedSession session : sessions) {
			if (session.getEndTime() > highestEndTime) {
				highestEndTime = session.getEndTime();
			}
		}

		// The time range for which an intensity will be calculated
		long rangeLength = Math.min(interval, highestEndTime - startTime);

		// rounds highest end time up
		long roundedHighestEndTime = highestEndTime;
		if ((highestEndTime % rangeLength) != 0) {
			roundedHighestEndTime = (highestEndTime - (highestEndTime % rangeLength)) + rangeLength;
		}

		long completePeriod = roundedHighestEndTime - startTime;
		long amountOfRanges = completePeriod / rangeLength;

		ArrayList<Range<Long>> listOfRanges = IntensityCalculationUtils.calculateRanges(startTime, amountOfRanges, rangeLength);

		// Remove first and last range from list if necessary
		if (listOfRanges.get(0).getMinimum() != startTime) {
			listOfRanges.remove(0);
		}

		if (listOfRanges.get(listOfRanges.size() - 1).getMaximum() != highestEndTime) {
			listOfRanges.remove(listOfRanges.size() - 1);
		}

		// This map is used to hold necessary information which will be saved into DB
		List<Integer> intensities = new ArrayList<Integer>();

		for (Range<Long> range : listOfRanges) {
			ArrayList<SimplifiedSession> sessionsInRange = new ArrayList<SimplifiedSession>();
			for (SimplifiedSession session : sessions) {
				Range<Long> sessionRange = Range.between(session.getStartTime(), session.getEndTime());
				if (sessionRange.containsRange(range) || range.contains(session.getStartTime()) || range.contains(session.getEndTime())) {
					sessionsInRange.add(session);
				}
			}
			int intensityOfRange = (int) IntensityCalculationUtils.calculateIntensityForRange(range, sessionsInRange, rangeLength);
			intensities.add(intensityOfRange);
		}

		// TODO: use list as varying intensity?

		return Math.toIntExact(Math.round(intensities.stream().mapToDouble(a -> a).average().getAsDouble()));
	}

}