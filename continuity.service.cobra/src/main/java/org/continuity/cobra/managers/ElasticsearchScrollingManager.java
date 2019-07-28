package org.continuity.cobra.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHost;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.idpa.AppId;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henning Schulz
 *
 */
public abstract class ElasticsearchScrollingManager<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchScrollingManager.class);

	private static final long SCROLL_MINUTES = 1;

	protected final RestHighLevelClient client;

	private final String mapping;

	protected ElasticsearchScrollingManager(String host, String mappingName) throws IOException {
		this.client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, 9200, "http"), new HttpHost(host, 9300, "http")));

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + mappingName + "-mapping.json")))) {
			this.mapping = reader.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	public void destroy() throws IOException {
		client.close();
	}

	protected abstract String toIndex(AppId aid, String tailoring);

	/**
	 * Transforms an element to JSON.
	 *
	 * @param element
	 * @return A pair of the JSON (left) and the document ID (right).
	 */
	protected abstract Pair<String, String> serialize(T element);

	protected abstract T deserialize(String json);

	/**
	 * Stores the elements using the default tailoring (all).
	 *
	 * @param aid
	 * @param elements
	 * @throws IOException
	 */
	protected void storeElements(AppId aid, Collection<T> elements) throws IOException {
		storeElements(aid, Collections.emptyList(), elements);
	}

	/**
	 * Stores the elements using the defined tailoring.
	 *
	 * @param aid
	 * @param tailoring
	 * @param elements
	 * @throws IOException
	 */
	protected void storeElements(AppId aid, List<String> tailoring, Collection<T> elements) throws IOException {
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));
		initIndex(index);

		BulkRequest request = new BulkRequest();

		elements.stream().map(this::serialize).filter(Objects::nonNull).forEach(json -> {
			request.add(new IndexRequest(index).source(json.getLeft(), XContentType.JSON).id(json.getRight()));
		});

		BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);

		LOGGER.info("The bulk request to {} took {} and resulted in status {}.", index, response.getTook(), response.status());
	}

	/**
	 * Reads the elements using the default tailoring (all).
	 *
	 * @param aid
	 * @param query
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 */
	protected List<T> readElements(AppId aid, QueryBuilder query, String message) throws IOException, TimeoutException {
		return readElements(aid, Collections.emptyList(), query, message);
	}

	/**
	 * Reads the elements using the defined tailoring.
	 *
	 * @param aid
	 * @param tailoring
	 * @param query
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 */
	protected List<T> readElements(AppId aid, List<String> tailoring, QueryBuilder query, String message) throws IOException, TimeoutException {
		String sTailoring = Session.convertTailoringToString(tailoring);

		if (!indexExists(toIndex(aid, sTailoring))) {
			return Collections.emptyList();
		}

		SearchRequest search = new SearchRequest(toIndex(aid, sTailoring));
		// 10000 is the maximum
		search.source(new SearchSourceBuilder().query(query).size(10000));
		search.scroll(TimeValue.timeValueMinutes(SCROLL_MINUTES));

		SearchResponse response;
		try {
			response = client.search(search, RequestOptions.DEFAULT);
		} catch (ElasticsearchStatusException e) {
			LOGGER.info("Could not get any elements from {} {}: {}", aid, toIndex(aid, sTailoring), message, e.getMessage());
			return Collections.emptyList();
		}

		SearchHits hits = response.getHits();
		LOGGER.info("The search request to {} {} resulted in {}.", aid, toIndex(aid, sTailoring), message, hits.getTotalHits());

		return processSearchResponse(response, aid, message, 0);
	}

	private List<T> processSearchResponse(SearchResponse response, AppId aid, String message, int scrollNumber) throws IOException, TimeoutException {
		if (response.isTimedOut()) {
			throw new TimeoutException(String.format("The search request to app-id %s and version %s timed out!", aid.toString()));
		}

		SearchHits hits = response.getHits();
		String scrollId = response.getScrollId();

		LOGGER.info("Scroll #{} took {} and is {}.", scrollNumber, response.getTook(), response.status());

		if (hits.getHits().length > 0) {
			List<T> results = new ArrayList<>();
			Arrays.stream(hits.getHits()).map(SearchHit::getSourceAsString).map(this::deserialize).filter(Objects::nonNull).forEach(results::add);

			results.addAll(scrollForElements(scrollId, aid, message, scrollNumber));

			return results;
		} else {
			LOGGER.info("Reached end of scroll.");
			clearScroll(scrollId);
			return Collections.emptyList();
		}
	}

	private List<T> scrollForElements(String scrollId, AppId aid, String message, int scrollNumber) throws IOException, TimeoutException {
		SearchScrollRequest scroll = new SearchScrollRequest(scrollId);
		scroll.scroll(TimeValue.timeValueMinutes(SCROLL_MINUTES));
		return processSearchResponse(client.scroll(scroll, RequestOptions.DEFAULT), aid, message, scrollNumber + 1);
	}

	private void clearScroll(String scrollId) throws IOException {
		ClearScrollRequest request = new ClearScrollRequest();
		request.addScrollId(scrollId);
		ClearScrollResponse response = client.clearScroll(request, RequestOptions.DEFAULT);

		if (response.isSucceeded()) {
			LOGGER.info("Cleared the scroll with ID {}. Freed {} contexts.", scrollId, response.getNumFreed());
		} else {
			LOGGER.error("Could not clear the scroll with ID {}: {}", scrollId, response.status());
		}
	}

	/**
	 * Checks whether a certain index exists.
	 *
	 * @param index
	 *            The index to check.
	 * @return {@code true} if it exists.
	 * @throws IOException
	 */
	protected boolean indexExists(String index) throws IOException {
		GetIndexRequest request = new GetIndexRequest(index);
		return client.indices().exists(request, RequestOptions.DEFAULT);
	}

	protected void initIndex(String index) throws IOException {
		if (indexExists(index)) {
			return;
		}

		CreateIndexRequest request = new CreateIndexRequest(index);
		request.mapping(mapping, XContentType.JSON);
		CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);

		if (response.isAcknowledged()) {
			LOGGER.info("Index {} has been created.", index);
		} else {
			LOGGER.warn("Creation of index {} has not been acknowledged!", index);
		}
	}

	protected String formatOrNull(Date date) {
		if (date == null) {
			return null;
		} else {
			return ApiFormats.DATE_FORMAT.format(date);
		}
	}

}