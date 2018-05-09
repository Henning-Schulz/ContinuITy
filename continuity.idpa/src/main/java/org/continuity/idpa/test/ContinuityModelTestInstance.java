package org.continuity.idpa.test;

import java.io.IOException;

import org.continuity.idpa.WeakReference;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.CsvInput;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ExtractedInput;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.annotation.PropertyOverride;
import org.continuity.idpa.annotation.PropertyOverrideKey;
import org.continuity.idpa.annotation.RegExExtraction;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.continuity.idpa.yaml.IdpaYamlSerializer;

/**
 * @author Henning Schulz
 *
 */
public enum ContinuityModelTestInstance {

	SIMPLE {

		@Override
		protected Application setupSystemModel() {
			Application system = new Application();

			HttpEndpoint interf = new HttpEndpoint();
			interf.setDomain("mydomain");
			interf.setId("login");

			HttpParameter param = new HttpParameter();
			param.setId("user");
			param.setParameterType(HttpParameterType.REQ_PARAM);
			interf.getParameters().add(param);

			system.addEndpoint(interf);

			return system;
		}

		@Override
		protected ApplicationAnnotation setupAnnotation(Application system) {
			ApplicationAnnotation annotation = new ApplicationAnnotation();

			HttpEndpoint interf = (HttpEndpoint) system.getEndpoints().get(0);

			// Input

			DirectListInput input = new DirectListInput();
			input.setId("DAT1");
			input.getData().add("foo");
			input.getData().add("bar");

			CsvInput csvInput = new CsvInput();
			csvInput.setFilename("myfile.csv");
			csvInput.setColumn(3);
			csvInput.getAssociated().add(input);

			ExtractedInput extrInput = new ExtractedInput();
			RegExExtraction extr = new RegExExtraction();
			extr.setFrom(WeakReference.create(interf));
			extr.setPattern("(.*)");
			extrInput.getExtractions().add(extr);

			DirectListInput unknownInput = new DirectListInput();
			unknownInput.setId("UNK1");
			unknownInput.getData().add("something");

			// Annotation

			annotation = new ApplicationAnnotation();
			annotation.getInputs().add(input);
			annotation.getInputs().add(csvInput);
			annotation.getInputs().add(extrInput);
			annotation.getInputs().add(unknownInput);
			annotation.setId("ANN");

			EndpointAnnotation interfaceAnn = new EndpointAnnotation();
			interfaceAnn.setAnnotatedEndpoint(WeakReference.create(interf));
			PropertyOverride<PropertyOverrideKey.EndpointLevel> ov = new PropertyOverride<>();
			ov.setKey(PropertyOverrideKey.HttpEndpoint.DOMAIN);
			ov.setValue("localhost");
			interfaceAnn.addOverride(ov);

			ParameterAnnotation paramAnn = new ParameterAnnotation();
			paramAnn.setAnnotatedParameter(WeakReference.create(interf.getParameters().get(0)));
			paramAnn.setInput(input);

			interfaceAnn.getParameterAnnotations().add(paramAnn);
			annotation.getEndpointAnnotations().add(interfaceAnn);

			return annotation;
		}

	},

	DVDSTORE_PARSED {
		@Override
		protected Application setupSystemModel() {
			IdpaYamlSerializer<Application> serializer = new IdpaYamlSerializer<>(Application.class);

			try {
				return serializer.readFromYamlInputStream(getClass().getResourceAsStream("/dvdstore-systemmodel.yml"));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected ApplicationAnnotation setupAnnotation(Application system) {
			IdpaYamlSerializer<ApplicationAnnotation> serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);
			try {
				return serializer.readFromYamlInputStream(getClass().getResourceAsStream("/dvdstore-annotation.yml"));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
	};

	private final Application systemModel;
	private final ApplicationAnnotation annotation;

	/**
	 *
	 */
	private ContinuityModelTestInstance() {
		this.systemModel = setupSystemModel();
		this.annotation = setupAnnotation(systemModel);
	}

	protected abstract Application setupSystemModel();

	protected abstract ApplicationAnnotation setupAnnotation(Application system);

	/**
	 * Gets {@link #systemModel}.
	 *
	 * @return {@link #systemModel}
	 */
	public Application getSystemModel() {
		return this.systemModel;
	}

	/**
	 * Gets {@link #annotation}.
	 *
	 * @return {@link #annotation}
	 */
	public ApplicationAnnotation getAnnotation() {
		return this.annotation;
	}

}
