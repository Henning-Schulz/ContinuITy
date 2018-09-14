package org.continuity.request.rates.entities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CSV record representing a single request.
 *
 * @author Henning Schulz
 *
 */
public class RequestRecord {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestRecord.class);

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss-SSSX");

	private static final String DEFAULT_ENCODING = "<no-encoding>";

	private Date startDate;

	private Date endDate;

	private String name;

	private String domain;

	private String port;

	private String path;

	private String method;

	private String encoding = DEFAULT_ENCODING;

	private String protocol;

	private List<String> parameters;

	private List<String> headers;

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public List<String> getParameters() {
		return parameters;
	}

	public void setParameters(List<String> parameters) {
		this.parameters = parameters;
	}

	public List<String> getHeaders() {
		return headers;
	}

	public void setHeaders(List<String> headers) {
		this.headers = headers;
	}

	public static String csvHeader() {
		return "startDate;endDate;name;domain;port;path;method;encoding;protocol;parameters;headers";
	}

	public String toCsvLine() {
		StringBuilder builder = new StringBuilder();

		builder.append(startDate != null ? DATE_FORMAT.format(startDate) : null);
		builder.append(";");
		builder.append(endDate != null ? DATE_FORMAT.format(endDate) : null);
		builder.append(";");
		builder.append(name);
		builder.append(";");
		builder.append(domain);
		builder.append(";");
		builder.append(port);
		builder.append(";");
		builder.append(path);
		builder.append(";");
		builder.append(method);
		builder.append(";");
		builder.append(encoding);
		builder.append(";");
		builder.append(protocol);
		builder.append(";");
		builder.append(formatList(parameters));
		builder.append(";");
		builder.append(formatList(headers));

		return builder.toString();
	}

	private String formatList(List<String> list) {
		return list.stream().reduce((a, b) -> a + "&" + b).get();
	}

	public static RequestRecord fromCsvLine(String csvLine) {
		String[] elems = csvLine.split("\\;");

		if (elems.length != csvHeader().split("\\;").length) {
			LOGGER.error("CSV line has wrong length. Should be {}, but was {}", csvHeader().split("\\;").length, elems.length);
		}

		RequestRecord rec = new RequestRecord();

		try {
			rec.setStartDate(DATE_FORMAT.parse(elems[0]));
		} catch (ParseException e) {
			LOGGER.error("Cannot parse date", e);
		}
		try {
			rec.setEndDate(DATE_FORMAT.parse(elems[1]));
		} catch (ParseException e) {
			LOGGER.error("Cannot parse date", e);
		}
		rec.setName(elems[2]);
		rec.setDomain(elems[3]);
		rec.setPort(elems[4]);
		rec.setPath(elems[5]);
		rec.setMethod(elems[6]);
		rec.setEncoding(elems[7]);
		rec.setProtocol(elems[8]);
		rec.setParameters(parseList(elems[9]));
		rec.setHeaders(parseList(elems[10]));

		return rec;
	}

	private static List<String> parseList(String listAsString) {
		return Arrays.asList(listAsString.split("\\&"));
	}

	@Override
	public String toString() {
		return toCsvLine();
	}

}
