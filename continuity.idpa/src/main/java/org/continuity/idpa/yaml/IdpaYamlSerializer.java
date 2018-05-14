package org.continuity.idpa.yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import org.continuity.idpa.AbstractIdpaElement;
import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.annotation.PropertyOverride;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

/**
 * @author Henning Schulz
 *
 */
public class IdpaYamlSerializer<T extends IdpaElement> {

	private final Class<T> type;

	/**
	 *
	 */
	public IdpaYamlSerializer(Class<T> type) {
		this.type = type;
	}

	public T readFromYaml(File yamlSource) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = createReadMapper();
		T read = mapper.readValue(yamlSource, type);
		return read;
	}

	public T readFromYaml(String yamlSource) throws JsonParseException, JsonMappingException, IOException {
		return readFromYaml(new File(yamlSource));
	}

	public T readFromYaml(URL yamlSource) throws JsonParseException, JsonMappingException, IOException {
		return readFromYaml(yamlSource.getPath());
	}

	public T readFromYaml(Path yamlPath) throws JsonParseException, JsonMappingException, IOException {
		return readFromYaml(yamlPath.toString());
	}

	public T readFromYamlInputStream(InputStream inputStream) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = createReadMapper();
		T read = mapper.readValue(inputStream, type);
		return read;
	}

	public T readFromYamlString(String yamlString) throws JsonParseException, JsonMappingException, IOException {
		return createReadMapper().readValue(yamlString, type);
	}

	public T readFromJsonNode(JsonNode root) throws JsonParseException, JsonMappingException, IOException {
		return createReadMapper().readerFor(type).readValue(root);
	}

	private ObjectMapper createReadMapper() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID));
		mapper.registerModule(new SimpleModule().setDeserializerModifier(new ContinuityDeserializerModifier()));

		mapper.registerModule(new SimpleModule().addDeserializer(PropertyOverride.class, new PropertyOverrideDeserializer()));

		return mapper;
	}

	public void writeToYaml(T model, File yamlFile) throws JsonGenerationException, JsonMappingException, IOException {
		createWriter().writeValue(yamlFile, model);
	}

	public void writeToYaml(T model, String yamlFile) throws JsonGenerationException, JsonMappingException, IOException {
		writeToYaml(model, new File(yamlFile));
	}

	public void writeToYaml(T model, URL yamlFile) throws JsonGenerationException, JsonMappingException, IOException {
		writeToYaml(model, yamlFile.getPath());
	}

	public void writeToYaml(T model, Path yamlPath) throws JsonGenerationException, JsonMappingException, IOException {
		writeToYaml(model, yamlPath.toString());
	}

	public String writeToYamlString(T model) throws JsonProcessingException {
		return createWriter().writeValueAsString(model);
	}

	private ObjectWriter createWriter() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID));
		mapper.addMixIn(AbstractIdpaElement.class, IdpaElementMixin.class);
		mapper.registerModule(new SimpleModule().setSerializerModifier(new ContinuitySerializerModifier()));
		mapper.registerModule(new SimpleModule().addSerializer(getPropertyOverrideClass(), new PropertyOverrideSerializer()));
		return mapper.writer(new SimpleFilterProvider().addFilter("idFilter", new IdFilter()));
	}

	@SuppressWarnings("unchecked")
	private Class<PropertyOverride<?>> getPropertyOverrideClass() {
		Class<?> clazz = PropertyOverride.class;
		return (Class<PropertyOverride<?>>) clazz;
	}

	private static class ContinuitySerializerModifier extends BeanSerializerModifier {

		@SuppressWarnings("unchecked")
		@Override
		public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
			if (IdpaElement.class.isAssignableFrom(beanDesc.getBeanClass())) {
				return new IdpaSerializer((JsonSerializer<Object>) serializer);
			}

			return serializer;
		}

	}

	private static class ContinuityDeserializerModifier extends BeanDeserializerModifier {

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
			if (IdpaElement.class.isAssignableFrom(beanDesc.getBeanClass())) {
				return new IdpaDeserializer((JsonDeserializer<Object>) deserializer);
			}

			return deserializer;
		}

	}

	private static class IdFilter extends SimpleBeanPropertyFilter {

		@Override
		public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception {
			if (include(writer)) {
				if (!writer.getName().equals("id")) {
					writer.serializeAsField(pojo, jgen, provider);
					return;
				}
			} else if (!jgen.canOmitFields()) { // since 2.3
				writer.serializeAsOmittedField(pojo, jgen, provider);
			}
		}

		@Override
		protected boolean include(BeanPropertyWriter writer) {
			return true;
		}

		@Override
		protected boolean include(PropertyWriter writer) {
			return true;
		}
	}
}
