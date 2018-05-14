package org.continuity.api.rest;

public class RequestBuilder {

	private final String url;

	private StringBuilder queryString;
	private boolean queryStringEmpty = true;

	public RequestBuilder(String url) {
		this.url = url;
	}

	public String get() {
		return url + queryString;
	}

	public RequestBuilder withQuery(String param, String value)
	{
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

}
