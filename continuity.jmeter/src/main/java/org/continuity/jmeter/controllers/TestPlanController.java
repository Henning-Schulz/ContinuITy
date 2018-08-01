package org.continuity.jmeter.controllers;

import static org.continuity.api.rest.RestApi.JMeter.TestPlan.ROOT;
import static org.continuity.api.rest.RestApi.JMeter.TestPlan.Paths.CAN_EXECUTE;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controls creation and execution of JMeter test plans.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class TestPlanController {

	@RequestMapping(value = CAN_EXECUTE, method = RequestMethod.GET)
	public boolean canExecute() {
		return true;
	}

}
