package org.continuity.session.logs.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.entities.ApiFormats;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.session.logs.entities.TraceRecord;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Trace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Henning Schulz
 *
 */
public class ElasticsearchTraceManager extends ElasticsearchScrollingManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchTraceManager.class);

	private static final long SCROLL_MINUTES = 1;

	private final ObjectMapper mapper;

	public ElasticsearchTraceManager(String host, ObjectMapper mapper) throws IOException {
		super(host, "trace");
		this.mapper = mapper;
	}

	public void destroy() throws IOException {
		client.close();
	}

	/**
	 * Stores the passed traces to the elasticsearch database.
	 *
	 * @param aid
	 *            The app-id of the corresponding application.
	 * @param version
	 *            The version of the application.
	 * @param traces
	 *            The traces to be stored.
	 * @throws IOException
	 */
	public void storeTraces(AppId aid, VersionOrTimestamp version, List<Trace> traces) throws IOException {
		storeTraceRecords(aid, version, traces.stream().map(t -> new TraceRecord(version, t)).collect(Collectors.toList()));
	}

	/**
	 * Stores the passed trace records to the elasticsearch database.
	 *
	 * @param aid
	 *            The app-id of the corresponding application.
	 * @param version
	 *            The version of the application.
	 * @param traces
	 *            The trace records to be stored.
	 * @throws IOException
	 */
	public void storeTraceRecords(AppId aid, VersionOrTimestamp version, List<TraceRecord> traces) throws IOException {
		initIndex(toTraceIndex(aid));

		BulkRequest request = new BulkRequest();

		traces.stream().map(this::serializeTrace).filter(Objects::nonNull).forEach(json -> {
			request.add(new IndexRequest(toTraceIndex(aid)).source(json.getLeft(), XContentType.JSON).id(Long.toString(json.getRight())));
		});

		BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);

		LOGGER.info("The bulk request to app-id {} and version {} took {} and resulted in status {}.", aid, version, response.getTook(), response.status());
	}

	private Pair<String, Long> serializeTrace(TraceRecord record) {
		try {
			return Pair.of(mapper.writeValueAsString(record), record.getTrace().getTraceId());
		} catch (JsonProcessingException e) {
			LOGGER.error("Could not write TraceRecord to JSON string!", e);
			return null;
		}
	}

	/**
	 * Reads the traces of a given app-id, version (or timestamp), and time range from the database.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp. Can be {@code null}. In this case, it will be ignored.
	 * @param from
	 *            The lower limit. {@code null} means unbound.
	 * @param to
	 *            The upper limit. {@code null} means unbound.
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 *             If a request to the database times out.
	 */
	public List<Trace> readTraces(AppId aid, VersionOrTimestamp version, Date from, Date to) throws IOException, TimeoutException {
		return readTraceRecords(aid, version, from, to).stream().map(TraceRecord::getTrace).collect(Collectors.toList());
	}

	/**
	 * Reads the traces of a given app-id, version (or timestamp), and time range from the database.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp. Can be {@code null}. In this case, it will be ignored.
	 * @param from
	 *            The lower limit. {@code null} means unbound.
	 * @param to
	 *            The upper limit. {@code null} means unbound.
	 * @return The found traces as {@link TraceRecord}.
	 * @throws IOException
	 * @throws TimeoutException
	 *             If a request to the database times out.
	 */
	public List<TraceRecord> readTraceRecords(AppId aid, VersionOrTimestamp version, Date from, Date to) throws IOException, TimeoutException {
		SearchRequest search = new SearchRequest(toTraceIndex(aid));

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (version != null) {
			query = query.must(QueryBuilders.termQuery("version", version.toNormalizedString()));
		}

		if ((from != null) && (to != null)) {
			query.must(QueryBuilders.rangeQuery("trace.rootOfTrace.rootOfSubTrace.timeStamp").from(from.getTime(), false).to(to.getTime(), true));
		} else {
			LOGGER.warn("The provided time range ({} - {}) contains null elements! Ignoring.", from, to);
		}

		search.source(new SearchSourceBuilder().query(query).size(10000)); // This is the maximum

		search.scroll(TimeValue.timeValueMinutes(SCROLL_MINUTES));

		SearchResponse response = client.search(search, RequestOptions.DEFAULT);
		SearchHits hits = response.getHits();

		String message = String.format(", version %s, and time range %s - %s", version.toString(), formatOrNull(from), formatOrNull(to));
		LOGGER.info("The search request to app-id {}{} resulted in {}.", aid, message, hits.getTotalHits());

		return processSearchResponse(response, aid, message, 0);
	}

	/**
	 * Reads all traces having one of the defined unique session IDs.
	 *
	 * @param aid
	 * @param rootEndpoint
	 *            The root endpoint to filter for. Can be {@code null}. In this case, it will be
	 *            ignored.
	 * @param uniqueSessionIds
	 *            The unique (!) session IDs.
	 * @return The found traces as {@link TraceRecord}.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<TraceRecord> readTraceRecords(AppId aid, String rootEndpoint, List<String> uniqueSessionIds) throws IOException, TimeoutException {
		SearchRequest search = new SearchRequest(toTraceIndex(aid));

		BoolQueryBuilder query;

		TermsQueryBuilder sessionQuery = QueryBuilders.termsQuery("unique-session-ids", uniqueSessionIds);

		if (rootEndpoint != null) {
			query = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("endpoint", rootEndpoint)).must(sessionQuery);
		} else {
			query = QueryBuilders.boolQuery().must(sessionQuery);
		}

		search.source(new SearchSourceBuilder().query(query).size(10000)); // This is the maximum
		search.scroll(TimeValue.timeValueMinutes(SCROLL_MINUTES));

		SearchResponse response = client.search(search, RequestOptions.DEFAULT);
		SearchHits hits = response.getHits();
		LOGGER.info("The search request to app-id {} and unique IDs resulted in {}.", aid, hits.getTotalHits());

		return processSearchResponse(response, aid, " for unique IDs", 0);
	}

	private String formatOrNull(Date date) {
		if (date == null) {
			return null;
		} else {
			return ApiFormats.DATE_FORMAT.format(date);
		}
	}

	private List<TraceRecord> processSearchResponse(SearchResponse response, AppId aid, String message, int scrollNumber) throws IOException, TimeoutException {
		if (response.isTimedOut()) {
			throw new TimeoutException(
					String.format("The search request to app-id %s%s timed out!", aid.toString(), message));
		}

		SearchHits hits = response.getHits();
		String scrollId = response.getScrollId();

		LOGGER.info("Scroll #{} took {} and is {}.", scrollNumber, response.getTook(), response.status());

		if (hits.getHits().length > 0) {
			List<TraceRecord> traces = new ArrayList<>();
			Arrays.stream(hits.getHits()).map(SearchHit::getSourceAsString).map(this::readFromString).filter(Objects::nonNull).forEach(traces::add);

			traces.addAll(scrollForTraces(scrollId, aid, message, scrollNumber));

			return traces;
		} else {
			LOGGER.info("Reached end of scroll.");
			clearScroll(scrollId);
			return Collections.emptyList();
		}
	}

	private List<TraceRecord> scrollForTraces(String scrollId, AppId aid, String message, int scrollNumber) throws IOException, TimeoutException {
		SearchScrollRequest scroll = new SearchScrollRequest(scrollId);
		scroll.scroll(TimeValue.timeValueMinutes(SCROLL_MINUTES));
		return processSearchResponse(client.scroll(scroll, RequestOptions.DEFAULT), aid, message, scrollNumber + 1);
	}

	private TraceRecord readFromString(String json) {
		try {
			return mapper.readValue(json, TraceRecord.class);
		} catch (IOException e) {
			LOGGER.error("Could not read TraceRecord from JSON string!", e);
			return null;
		}
	}

	private String toTraceIndex(AppId aid) {
		return aid.dropService() + ".traces";
	}

}
