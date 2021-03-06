package org.continuity.cobra.controllers;

import static org.continuity.api.rest.RestApi.Cobra.Clustering.ROOT;
import static org.continuity.api.rest.RestApi.Cobra.Clustering.Paths.TRIGGER_LATEST;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.amqp.ExchangeDefinition;
import org.continuity.api.amqp.RoutingKeyFormatter;
import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.cobra.AppendStrategyConfiguration;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.cobra.entities.ClusteringContinuation;
import org.continuity.cobra.entities.ClustinatorInput;
import org.continuity.cobra.managers.ElasticsearchBehaviorManager;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.idpa.AppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping(ROOT)
public class ClusteringController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClusteringController.class);

	@Autowired
	private ElasticsearchSessionManager sessionManager;

	@Autowired
	private ElasticsearchBehaviorManager behaviorManager;

	@Autowired
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@RequestMapping(value = TRIGGER_LATEST, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "tailoring", required = true, dataType = "string", paramType = "path") })
	public void triggerLatestClustering(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable String tailoring,
			@RequestParam(value = "ignore-timeout", defaultValue = "false") boolean ignoreTimeout, @RequestParam(defaultValue = "-1") long before,
			@RequestParam(value = "continue-with-next", defaultValue = "false") boolean continueWithNext) throws IOException, TimeoutException {

		if (before > 0) {
			triggerLatestClusteringBefore(aid, Session.convertStringToTailoring(tailoring), before, ignoreTimeout, continueWithNext);
		} else {
			triggerLatestClustering(aid, Session.convertStringToTailoring(tailoring), ignoreTimeout);
		}
	}

	public void triggerLatestClustering(AppId aid, List<String> tailoring, boolean ignoreTimeout) throws IOException, TimeoutException {
		Date sessionsEnd = sessionManager.getLatestDate(aid, null, tailoring);
		triggerLatestClusteringBefore(aid, tailoring, sessionsEnd.getTime(), ignoreTimeout, false);
	}

	public void triggerLatestClusteringBefore(AppId aid, List<String> tailoring, long clusteringEnd, boolean ignoreTimeout, boolean continueWithNext) throws IOException, TimeoutException {
		CobraConfiguration config = configProvider.getConfiguration(aid);

		Duration interval = config.getClustering().getInterval();
		Duration overlap = config.getClustering().getOverlap();
		Duration timeout = config.getSessions().getTimeout();
		long intervalMillis = interval.toMillis();
		long overlapMillis = overlap.toMillis();
		long timeoutMillis = timeout.toMillis();

		clusteringEnd = ignoreTimeout ? clusteringEnd : clusteringTimestamp(clusteringEnd, intervalMillis, timeoutMillis);
		long intervalStart = clusteringEnd - intervalMillis;

		MarkovBehaviorModel latestBefore = behaviorManager.readLatest(aid, tailoring, intervalStart - 1);
		AppendStrategyConfiguration appendStrategy = latestBefore == null ? config.getClustering().getInitial() : config.getClustering().getAppend();

		long clusteringStart = appendStrategy.getStrategy().includeOverlap() ? clusteringEnd - intervalMillis - overlapMillis : intervalStart;

		LOGGER.info("{} {}: Triggering clustering with start {}, interval start {}, end {} (respecting the session timeout of {}) ...", aid, tailoring, new Date(clusteringStart),
				new Date(intervalStart), new Date(clusteringEnd), timeout);

		ClustinatorInput input = new ClustinatorInput().setAppId(aid).setTailoring(tailoring).setAppendStrategy(appendStrategy.getStrategy()).setStartMicros(clusteringStart * 1000)
				.setIntervalStartMicros(intervalStart * 1000).setEndMicros(clusteringEnd * 1000);

		input.setMinSampleSize(appendStrategy.getMinSampleSize()).setEpsilon(appendStrategy.getEpsilon()).setAvgTransitionTolerance(appendStrategy.getAvgTransitionTolerance())
				.setK(appendStrategy.getK()).setNumSeedings(appendStrategy.getNumSeedings()).setMaxIterations(appendStrategy.getMaxIterations())
				.setConvergenceTolerance(appendStrategy.getConvergenceTolerance()).setParallelize(appendStrategy.getParallelize()).setQuantileRange(appendStrategy.getQuantileRange())
				.setRadiusFactor(appendStrategy.getRadiusFactor());

		input.setLookback(appendStrategy.getStrategy().getLookback(config.getClustering().getLookback())).setDimensions(config.getClustering().getDimensions())
				.setContinuation(ClusteringContinuation.fromBool(ignoreTimeout, continueWithNext));

		ExchangeDefinition<RoutingKeyFormatter.AppId> exchange = AmqpApi.Cobra.Clustinator.TASK_CLUSTER;
		amqpTemplate.convertAndSend(exchange.name(), exchange.formatRoutingKey().of(aid), input);

		LOGGER.info("{} {}: Clustering triggered. Waiting for the clustinator.", aid, tailoring);
	}

	private long clusteringTimestamp(long date, long intervalMillis, long timeoutMillis) {
		return ((date - timeoutMillis) / intervalMillis) * intervalMillis;
	}

}
