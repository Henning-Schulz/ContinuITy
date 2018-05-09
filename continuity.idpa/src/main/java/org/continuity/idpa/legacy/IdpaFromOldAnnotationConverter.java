package org.continuity.idpa.legacy;

import java.io.IOException;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.PropertyOverrideKey;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.yaml.IdpaYamlSerializer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class IdpaFromOldAnnotationConverter {

	private final IdpaYamlSerializer<Application> applicationSerializer = new IdpaYamlSerializer<>(Application.class);

	private final IdpaYamlSerializer<ApplicationAnnotation> annotationSerializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);

	public Application convertFromSystemModel(ObjectNode systemModel) throws JsonParseException, JsonMappingException, IOException {
		rename(systemModel, "interfaces", "endpoints");

		return applicationSerializer.readFromJsonNode(systemModel);
	}

	public ApplicationAnnotation convertFromAnnotation(ObjectNode annotation) throws JsonParseException, JsonMappingException, IOException {
		rename(annotation, "interface-annotations", "endpoint-annotations");
		renameOverrides(annotation);

		JsonNode endpointAnnotations = annotation.get("endpoint-annotations");
		int i = 0;

		while (endpointAnnotations.has(i)) {
			JsonNode endpointAnn = endpointAnnotations.get(i);
			rename((ObjectNode) endpointAnn, "interface", "endpoint");

			renameOverrides(endpointAnn);

			JsonNode parameterAnnotations = endpointAnn.get("parameter-annotations");
			int j = 0;

			while (parameterAnnotations.has(j)) {
				renameOverrides(parameterAnnotations.get(j));

				j++;
			}

			i++;
		}

		return annotationSerializer.readFromJsonNode(annotation);
	}

	private void renameOverrides(JsonNode node) {
		JsonNode overrides = node.get("overrides");
		int i = 0;

		while (overrides.has(i)) {
			JsonNode ov = overrides.get(i);

			for (PropertyOverrideKey.HttpEndpoint key : PropertyOverrideKey.HttpEndpoint.values()) {
				if (ov.has("HttpInterface." + key.name().toLowerCase())) {
					rename((ObjectNode) ov, "HttpInterface." + key.name().toLowerCase(), key.toString());
				}
			}
		}
	}

	private void rename(ObjectNode root, String oldName, String newName) {
		JsonNode node = root.get(oldName);
		root.remove(oldName);
		root.set(newName, node);
	}

}
