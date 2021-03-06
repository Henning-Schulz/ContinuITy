package org.continuity.wessbas.managers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.continuity.api.entities.artifact.SessionsBundle;
import org.continuity.api.entities.artifact.SessionsBundlePack;
import org.continuity.api.entities.artifact.SimplifiedSession;
import org.continuity.idpa.VersionOrTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.markov4jmeter.behavior.BehaviorMix;
import net.sf.markov4jmeter.behavior.Session;
import net.sf.markov4jmeter.behaviormodelextractor.BehaviorModelExtractor;
import net.sf.markov4jmeter.behaviormodelextractor.extraction.ExtractionException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import wessbas.commons.parser.ParseException;

/**
 *
 * @author Alper Hidiroglu
 *
 */
public class BehaviorMixManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(BehaviorMixManager.class);

	private final Path workingDir;

	private final VersionOrTimestamp version;

	public Path getWorkingDir() {
		return workingDir;
	}

	/**
	 * Constructor.
	 */
	public BehaviorMixManager(VersionOrTimestamp version) {
		this.version = version;

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
	public BehaviorMixManager(VersionOrTimestamp version, Path workingDir) {
		this.workingDir = workingDir;
		this.version = version;
	}

	/**
	 * Runs the pipeline and returns a SessionsBundlePack that holds a list of SessionBundles.
	 *
	 *
	 * @param task
	 *            Input monitoring data to be transformed into a WESSBAS DSL instance.
	 *
	 * @return The generated workload model.
	 */
	public SessionsBundlePack runPipeline(String sessionLog) {
		BehaviorMix mix;
		SessionsBundlePack sessionsBundles;

		try {
			mix = convertSessionLogIntoBehaviorMix(sessionLog);
			sessionsBundles = extractSessions(version, mix);

		} catch (Exception e) {
			LOGGER.error("Could not create the Behavior Mix!", e);
			mix = null;
			sessionsBundles = null;
		}

		return sessionsBundles;
	}

	/**
	 * This method extracts the Behavior Mix from a session log.
	 *
	 * @param sessionLog
	 * @throws IOException
	 * @throws GeneratorException
	 * @throws SecurityException
	 */
	private BehaviorMix convertSessionLogIntoBehaviorMix(String sessionLog) throws IOException, SecurityException, GeneratorException, ExtractionException, ParseException {
		Path sessionLogsPath = writeSessionLogIntoFile(sessionLog);
		BehaviorMix mix = createBehaviorMix(sessionLogsPath);
		return mix;
	}

	/**
	 *
	 * @param sessionLog
	 * @return
	 * @throws IOException
	 */
	private Path writeSessionLogIntoFile(String sessionLog) throws IOException {
		Path sessionLogsPath = workingDir.resolve("sessions.dat");
		Files.write(sessionLogsPath, Collections.singletonList(sessionLog), StandardOpenOption.CREATE);
		return sessionLogsPath;
	}

	/**
	 * Creates the Behavior Mix and writes the corresponding files.
	 *
	 * @param sessionLogsPath
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 * @throws ExtractionException
	 */
	private BehaviorMix createBehaviorMix(Path sessionLogsPath) throws IOException, ParseException, ExtractionException {
		Path outputDir = workingDir.resolve("behaviormodelextractor");
		outputDir.toFile().mkdir();

		BehaviorModelExtractor extractor = new BehaviorModelExtractor();
		extractor.init(null, null, 0);
		BehaviorMix mix = extractor.extractBehaviorMix(sessionLogsPath.toString(), outputDir.toString());

		extractor.writeIntoFiles(mix, outputDir.toString());

		return mix;
	}

	/**
	 *
	 * @param mix
	 * @return
	 */
	private SessionsBundlePack extractSessions(VersionOrTimestamp version, BehaviorMix mix) {
		SessionsBundlePack sessionsBundles = new SessionsBundlePack(version, new LinkedList<>());
		for (int i = 0; i < mix.getEntries().size(); i++) {
			List<SimplifiedSession> simplifiedSessions = new LinkedList<SimplifiedSession>();
			for (Session session : mix.getEntries().get(i).getSessions()) {
				SimplifiedSession simpleSession = new SimplifiedSession(session.getId(), session.getStartTime(), session.getEndTime());
				simplifiedSessions.add(simpleSession);
			}
			SessionsBundle sessBundle = new SessionsBundle(i, simplifiedSessions);
			sessionsBundles.getSessionsBundles().add(sessBundle);
		}
		return sessionsBundles;
	}

}
