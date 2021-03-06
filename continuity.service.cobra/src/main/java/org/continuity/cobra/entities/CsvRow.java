package org.continuity.cobra.entities;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;

import org.continuity.api.entities.ApiFormats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.univocity.parsers.annotations.Headers;
import com.univocity.parsers.annotations.Parsed;
import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 * A CSV record representing a single request.
 *
 * @author Henning Schulz
 *
 */
@Headers(write = true)
public class CsvRow {

	private static final Logger LOGGER = LoggerFactory.getLogger(CsvRow.class);

	private static final String DEFAULT_ENCODING = "<no-encoding>";

	@Parsed(field = "session-id")
	private String sessionId;

	@Parsed(field = "start")
	private String startDate;

	@Parsed(field = "end")
	private String endDate;

	@Parsed
	private String name;

	@Parsed
	private String domain;

	@Parsed
	private String port;

	@Parsed
	private String path;

	@Parsed
	private String method;

	@Parsed(defaultNullRead = DEFAULT_ENCODING)
	private String encoding;

	@Parsed
	private String protocol;

	@Parsed
	private String parameters;

	@Parsed
	private String headers;

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
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

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String getHeaders() {
		return headers;
	}

	public void setHeaders(String headers) {
		this.headers = headers;
	}

	public boolean checkDates() {
		try {
			if (startDate != null) {
				ApiFormats.DATE_FORMAT.parse(startDate);
			}

			if (endDate != null) {
				ApiFormats.DATE_FORMAT.parse(endDate);
			}
		} catch (ParseException e) {
			LOGGER.error("Cannot parse date!", e);
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append(sessionId);
		builder.append(",");
		builder.append(startDate);
		builder.append(",");
		builder.append(endDate);
		builder.append(",");
		builder.append(name);
		builder.append(",");
		builder.append(domain);
		builder.append(",");
		builder.append(port);
		builder.append(",");
		builder.append(path);
		builder.append(",");
		builder.append(method);
		builder.append(",");
		builder.append(encoding);
		builder.append(",");
		builder.append(protocol);
		builder.append(",");
		builder.append(parameters);
		builder.append(",");
		builder.append(headers);

		return builder.toString();
	}

	public static List<CsvRow> listFromString(String requestLogs) {
		BeanListProcessor<CsvRow> rowProcessor = new BeanListProcessor<>(CsvRow.class);

		CsvParserSettings settings = new CsvParserSettings();
		settings.setProcessor(rowProcessor);
		settings.setHeaderExtractionEnabled(true);
		settings.setDelimiterDetectionEnabled(true, ',', ';');

		CsvParser parser = new CsvParser(settings);
		parser.parse(new ByteArrayInputStream(requestLogs.getBytes()));

		return rowProcessor.getBeans();
	}

	@Override
	public int hashCode() {
		return Objects.hash(domain, encoding, endDate, headers, method, name, parameters, path, port, protocol, sessionId, startDate);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof CsvRow)) {
			return false;
		}

		CsvRow other = (CsvRow) obj;
		return Objects.equals(domain, other.domain) && Objects.equals(encoding, other.encoding) && Objects.equals(endDate, other.endDate) && Objects.equals(headers, other.headers)
				&& Objects.equals(method, other.method) && Objects.equals(name, other.name) && Objects.equals(parameters, other.parameters) && Objects.equals(path, other.path)
				&& Objects.equals(port, other.port) && Objects.equals(protocol, other.protocol) && Objects.equals(sessionId, other.sessionId) && Objects.equals(startDate, other.startDate);
	}

}
