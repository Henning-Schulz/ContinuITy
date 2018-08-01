package org.continuity.api.entities.config;

import org.continuity.api.entities.links.LinkExchangeModel;

public class TaskDescription {

	private String taskId;

	private String tag;

	private LinkExchangeModel source;

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public LinkExchangeModel getSource() {
		return source;
	}

	public void setSource(LinkExchangeModel source) {
		this.source = source;
	}

}
