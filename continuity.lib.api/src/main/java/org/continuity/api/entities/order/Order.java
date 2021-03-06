package org.continuity.api.entities.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.exchange.ArtifactType;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.lctl.WorkloadDescription;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "target", "app-id", "services", "version", "perspective", "testing-context", "workload", "options", "source" })
public class Order {

	@JsonProperty("app-id")
	private AppId appId;

	@JsonInclude(Include.NON_EMPTY)
	private List<ServiceSpecification> services;

	@JsonInclude(Include.NON_NULL)
	private VersionOrTimestamp version;

	private ArtifactType target;

	@JsonInclude(Include.NON_NULL)
	private LocalDateTime perspective;

	@JsonProperty("testing-context")
	@JsonInclude(Include.NON_EMPTY)
	private Set<String> testingContext;

	@JsonInclude(Include.NON_NULL)
	private WorkloadDescription workload;

	@JsonInclude(Include.NON_NULL)
	private ArtifactExchangeModel source;

	@JsonInclude(Include.NON_NULL)
	private OrderOptions options;

	public AppId getAppId() {
		return appId;
	}

	public void setAppId(AppId appId) {
		this.appId = appId;
	}

	public List<ServiceSpecification> getServices() {
		return services;
	}

	public void setServices(List<ServiceSpecification> services) {
		this.services = services;
	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public void setVersion(VersionOrTimestamp version) {
		this.version = version;
	}

	public ArtifactType getTarget() {
		return target;
	}

	public void setTarget(ArtifactType target) {
		this.target = target;
	}

	public LocalDateTime getPerspective() {
		return perspective;
	}

	public void setPerspective(LocalDateTime perspective) {
		this.perspective = perspective;
	}

	public Set<String> getTestingContext() {
		return testingContext;
	}

	public void setTestingContext(Set<String> testingContext) {
		this.testingContext = testingContext;
	}

	public WorkloadDescription getWorkload() {
		return workload;
	}

	public void setWorkload(WorkloadDescription workloadDescription) {
		this.workload = workloadDescription;
	}

	public ArtifactExchangeModel getSource() {
		return source;
	}

	public void setSource(ArtifactExchangeModel source) {
		this.source = source;
	}

	public OrderOptions getOptions() {
		return options;
	}

	public void setOptions(OrderOptions options) {
		this.options = options;
	}

}
