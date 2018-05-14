package org.continuity.api.rest;

public class RequestBuilder {

	private String host;

	private final String path;

	private final StringBuilder queryString = new StringBuilder();
	private boolean queryStringEmpty = true;

	public RequestBuilder(String host, String path) {
		this.host = host;
		this.path = path;
	}

	public String get() {
		return "http://" + host + path + queryString;
	}

	public RequestBuilder withQuery(String param, String value) {
		if (queryStringEmpty) {
			queryString.append("?");
			queryStringEmpty = false;
		} else {
			queryString.append("&");
		}

		queryString.append(param);
		queryString.append("=");
		queryString.append(value);

		return this;
	}

	public RequestBuilder withHost(String host) {
		this.host = host;
		return this;
	}

	@Override
	public String toString() {
		return get();
	}

}
