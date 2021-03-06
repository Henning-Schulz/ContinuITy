package org.continuity.cobra.entities;

import java.util.List;
import java.util.Set;

import org.continuity.api.entities.deserialization.TailoringDeserializer;
import org.continuity.api.entities.deserialization.TailoringSerializer;
import org.continuity.idpa.AppId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author Henning Schulz
 *
 */
public class ForecasticInput {

	@JsonProperty("app_id")
	private AppId appId;

	@JsonSerialize(using = TailoringSerializer.class)
	@JsonDeserialize(using = TailoringDeserializer.class)
	private List<String> tailoring;

	private Long perspective;

	private List<ForecastTimerange> ranges;

	private List<TimedContextRecord> context;

	@JsonProperty("context_variables")
	private Set<String> contextVariables;

	private long resolution;

	@JsonProperty("forecast_total")
	private boolean forecastTotal;

	private String approach;

	private TypeAndProperties aggregation;

	private List<TypeAndProperties> adjustments;

	public AppId getAppId() {
		return appId;
	}

	public ForecasticInput setAppId(AppId appId) {
		this.appId = appId;
		return this;
	}

	public List<String> getTailoring() {
		return tailoring;
	}

	public ForecasticInput setTailoring(List<String> tailoring) {
		this.tailoring = tailoring;
		return this;
	}

	public Long getPerspective() {
		return perspective;
	}

	public ForecasticInput setPerspective(Long perspective) {
		this.perspective = perspective;
		return this;
	}

	public boolean isForecastTotal() {
		return forecastTotal;
	}

	public ForecasticInput setForecastTotal(boolean forecastTotal) {
		this.forecastTotal = forecastTotal;
		return this;
	}

	public List<ForecastTimerange> getRanges() {
		return ranges;
	}

	public ForecasticInput setRanges(List<ForecastTimerange> ranges) {
		this.ranges = ranges;
		return this;
	}

	public List<TimedContextRecord> getContext() {
		return context;
	}

	public ForecasticInput setContext(List<TimedContextRecord> context) {
		this.context = context;
		return this;
	}

	public Set<String> getContextVariables() {
		return contextVariables;
	}

	public ForecasticInput setContextVariables(Set<String> contextVariables) {
		this.contextVariables = contextVariables;
		return this;
	}

	public long getResolution() {
		return resolution;
	}

	public ForecasticInput setResolution(long resolution) {
		this.resolution = resolution;
		return this;
	}

	public String getApproach() {
		return approach;
	}

	public ForecasticInput setApproach(String approach) {
		this.approach = approach;
		return this;
	}

	public TypeAndProperties getAggregation() {
		return aggregation;
	}

	public ForecasticInput setAggregation(TypeAndProperties aggregation) {
		this.aggregation = aggregation;
		return this;
	}

	public List<TypeAndProperties> getAdjustments() {
		return adjustments;
	}

	public ForecasticInput setAdjustments(List<TypeAndProperties> adjustments) {
		this.adjustments = adjustments;
		return this;
	}

}
