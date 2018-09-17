package org.continuity.request.rates.transform;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.commons.utils.StringUtils;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.continuity.request.rates.entities.RequestRecord;
import org.continuity.request.rates.model.RequestRatesModel;

public class RequestRatesCalculator {

	private final Application application;

	private final RequestUriMapper uriMapper;

	public RequestRatesCalculator() {
		this.application = null;
		this.uriMapper = null;
	}

	public RequestRatesCalculator(Application application) {
		this.application = application;
		this.uriMapper = new RequestUriMapper(application);
	}

	public RequestRatesModel calculate(List<RequestRecord> records) {
		RequestRatesModel model = new RequestRatesModel();

		sortRecords(records);

		model.setRequestPerMinute(records.size() / calculateDuration(records));

		if (application == null) {
			model.setEndpoints(calculateEndpointsUsingNames(records));
		} else {
			model.setEndpoints(calculateEndpointsUsingApplication(records));
		}

		return model;
	}

	private void sortRecords(List<RequestRecord> records) {
		Collections.sort(records, (RequestRecord a, RequestRecord b) -> {
			int startTimeComparison = a.getStartDate().compareTo(b.getStartDate());

			if (startTimeComparison != 0) {
				return startTimeComparison;
			} else {
				return a.getEndDate().compareTo(b.getEndDate());
			}
		});
	}

	private long calculateDuration(List<RequestRecord> records) {
		Date startDate = records.get(0).getStartDate();
		Date endDate = records.get(records.size() - 1).getStartDate();

		return TimeUnit.MINUTES.convert(endDate.getTime() - startDate.getTime(), TimeUnit.MILLISECONDS);
	}

	private Map<Double, Endpoint<?>> calculateEndpointsUsingApplication(List<RequestRecord> records) {
		return records.stream().map(rec -> uriMapper.map(rec.getPath(), rec.getMethod())).filter(Objects::nonNull).collect(Collectors.groupingBy(HttpEndpoint::getId)).entrySet().stream()
				.map(entry -> Pair.of(((double) entry.getValue().size()) / records.size(), entry.getValue().get(0))).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	private Map<Double, Endpoint<?>> calculateEndpointsUsingNames(List<RequestRecord> records) {
		return records.stream().collect(Collectors.groupingBy(RequestRecord::getName)).entrySet().stream()
				.map(entry -> Pair.of(((double) entry.getValue().size()) / records.size(), aggregateRequests(entry.getValue()))).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	private Endpoint<?> aggregateRequests(List<RequestRecord> records) {
		HttpEndpoint endpoint = new HttpEndpoint();

		endpoint.setId(getFirst(records, RequestRecord::getName));

		endpoint.setDomain(getFirst(records, RequestRecord::getDomain));
		endpoint.setPort(getFirst(records, RequestRecord::getPort));
		endpoint.setPath(getFirst(records, RequestRecord::getPath));
		endpoint.setMethod(getFirst(records, RequestRecord::getMethod));
		endpoint.setProtocol(getFirst(records, RequestRecord::getMethod));
		endpoint.setEncoding(getFirst(records, RequestRecord::getEncoding));

		endpoint.setHeaders(extractHeaders(records));

		endpoint.setParameters(extractHttpParameters(records));
		setParameterIds(endpoint);

		return endpoint;
	}

	private <T> T getFirst(List<RequestRecord> records, Function<RequestRecord, T> getter) {
		for (RequestRecord rec : records) {
			T elem = getter.apply(rec);
			if (elem != null) {
				return elem;
			}
		}

		return null;
	}

	private List<String> extractHeaders(List<RequestRecord> records) {
		return records.stream().map(RequestRecord::getHeaders).flatMap(List::stream).distinct().collect(Collectors.toList());
	}

	private List<HttpParameter> extractHttpParameters(List<RequestRecord> records) {
		return records.stream().map(RequestRecord::getHeaders).flatMap(List::stream).distinct().map(name -> {
			HttpParameter param = new HttpParameter();

			if (name.startsWith("_BODY")) {
				param.setParameterType(HttpParameterType.BODY);
			} else if (name.startsWith("URL_PART")) {
				param.setName(name.substring("URL_PART".length()));
				param.setParameterType(HttpParameterType.URL_PART);
			} else {
				param.setName(name);
				param.setParameterType(HttpParameterType.REQ_PARAM);
			}

			return param;
		}).collect(Collectors.toList());
	}

	private void setParameterIds(HttpEndpoint interf) {
		final Set<String> ids = new HashSet<>();

		for (HttpParameter param : interf.getParameters()) {
			String id = StringUtils.formatAsId(true, interf.getId(), param.getName(), param.getParameterType().toString());
			String origId = id;
			int i = 2;

			while (ids.contains(id)) {
				id = origId + "_" + i++;
			}

			ids.add(id);
			param.setId(id);
		}
	}

}
