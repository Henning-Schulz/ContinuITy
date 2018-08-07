package org.continuity.cli.commands;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;

import org.continuity.api.entities.config.LoadTestType;
import org.continuity.api.entities.config.Order;
import org.continuity.api.entities.config.OrderGoal;
import org.continuity.api.entities.config.OrderOptions;
import org.continuity.api.entities.config.WorkloadModelType;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.api.rest.RestApi;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.entities.OrderLinks;
import org.continuity.cli.storage.OrderStorage;
import org.continuity.commons.utils.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

/**
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class OrderCommands {

	private static final String ORDERS_DIR = "orders";

	private static final String NEW_ORDER_FILENAME = "order.yml";

	@Autowired
	private PropertiesProvider propertiesProvider;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private OrderStorage storage;

	private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID));

	@ShellMethod(key = { "order-create" }, value = "Creates a new order.")
	public String createOrder(String goal) throws JsonGenerationException, JsonMappingException, IOException {
		OrderGoal orderGoal = OrderGoal.fromPrettyString(goal);

		if (orderGoal == null) {
			return "Unknown order goal " + goal + "! The allowed goals are " + Arrays.stream(OrderGoal.values()).map(OrderGoal::toPrettyString).reduce((a, b) -> a + ", " + b).get();
		}

		Order order = initializeOrder();
		order.setGoal(orderGoal);

		Desktop.getDesktop().open(storeAsNewOrder(order));

		return "Created and opened a new order at orders/order-new.yml";
	}

	@ShellMethod(key = { "order-edit" }, value = "Edits an already created order.")
	public String editOrder(@ShellOption(defaultValue = OrderStorage.ID_LATEST) String id) throws JsonGenerationException, JsonMappingException, IOException {
		Order order = storage.getOrder(id);

		if (order == null) {
			return "There is no order with ID " + id;
		}

		Desktop.getDesktop().open(storeAsNewOrder(order));

		return "Updated the current order and opened it.";
	}

	@ShellMethod(key = { "order-submit" }, value = "Submits the latest created order.")
	public String submitOrder() throws JsonParseException, JsonMappingException, IOException {
		Path orderDir = Paths.get(propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR), ORDERS_DIR, NEW_ORDER_FILENAME);
		Order order = mapper.readValue(orderDir.toFile(), Order.class);

		String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));
		ResponseEntity<OrderLinks> response;
		try {
			response = restTemplate.postForEntity(RestApi.Orchestrator.Orchestration.SUBMIT.requestUrl().withHost(url).get(), order, OrderLinks.class);
		} catch (HttpStatusCodeException e) {
			return e.getResponseBodyAsString();
		}

		String storageId = storage.newOrder(order);
		storage.storeLinks(storageId, response.getBody());

		return "Submitted the order. For further actions:\n  ID: " + storageId + "\n  wait link: " + response.getBody().getWaitLink() + "\n  result link: " + response.getBody().getResultLink();
	}

	@ShellMethod(key = { "order-wait" }, value = "Waits for an order to be finished.")
	public String waitForOrder(String timeout, @ShellOption(defaultValue = OrderStorage.ID_LATEST) String id) throws IOException {
		OrderLinks tuple = storage.getLinks(id);

		if (tuple == null) {
			return "Please create and submit an order before waiting!";
		}

		ResponseEntity<OrderReport> response;
		try {
			response = restTemplate.getForEntity(tuple.getWaitLink() + "?timeout=" + timeout, OrderReport.class);
		} catch (HttpStatusCodeException e) {
			return e.getResponseBodyAsString();
		}

		if (response.getStatusCode().equals(HttpStatus.OK) && response.hasBody()) {
			storage.storeReport(id, response.getBody());

			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getBody());
		} else {
			return "The order is not finished, yet.";
		}
	}

	@ShellMethod(key = { "order-report" }, value = "Gets the order report if available.")
	public String getOrderReport(@ShellOption(defaultValue = OrderStorage.ID_LATEST) String id) throws IOException {
		OrderReport report = storage.getReport(id);

		if (report == null) {
			return waitForOrder("0", id);
		} else {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
		}
	}

	@ShellMethod(key = { "order-clean" }, value = "Cleans the order storage.")
	public String cleanOrders() throws IOException {
		int num = storage.clean();

		return "Cleaned " + num + " orders.";
	}

	private Order initializeOrder() {
		Order order = new Order();

		order.setTag("TAG");

		OrderOptions options = new OrderOptions();
		options.setDuration(60);
		options.setNumUsers(1);
		options.setRampup(1);
		options.setLoadTestType(LoadTestType.JMETER);
		options.setWorkloadModelType(WorkloadModelType.WESSBAS);
		order.setOptions(options);

		LinkExchangeModel links = new LinkExchangeModel();
		links.getExternalDataLinks().setLink("LINK_TO_DATA").setTimestamp(new Date(0));
		links.getSessionLogsLinks().setLink("SESSION_LOGS_LINK");
		links.getWorkloadModelLinks().setType(WorkloadModelType.WESSBAS).setLink("WORKLOAD_MODEL_LINK");
		links.getLoadTestLinks().setType(LoadTestType.JMETER).setLink("LOADTEST_LINK");
		order.setSource(links);

		return order;
	}

	private File storeAsNewOrder(Order order) throws JsonGenerationException, JsonMappingException, IOException {
		Path ordersDir = Paths.get(propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR), ORDERS_DIR);
		ordersDir.toFile().mkdirs();

		File orderFile = ordersDir.resolve(NEW_ORDER_FILENAME).toFile();
		mapper.writeValue(orderFile, order);

		return orderFile;
	}

}
