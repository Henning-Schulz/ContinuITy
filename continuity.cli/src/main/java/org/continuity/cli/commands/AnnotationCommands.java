package org.continuity.cli.commands;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import org.continuity.api.rest.RestApi.Frontend.Idpa;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.commons.idpa.AnnotationExtractor;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.yaml.IdpaYamlSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * CLI for annotation handling.
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class AnnotationCommands {

	@Autowired
	private PropertiesProvider propertiesProvider;

	@Autowired
	private RestTemplate restTemplate;

	@ShellMethod(key = { "download-annotation", "download-ann" }, value = "Downloads and opens the annotation with the specified tag.")
	public String downloadAnnotation(String tag) throws JsonGenerationException, JsonMappingException, IOException {
		String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));

		ResponseEntity<Application> systemResponse = restTemplate.getForEntity(Idpa.GET_APPLICATION.requestUrl(tag).withHost(url).get(), Application.class);
		ResponseEntity<ApplicationAnnotation> annotationResponse = restTemplate.getForEntity(Idpa.GET_ANNOTATION.requestUrl(tag).withHost(url).get(), ApplicationAnnotation.class);

		if (!systemResponse.getStatusCode().is2xxSuccessful()) {
			return "Could not get system model: " + systemResponse;
		}

		if (!annotationResponse.getStatusCode().is2xxSuccessful()) {
			return "Could not get annotation: " + annotationResponse;
		}

		IdpaYamlSerializer<IdpaElement> serializer = new IdpaYamlSerializer<>(IdpaElement.class);
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File systemFile = new File(workingDir + "/system-model-" + tag + ".yml");
		File annotationFile = new File(workingDir + "/annotation-" + tag + ".yml");
		serializer.writeToYaml(systemResponse.getBody(), systemFile);
		serializer.writeToYaml(annotationResponse.getBody(), annotationFile);

		Desktop.getDesktop().open(systemFile);
		Desktop.getDesktop().open(annotationFile);

		return "Downloaded and opened the system model and the annotation.";
	}

	@ShellMethod(key = { "open-annotation", "open-ann" }, value = "Opens an already downloaded annotation with the specified tag.")
	public String openAnnotation(String tag) throws IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File systemFile = new File(workingDir + "/system-model-" + tag + ".yml");
		File annotationFile = new File(workingDir + "/annotation-" + tag + ".yml");

		Desktop.getDesktop().open(systemFile);
		Desktop.getDesktop().open(annotationFile);

		return "Opened the system model and the annotation with tag " + tag + " from " + workingDir;
	}

	@ShellMethod(key = { "upload-annotation", "upload-ann" }, value = "Uploads the annotation with the specified tag.")
	public String uploadAnnotation(String tag) throws JsonParseException, JsonMappingException, IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		IdpaYamlSerializer<ApplicationAnnotation> serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);
		ApplicationAnnotation annotation = serializer.readFromYaml(workingDir + "/annotation-" + tag + ".yml");

		String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));
		ResponseEntity<String> response;
		try {
			response = restTemplate.postForEntity(Idpa.GET_ANNOTATION.requestUrl(tag).withHost(url).get(), annotation, String.class);
		} catch (HttpStatusCodeException e) {
			response = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
		}

		if (!response.getStatusCode().is2xxSuccessful()) {
			return "Error during upload: " + response;
		} else {
			return "Successfully uploaded the annotation with tag " + tag + ". Report is: " + response.getBody();
		}
	}

	@ShellMethod(key = { "upload-system", "upload-sys" }, value = "Handle with care! Uploads the system model with the specified tag. Can break the online stored annotation!")
	public String uploadSystem(String tag) throws JsonParseException, JsonMappingException, IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		IdpaYamlSerializer<Application> serializer = new IdpaYamlSerializer<>(Application.class);
		Application system = serializer.readFromYaml(workingDir + "/system-model-" + tag + ".yml");

		String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));
		ResponseEntity<String> response;
		try {
			response = restTemplate.postForEntity(Idpa.GET_APPLICATION.requestUrl(tag).withHost(url).get(), system, String.class);
		} catch (HttpStatusCodeException e) {
			response = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
		}

		if (!response.getStatusCode().is2xxSuccessful()) {
			return "Error during upload: " + response;
		} else {
			return "Successfully uploaded the system with tag " + tag + ". Report is: " + response.getBody();
		}
	}

	@ShellMethod(key = { "init-annotation", "init-ann" }, value = "Initializes an annotation for the stored system model with the specified tag.")
	public String initAnnotation(String tag) throws JsonParseException, JsonMappingException, IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File systemFile = new File(workingDir + "/system-model-" + tag + ".yml");
		File annFile = new File(workingDir + "/annotation-" + tag + ".yml");

		IdpaYamlSerializer<Application> systemSerializer = new IdpaYamlSerializer<>(Application.class);
		Application system = systemSerializer.readFromYaml(systemFile);
		ApplicationAnnotation annotation = new AnnotationExtractor().extractAnnotation(system);

		IdpaYamlSerializer<ApplicationAnnotation> annSerializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);
		annSerializer.writeToYaml(annotation, annFile);

		Desktop.getDesktop().open(systemFile);
		Desktop.getDesktop().open(annFile);

		return "Initialized and opened the annotation.";
	}

}
