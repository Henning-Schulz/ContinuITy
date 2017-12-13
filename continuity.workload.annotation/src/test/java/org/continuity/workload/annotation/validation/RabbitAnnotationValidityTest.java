package org.continuity.workload.annotation.validation;

import org.continuity.workload.annotation.amqp.AnnotationAmpqHandler;
import org.continuity.workload.annotation.config.RabbitMqConfig;
import org.continuity.workload.annotation.entities.AnnotationValidityReport;
import org.continuity.workload.annotation.entities.WorkloadModelLink;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * @author Henning Schulz
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RabbitAnnotationValidityTest {

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AnnotationAmpqHandler annotationHandler;

	@Before
	public void setupMessageQueue() {
		RabbitAdmin admin = new RabbitAdmin(connectionFactory);
		Queue queue = new Queue(RabbitAnnotationValidityConfig.CLIENT_MESSAGE_QUEUE_NAME, false);
		admin.declareQueue(queue);
		TopicExchange exchange = new TopicExchange(RabbitAnnotationValidityConfig.CLIENT_MESSAGE_EXCHANGE_NAME, false, true);
		admin.declareExchange(exchange);
		admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("*"));
	}

	// @Before
	// public void setupModelCreatedQueue() {
	// WessbasA
	//
	// RabbitAdmin admin = new RabbitAdmin(connectionFactory);
	// Queue queue = new Queue(RabbitMqConfig.MODEL_CREATED_QUEUE_NAME, false);
	// admin.declareQueue(queue);
	// TopicExchange exchange = new TopicExchange(RabbitMqConfig.MODEL_CREATED_EXCHANGE_NAME, false,
	// true);
	// admin.declareExchange(exchange);
	// admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("*"));
	//
	// SimpleMessageListenerContainer container = new
	// SimpleMessageListenerContainer(connectionFactory);
	//
	// Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
	// DefaultClassMapper typeMapper = new DefaultClassMapper();
	// typeMapper.setDefaultType(HashMap.class);
	// typeMapper.setTrustedPackages("*");
	// converter.setClassMapper(typeMapper);
	// MessageListenerAdapter adapter = new MessageListenerAdapter(receivingStub, converter);
	// adapter.setMessageConverter(converter);
	// container.setMessageListener(adapter);
	// container.setQueueNames(ModelGeneratorTestConfig.MODEL_CREATED_QUEUE_NAME);
	// container.start();
	// }

	@After
	public void shutdownQueues() {
		RabbitAdmin admin = new RabbitAdmin(connectionFactory);
		admin.deleteQueue(RabbitAnnotationValidityConfig.CLIENT_MESSAGE_QUEUE_NAME);
	}

	@Test
	public void test() {
		System.out.println(restTemplate.getForEntity("http://workload-annotation/test/validation/rabbit/first/system", JsonNode.class));
		System.out.println(restTemplate.getForEntity("http://workload-annotation/dummy/dvdstore/system", JsonNode.class));

		String baseLink = "http://workload-annotation/test/validation/rabbit";

		WorkloadModelLink link = new WorkloadModelLink();
		link.setTag("RabbitAnnotationValidityTest");
		link.setSystemModelLink(baseLink + "/first/system");
		link.setAnnotationLink(baseLink + "/first/annotation");
		amqpTemplate.convertAndSend(RabbitMqConfig.MODEL_CREATED_QUEUE_NAME, "test", link);
		AnnotationValidityReport report = amqpTemplate.receiveAndConvert(RabbitAnnotationValidityConfig.CLIENT_MESSAGE_QUEUE_NAME, ParameterizedTypeReference.forType(AnnotationValidityReport.class));
		Assert.assertNull(report);

		link.setSystemModelLink(baseLink + "/second/system");
		link.setAnnotationLink(baseLink + "/first/annotation");
		report = amqpTemplate.receiveAndConvert(RabbitAnnotationValidityConfig.CLIENT_MESSAGE_QUEUE_NAME, ParameterizedTypeReference.forType(AnnotationValidityReport.class));
		amqpTemplate.convertAndSend(RabbitMqConfig.MODEL_CREATED_QUEUE_NAME, "test", link);
		Assert.assertNotNull(report);
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());

		link.setSystemModelLink(baseLink + "/third/system");
		link.setAnnotationLink(baseLink + "/first/annotation");
		report = amqpTemplate.receiveAndConvert(RabbitAnnotationValidityConfig.CLIENT_MESSAGE_QUEUE_NAME, ParameterizedTypeReference.forType(AnnotationValidityReport.class));
		amqpTemplate.convertAndSend(RabbitMqConfig.MODEL_CREATED_QUEUE_NAME, "test", link);
		Assert.assertNotNull(report);
		Assert.assertFalse(report.isOk());
		Assert.assertTrue(report.isBreaking());
	}

}
