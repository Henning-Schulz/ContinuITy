package org.continuity.api.entities.config;

public class TaskDescription {

	private long taskId;

	private String tag;

	private SourceDescription source;

	public long getTaskId() {
		return taskId;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public SourceDescription getSource() {
		return source;
	}

	public void setSource(SourceDescription source) {
		this.source = source;
	}

}
