package org.continuity.api.amqp;

import org.continuity.api.amqp.RoutingKeyFormatter.Keyword;
import org.continuity.api.amqp.RoutingKeyFormatter.LoadTestType;
import org.continuity.api.amqp.RoutingKeyFormatter.ServiceName;
import org.continuity.api.amqp.RoutingKeyFormatter.Tag;
import org.continuity.api.amqp.RoutingKeyFormatter.WorkloadAndLoadTestType;
import org.continuity.api.amqp.RoutingKeyFormatter.WorkloadType;
import org.continuity.api.amqp.RoutingKeyFormatter.WorkloadTypeAndLink;

/**
 * Holds all AMQP exchange definitions of all ContinuITy services.
 *
 * @author Henning Schulz
 *
 */
public class AmqpApi {

	public static final ExchangeDefinition<ServiceName> DEAD_LETTER_EXCHANGE = ExchangeDefinition.event("global", "deadletter").nonDurable().autoDelete().withRoutingKey(ServiceName.INSTANCE);

	public static final String DEAD_LETTER_EXCHANGE_KEY = "x-dead-letter-exchange";

	public static final String DEAD_LETTER_ROUTING_KEY_KEY = "x-dead-letter-routing-key";

	private AmqpApi() {
	}

	public static class Global {

		private static final String SCOPE = "global";

		public static final ExchangeDefinition<ServiceName> EVENT_FINISHED = ExchangeDefinition.event(SCOPE, "finished").nonDurable().autoDelete().withRoutingKey(ServiceName.INSTANCE);

		private Global() {
		}

	}

	public static class SessionLogs {

		private static final String SCOPE = "sessionlogs";

		public static final ExchangeDefinition<Tag> TASK_CREATE = ExchangeDefinition.task(SCOPE, "create").nonDurable().autoDelete().withRoutingKey(Tag.INSTANCE);

		private SessionLogs() {
		}

	}

	//
	// Old API
	//

	/**
	 * AMQP API of the frontend.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Frontend {

		private static final String SCOPE = "frontend";

		public static final ExchangeDefinition<WorkloadType> DATA_AVAILABLE = ExchangeDefinition.event(SCOPE, "data.available").nonDurable().autoDelete().withRoutingKey(WorkloadType.INSTANCE);

		public static final ExchangeDefinition<LoadTestType> LOADTESTEXECUTION_REQUIRED = ExchangeDefinition.event(SCOPE, "loadtestexecution.required").nonDurable().autoDelete()
				.withRoutingKey(LoadTestType.INSTANCE);

		public static final ExchangeDefinition<WorkloadAndLoadTestType> LOADTESTCREATIONANDEXECUTION_REQUIRED = ExchangeDefinition.event(SCOPE, "loadtestcreationandexecution.required").nonDurable()
				.autoDelete().withRoutingKey(WorkloadAndLoadTestType.INSTANCE);

		private Frontend() {
		}

	}

	/**
	 * AMQP API of the IDPA annotation service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class IdpaAnnotation {

		private static final String SCOPE = "idpaannotation";

		public static final ExchangeDefinition<Keyword> MESSAGE_AVAILABLE = ExchangeDefinition.event(SCOPE, "message").nonDurable().autoDelete().withRoutingKey(Keyword.INSTANCE);

		private IdpaAnnotation() {
		}

	}

	/**
	 * AMQP API of the IDPA application service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class IdpaApplication {

		private static final String SCOPE = "idpaapplication";

		public static final ExchangeDefinition<Tag> APPLICATION_CHANGED = ExchangeDefinition.event(SCOPE, "changed").nonDurable().autoDelete().withRoutingKey(Tag.INSTANCE);

		private IdpaApplication() {
		}

	}

	/**
	 * AMQP API of the load test services, e.g., jmeter.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class LoadTest {

		private static final String SCOPE = "loadtest";

		public static final ExchangeDefinition<LoadTestType> REPORT_AVAILABLE = ExchangeDefinition.event(SCOPE, "report").nonDurable().autoDelete().withRoutingKey(LoadTestType.INSTANCE);

		private LoadTest() {
		}

	}

	/**
	 * AMQP API of the workload services, e.g., wessbas.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Workload {

		private static final String SCOPE = "workload";

		// Not declaring auto delete, since queues are bound dynamically so that the exchange might
		// have no queue for a while
		public static final ExchangeDefinition<WorkloadTypeAndLink> MODEL_CREATED = ExchangeDefinition.event(SCOPE, "model.created").nonDurable().nonAutoDelete()
				.withRoutingKey(WorkloadTypeAndLink.INSTANCE);

		private Workload() {
		}

	}

}
