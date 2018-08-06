package org.continuity.orchestrator.entities;

import java.util.Iterator;
import java.util.List;

import org.continuity.api.entities.config.Order;
import org.continuity.api.entities.config.PropertySpecification;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.TaskReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Recipe {

	private static final Logger LOGGER = LoggerFactory.getLogger(Recipe.class);

	private final String recipeId;

	private final List<RecipeStep> steps;

	private final Iterator<RecipeStep> iterator;

	private int stepCounter = 1;

	private String tag;

	private LinkExchangeModel source;

	private PropertySpecification properties;

	public Recipe(String recipeId, List<RecipeStep> steps, Order order) {
		this.recipeId = recipeId;
		this.steps = steps;
		this.iterator = steps.iterator();
		this.tag = order.getTag();
		this.source = order.getSource();

		if (order.getOptions() != null) {
			this.properties = order.getOptions().toProperties();
		}
	}

	public String getRecipeId() {
		return recipeId;
	}

	public LinkExchangeModel getSource() {
		return source;
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}

	public RecipeStep next() {
		RecipeStep nextStep = iterator.next();

		String taskId = recipeId + "." + stepCounter++ + "-" + nextStep.getName();
		TaskDescription task = new TaskDescription();
		task.setTaskId(taskId);
		task.setTag(tag);
		task.setSource(source);
		task.setProperties(properties);

		nextStep.setTask(task);

		return nextStep;
	}

	public void updateFromReport(TaskReport report) {
		try {
			source.merge(report.getResult());
		} catch (IllegalArgumentException | IllegalAccessException e) {
			LOGGER.error("Error when merging the response and existing source!", e);
		}
	}

}
