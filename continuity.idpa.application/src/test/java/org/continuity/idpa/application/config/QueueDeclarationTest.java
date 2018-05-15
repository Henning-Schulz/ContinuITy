package org.continuity.idpa.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.continuity.api.amqp.AmqpApi;
import org.junit.Test;

public class QueueDeclarationTest {

	@Test
	public void test() {
		assertThat(RabbitMqConfig.WORKLOAD_MODEL_CREATED_QUEUE_NAME).as("The defined queue name sould be equal to the derived one.")
				.isEqualTo(AmqpApi.Workload.MODEL_CREATED.deriveQueueName(RabbitMqConfig.SERVICE_NAME));
	}

}
