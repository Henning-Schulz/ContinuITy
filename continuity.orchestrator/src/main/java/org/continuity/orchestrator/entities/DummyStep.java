package org.continuity.orchestrator.entities;

import org.continuity.api.entities.config.TaskDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyStep implements RecipeStep {

	private static final Logger LOGGER = LoggerFactory.getLogger(DummyStep.class);

	private TaskDescription task;

	@Override
	public void execute() {
		LOGGER.warn("Dummy step for task {} - doing nothing!", task.getTaskId());

		// TODO: trigger next
	}

	@Override
	public void setTask(TaskDescription task) {
		this.task = task;
	}

	@Override
	public String getName() {
		return "dummy";
	}

}
