package org.continuity.session.logs.entities;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.continuity.api.entities.artifact.session.AbstractSessionRequest;
import org.continuity.api.entities.artifact.session.ExtendedRequestInformation;
import org.continuity.api.entities.artifact.session.SessionRequest;
import org.continuity.commons.idpa.UrlPartParameterExtractor;
import org.continuity.idpa.application.HttpEndpoint;
import org.spec.research.open.xtrace.api.core.callables.HTTPRequestProcessing;

import open.xtrace.OPENxtraceUtils;

public class TraceRequest extends AbstractSessionRequest implements SessionRequest {

	private TraceRecord trace;

	private HTTPRequestProcessing callable;

	private HttpEndpoint endpoint;

	public TraceRequest(TraceRecord trace, HTTPRequestProcessing callable, HttpEndpoint endpoint) {
		this.trace = trace;
		this.callable = callable;
		this.endpoint = endpoint;
	}

	@Override
	public boolean isPrePostProcessing() {
		return false;
	}

	@Override
	public boolean isPreProcessing() {
		return false;
	}

	@Override
	public boolean isPostProcessing() {
		return false;
	}

	@Override
	public String getId() {
		if (callable.getIdentifier().isPresent()) {
			return callable.getIdentifier().get().toString();
		} else {
			return Integer.toHexString(this.hashCode());
		}
	}

	@Override
	public String getEndpoint() {
		return endpoint.getId();
	}

	@Override
	public long getStartMicros() {
		return callable.getTimestamp() * 1000;
	}

	@Override
	public long getEndMicros() {
		return callable.getExitTime() * 1000;
	}

	@Override
	public String getSessionId() {
		return OPENxtraceUtils.extractSessionIdFromCookies(callable);
	}

	@Override
	public ExtendedRequestInformation getExtendedInformation() {
		ExtendedRequestInformation info = new ExtendedRequestInformation();

		info.setUri(callable.getUri());
		info.setParameters(toParameters(callable.getHTTPParameters(), callable.getRequestBody(), extractUriParams(callable.getUri(), endpoint.getPath())));
		info.setPort(callable.getContainingSubTrace().getLocation().getPort());
		info.setHost(callable.getContainingSubTrace().getLocation().getHost());

		if (callable.getRequestMethod().isPresent()) {
			info.setMethod(callable.getRequestMethod().get().name());
		}

		if (callable.getResponseCode().isPresent()) {
			info.setResponseCode(callable.getResponseCode().get().intValue());
		}

		return info;
	}

	private String toParameters(Optional<Map<String, String[]>> httpParameters, Optional<String> body, Map<String, String[]> uriParams) {
		Map<String, String[]> params = new HashMap<String, String[]>();

		if (httpParameters.isPresent()) {
			params = Optional.ofNullable(httpParameters.get()).map(HashMap<String, String[]>::new).orElse(new HashMap<>());
		}

		// TODO: This is a workaround because the session logs do not support parameters without
		// values (e.g., host/login?logout) and WESSBAS fails if it is transformed to
		// host/login?logout=
		params = params.entrySet().stream().filter(e -> (e.getValue() != null) && (e.getValue().length > 0) && !"".equals(e.getValue()[0])).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

		params.putAll(uriParams);

		if (body.isPresent() && !body.get().isEmpty()) {
			params.put("BODY", new String[] { body.get() });
		}

		if (params.isEmpty()) {
			return null;
		} else {
			return encodeQueryString(params);
		}
	}

	/**
	 * Extracts the parameters from the URI. E.g., if the URI pattern is
	 * <code>/foo/{bar}/get/{id}</code> and the actual URI is <code>/foo/abc/get/42</code>, the
	 * extracted parameters will be <code>URL_PART_bar=abc</code> and <code>URL_PARTid=42</code>.
	 *
	 * @param uri
	 *            The URI to extract the parameters from.
	 * @param urlPattern
	 *            The abstract URI that specifies the pattern.
	 * @return The extracted parameters in the form <code>[URL_PART_name -> value]</code>.
	 */
	private Map<String, String[]> extractUriParams(String uri, String urlPattern) {
		if (uri == null) {
			return Collections.emptyMap();
		}

		UrlPartParameterExtractor extractor = new UrlPartParameterExtractor(urlPattern, uri);
		Map<String, String[]> params = new HashMap<>();

		while (extractor.hasNext()) {
			String param = extractor.nextParameter();
			String value = extractor.currentValue();

			if (value == null) {
				throw new IllegalArgumentException("Uri and pattern need to have the same length, bus was '" + uri + "' and '" + urlPattern + "'!");
			}

			params.put("URL_PART_" + param, new String[] { value });
		}

		return params;
	}

	/**
	 * Encodes a map of parameters into a query string
	 *
	 * @param params
	 * @return
	 */
	protected String encodeQueryString(Map<String, String[]> params) {
		try {
			if (params.isEmpty()) {
				return null;
			}
			StringBuffer result = new StringBuffer();
			for (String key : params.keySet()) {
				String encodedKey = URLEncoder.encode(key, "UTF-8");
				for (String value : params.get(key)) {
					String encodedValue = "";
					if (value != null) {
						encodedValue = "=" + URLEncoder.encode(value, "UTF-8");
					}

					if (result.length() > 0) {
						result.append("&");
					}
					result.append(encodedKey + encodedValue);

				}
			}
			return result.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
