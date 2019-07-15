package org.continuity.session.logs.controllers;

import static org.continuity.api.rest.RestApi.SessionLogs.Sessions.Paths.CREATE;
import static org.continuity.api.rest.RestApi.SessionLogs.Sessions.Paths.GET;
import static org.continuity.api.rest.RestApi.SessionLogs.Sessions.QueryParameters.ADD_PRE_POST_PROCESSING;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.api.entities.artifact.SessionLogsInput;
import org.continuity.api.entities.artifact.session.ExtendedRequestInformation;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionRequest;
import org.continuity.api.rest.RestApi;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.session.logs.extractor.ModularizedOPENxtraceSessionLogsExtractor;
import org.continuity.session.logs.managers.ElasticsearchSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import open.xtrace.OPENxtraceUtils;
import springfox.documentation.annotations.ApiIgnore;

/**
 *
 * @author Alper Hi
 * @author Henning Schulz
 *
 */
@RestController()
@RequestMapping(RestApi.SessionLogs.Sessions.ROOT)
public class SessionLogsController {

	@Autowired
	private RestTemplate eurekaRestTemplate;

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionLogsController.class);

	@Autowired
	private ElasticsearchSessionManager elasticManager;

	@Autowired
	private ObjectMapper mapper;

	/**
	 * Provides session logs stored in the database.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 *            The services to which the logs are to be tailored, separated by ','.
	 * @param from
	 *            The start date of the sessions.
	 * @param to
	 *            The end date of the sessions.
	 * @return The session logs as string.
	 * @throws TimeoutException
	 * @throws IOException
	 */
	@RequestMapping(value = GET, method = RequestMethod.GET, produces = { "text/plain" })
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> getSessionLogs(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String tailoring, @RequestParam(required = false) String from,
			@RequestParam(required = false) String to, @RequestParam(defaultValue = "false") boolean simple) throws IOException, TimeoutException {
		Triple<ResponseEntity<String>, Date, Date> check = checkDates(from, to);

		if (check.getLeft() != null) {
			return check.getLeft();
		}

		List<Session> sessions = elasticManager.readSessionsInRange(aid, null, Arrays.asList(tailoring.split("\\.")), check.getMiddle(), check.getRight());

		if ((sessions == null) || sessions.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		String logs = sessions.stream().map(simple ? Session::toSimpleLog : Session::toExtensiveLog).collect(Collectors.joining("\n"));

		return ResponseEntity.ok(logs);
	}

	/**
	 * Provides sessions stored in the database.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 *            The services to which the logs are to be tailored, separated by ','.
	 * @param from
	 *            The start date of the sessions.
	 * @param to
	 *            The end date of the sessions.
	 * @return {@link Session}
	 * @throws TimeoutException
	 * @throws IOException
	 */
	@RequestMapping(value = GET, method = RequestMethod.GET, produces = { "application/json" })
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> getSessionsAsJson(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String tailoring, @RequestParam(required = false) String from,
			@RequestParam(required = false) String to, @RequestParam(defaultValue = "false") boolean simple) throws IOException, TimeoutException {
		Triple<ResponseEntity<String>, Date, Date> check = checkDates(from, to);

		if (check.getLeft() != null) {
			return check.getLeft();
		}

		List<Session> sessions = elasticManager.readSessionsInRange(aid, null, Arrays.asList(tailoring.split("\\.")), check.getMiddle(), check.getRight());

		if ((sessions == null) || sessions.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		String json;

		if (simple) {
			json = mapper.writeValueAsString(sessions);
		} else {
			json = mapper.writerWithView(SessionRequest.ExtendedView.class).writeValueAsString(sessions);
		}

		return ResponseEntity.ok(json);
	}

	@RequestMapping(value = "/test", method = RequestMethod.GET)
	public String test(boolean simple) throws JsonProcessingException {
		Session session = new Session();
		session.setId("sdfhs");
		SessionRequest req = new SessionRequest();
		req.setId("12345");
		req.setExtendedInformation(new ExtendedRequestInformation());
		req.getExtendedInformation().setHost("myhost");
		session.addRequest(req);

		if (simple) {
			return mapper.writeValueAsString(Collections.singletonList(session));
		} else {
			return mapper.writerWithView(SessionRequest.ExtendedView.class).writeValueAsString(session);
		}
	}

	private Triple<ResponseEntity<String>, Date, Date> checkDates(String from, String to) {
		Date dFrom = null;

		if (from != null) {
			try {
				dFrom = ApiFormats.DATE_FORMAT.parse(from);
			} catch (ParseException e) {
				LOGGER.error("Cannot parse from date!", e);
				return Triple.of(ResponseEntity.badRequest().body("Illegal date format of 'from' date: " + from), null, null);
			}
		}

		Date dTo = null;

		if (to != null) {
			try {
				dTo = ApiFormats.DATE_FORMAT.parse(to);
			} catch (ParseException e) {
				LOGGER.error("Cannot parse to date!", e);
				return Triple.of(ResponseEntity.badRequest().body("Illegal date format of 'to' date: " + to), null, null);
			}
		}

		return Triple.of(null, dFrom, dTo);
	}

	/**
	 * Creates session logs based on the provided input data. The Session logs will be directly
	 * passed and are not stored in the storage.
	 *
	 * @param sessionLogsInput
	 *            Provides the traces and the target services.
	 * @return {@link SessionLogs}
	 */
	@RequestMapping(value = CREATE, method = RequestMethod.POST)
	public ResponseEntity<SessionLogs> getModularizedSessionLogs(@RequestBody SessionLogsInput sessionLogsInput,
			@RequestParam(name = ADD_PRE_POST_PROCESSING, defaultValue = "false") boolean addPrePostProcessing) {
		ModularizedOPENxtraceSessionLogsExtractor extractor = new ModularizedOPENxtraceSessionLogsExtractor(AppId.fromString(""), eurekaRestTemplate, sessionLogsInput.getServices(),
				addPrePostProcessing);
		List<Trace> traces = OPENxtraceUtils.deserializeIntoTraceList(sessionLogsInput.getSerializedTraces());
		String sessionLogs = extractor.getSessionLogs(traces);

		return ResponseEntity.ok(new SessionLogs(VersionOrTimestamp.MIN_VALUE, sessionLogs));
	}
}
