package org.continuity.api.entities.report;

import org.continuity.api.entities.links.LinkExchangeModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class TaskReport {

	private long taskId;

	private boolean successful;

	@JsonInclude(Include.NON_NULL)
	private LinkExchangeModel result;

	public TaskReport(long taskId, boolean successful, LinkExchangeModel result) {
		this.taskId = taskId;
		this.successful = successful;
		this.result = result;
	}

	public TaskReport() {
	}

	public long getTaskId() {
		return taskId;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

	public LinkExchangeModel getResult() {
		return result;
	}

	public void setResult(LinkExchangeModel result) {
		this.result = result;
	}

}
