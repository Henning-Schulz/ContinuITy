package org.continuity.orchestrator.config;

import org.continuity.api.amqp.AmqpApi;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Henning Schulz
 *
 */
@Configuration
public class RabbitMqConfig {

	private static final String SERVICE_NAME = "frontend";

	public static final String IDPA_ANNOTATION_MESSAGE_AVAILABLE_QUEUE_NAME = AmqpApi.IdpaAnnotation.MESSAGE_AVAILABLE.deriveQueueName(SERVICE_NAME);

	public static final String IDPA_ANNOTATION_MESSAGE_AVAILABLE_ROUTING_KEY = AmqpApi.IdpaAnnotation.MESSAGE_AVAILABLE.formatRoutingKey().of("report");

	public static final String DEAD_LETTER_QUEUE_NAME = AmqpApi.DEAD_LETTER_EXCHANGE.deriveQueueName(SERVICE_NAME);

	@Bean
	MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	MessagePostProcessor typeRemovingProcessor() {
		return m -> {
			m.getMessageProperties().getHeaders().remove("__TypeId__");
			return m;
		};
	}

	@Bean
	AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(jsonMessageConverter());
		rabbitTemplate.setBeforePublishPostProcessors(typeRemovingProcessor());

		return rabbitTemplate;
	}

	@Bean
	Queue idpaAnnotationMessageAvailableQueue() {
		return QueueBuilder.nonDurable(IDPA_ANNOTATION_MESSAGE_AVAILABLE_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	TopicExchange idpaAnnotationMessageAvailableExchange() {
		return AmqpApi.IdpaAnnotation.MESSAGE_AVAILABLE.create();
	}

	@Bean
	Binding idpaAnnotationMessageAvailableBinding() {
		return BindingBuilder.bind(idpaAnnotationMessageAvailableQueue()).to(idpaAnnotationMessageAvailableExchange()).with(IDPA_ANNOTATION_MESSAGE_AVAILABLE_ROUTING_KEY);
	}

	// Dead letter exchange and queue

	@Bean
	TopicExchange deadLetterExchange() {
		return AmqpApi.DEAD_LETTER_EXCHANGE.create();
	}

	@Bean
	Queue deadLetterQueue() {
		return new Queue(DEAD_LETTER_QUEUE_NAME, true);
	}

	@Bean
	Binding deadLetterBinding() {
		return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(SERVICE_NAME);
	}

}
