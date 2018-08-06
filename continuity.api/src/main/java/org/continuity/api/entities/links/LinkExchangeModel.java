package org.continuity.api.entities.links;

import java.lang.reflect.Field;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LinkExchangeModel {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@JsonProperty(value = "tag", required = false)
	@JsonInclude(Include.NON_NULL)
	private String tag;

	@JsonProperty(value = "application-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String applicationLink;

	@JsonProperty(value = "delta-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String deltaLink;

	@JsonProperty(value = "initial-annotation-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String initialAnnotationLink;

	@JsonProperty(value = "workload-type", required = false)
	@JsonInclude(Include.NON_NULL)
	private String workloadType;

	@JsonProperty(value = "workload-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String workloadLink;

	@JsonProperty(value = "jmeter-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String jmeterLink;

	@JsonProperty(value = "jmeter-report-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String jmeterReportLink;

	@JsonProperty(value = "session-logs-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String sessionLogsLink;

	@JsonProperty(value = "external-data-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String externalDataLink;

	@JsonProperty("external-data-timestamp")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH-mm-ss-SSSX")
	@JsonInclude(Include.NON_NULL)
	private Date externalDataTimestamp;

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getApplicationLink() {
		return applicationLink;
	}

	public LinkExchangeModel setApplicationLink(String applicationLink) {
		this.applicationLink = applicationLink;
		return this;
	}

	public String getDeltaLink() {
		return deltaLink;
	}

	public LinkExchangeModel setDeltaLink(String deltaLink) {
		this.deltaLink = deltaLink;
		return this;
	}

	public String getWorkloadType() {
		return workloadType;
	}

	public LinkExchangeModel setWorkloadType(String workloadType) {
		this.workloadType = workloadType;
		return this;
	}

	public String getWorkloadLink() {
		return workloadLink;
	}

	public LinkExchangeModel setWorkloadLink(String workloadLink) {
		this.workloadLink = workloadLink;
		return this;
	}

	public String getJmeterLink() {
		return jmeterLink;
	}

	public LinkExchangeModel setJmeterLink(String jmeterLink) {
		this.jmeterLink = jmeterLink;
		return this;
	}

	public String getJmeterReportLink() {
		return jmeterReportLink;
	}

	public LinkExchangeModel setJmeterReportLink(String jmeterReportLink) {
		this.jmeterReportLink = jmeterReportLink;
		return this;
	}

	public String getInitialAnnotationLink() {
		return initialAnnotationLink;
	}

	public LinkExchangeModel setInitialAnnotationLink(String initialAnnotationLink) {
		this.initialAnnotationLink = initialAnnotationLink;
		return this;
	}

	public String getSessionLogsLink() {
		return sessionLogsLink;
	}

	public LinkExchangeModel setSessionLogsLink(String sessionLogsLink) {
		this.sessionLogsLink = sessionLogsLink;
		return this;
	}

	public String getExternalDataLink() {
		return externalDataLink;
	}

	public LinkExchangeModel setExternalDataLink(String externalDataLink) {
		this.externalDataLink = externalDataLink;
		return this;
	}

	public Date getExternalDataTimestamp() {
		return externalDataTimestamp;
	}

	public LinkExchangeModel setExternalDataTimestamp(Date externalDataTimestamp) {
		this.externalDataTimestamp = externalDataTimestamp;
		return this;
	}

	public void merge(LinkExchangeModel other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : LinkExchangeModel.class.getDeclaredFields()) {
			if (field.get(this) == null) {
				field.set(this, field.get(other));
			}
		}
	}

	@Override
	public String toString() {
		try {
			return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return e + " during serialization!";
		}
	}

}
