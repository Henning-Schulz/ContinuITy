package org.continuity.orchestrator.controllers;

import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.ROOT;
import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.Paths.SUBMIT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.Order;
import org.continuity.api.entities.config.OrderGoal;
import org.continuity.api.entities.config.OrderOptions.LoadTestType;
import org.continuity.api.entities.config.OrderOptions.WorkloadModelType;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MemoryStorage;
import org.continuity.orchestrator.entities.CreationStep;
import org.continuity.orchestrator.entities.DummyStep;
import org.continuity.orchestrator.entities.Recipe;
import org.continuity.orchestrator.entities.RecipeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

@RestController
@RequestMapping(ROOT)
public class OrchestrationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrchestrationController.class);

	@Autowired
	private MemoryStorage<Recipe> storage;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Value("${spring.application.name}")
	private String applicationName;

	@RequestMapping(path = SUBMIT, method = RequestMethod.POST)
	public ResponseEntity<Map<String, String>> submitOrder(@RequestBody Order order) {
		Optional<OrderGoal> goal = Optional.ofNullable(order.getGoal());
		List<RecipeStep> recipeSteps = new ArrayList<>();

		while (goal.isPresent()) {
			recipeSteps.add(createRecipeStep(goal.get(), order));
			goal = goal.get().getRequired();
		}

		recipeSteps = Lists.reverse(recipeSteps);

		String recipeId = storage.reserve(order.getTag());
		LOGGER.info("Processing new recipe {} with goal {}...", recipeId, order.getGoal());

		Recipe recipe = new Recipe(recipeId, recipeSteps, order);

		Map<String, String> responseMap = new HashMap<>();

		if (recipe.hasNext()) {
			storage.putToReserved(recipeId, recipe);
			recipe.next().execute();

			responseMap.put("get-link", RestApi.Orchestrator.Orchestration.RESULT.requestUrl(recipeId).withHost(applicationName).get());
			responseMap.put("wait-link", RestApi.Orchestrator.Orchestration.WAIT.requestUrl(recipeId).withHost(applicationName).get());

			return ResponseEntity.accepted().body(responseMap);
		} else {
			LOGGER.warn("Created empty recipe {}!", recipeId);
			responseMap.put("error", "No task contained in the order");
			return ResponseEntity.badRequest().body(responseMap);
		}
	}

	private RecipeStep createRecipeStep(OrderGoal goal, Order order) {
		RecipeStep step;
		String stepName = goal.toPrettyString();

		switch (goal) {
		case CREATE_SESSION_LOGS:
			step = new CreationStep(stepName, amqpTemplate, AmqpApi.SessionLogs.TASK_CREATE, AmqpApi.SessionLogs.TASK_CREATE.formatRoutingKey().of(order.getTag()));
			break;
		case CREATE_WORKLOAD_MODEL:
			WorkloadModelType workloadType;
			if ((order.getOptions() == null) || (order.getOptions().getWorkloadModelType() == null)) {
				workloadType = WorkloadModelType.WESSBAS;
			} else {
				workloadType = order.getOptions().getWorkloadModelType();
			}

			step = new CreationStep(stepName, amqpTemplate, AmqpApi.WorkloadModel.TASK_CREATE, AmqpApi.WorkloadModel.TASK_CREATE.formatRoutingKey().of(workloadType.toPrettyString()));
			break;
		case CREATE_LOAD_TEST:
			LoadTestType loadTestType;

			if ((order.getOptions() == null) || (order.getOptions().getLoadTestType() == null)) {
				loadTestType = LoadTestType.JMETER;
			} else {
				loadTestType = order.getOptions().getLoadTestType();
			}

			step = new CreationStep(stepName, amqpTemplate, AmqpApi.LoadTest.TASK_CREATE, AmqpApi.LoadTest.TASK_CREATE.formatRoutingKey().of(loadTestType.toPrettyString()));
			break;
		case EXECUTE_LOAD_TEST:
			loadTestType = order.getOptions().getLoadTestType();

			if (loadTestType == null) {
				loadTestType = LoadTestType.JMETER;
			}

			if (loadTestType.canExecute()) {
				step = new CreationStep(stepName, amqpTemplate, AmqpApi.LoadTest.TASK_EXECUTE, AmqpApi.LoadTest.TASK_EXECUTE.formatRoutingKey().of(loadTestType.toPrettyString()));
			} else {
				LOGGER.error("Cannot execute {} tests!", loadTestType);
				step = new DummyStep();
			}

			break;
		default:
			LOGGER.error("Cannot create {} step. Unknown goal!", order.getGoal());
			step = new DummyStep();
			break;

		}

		return step;
	}

}
