package org.continuity.workload.annotation.validation;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.HttpParameter;
import org.continuity.annotation.dsl.system.HttpParameterType;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.test.ContinuityModelTestInstance;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Henning Schulz
 *
 */
public enum AnnotationValidityTestInstance {

	FIRST("http://first/") {
		@Override
		public SystemModel getSystemModel() {
			return ContinuityModelTestInstance.SIMPLE.getSystemModel();
		}

		@Override
		public SystemAnnotation getAnnotation() {
			return ContinuityModelTestInstance.SIMPLE.getAnnotation();
		}
	},
	SECOND("http://second/") {
		@Override
		public SystemModel getSystemModel() {
			SystemModel system = new SystemModel();

			HttpInterface interf = new HttpInterface();
			interf.setDomain("mydomain");
			interf.setId("login");

			HttpParameter param = new HttpParameter();
			param.setId("user");
			param.setParameterType(HttpParameterType.REQ_PARAM);
			interf.getParameters().add(param);

			system.addInterface(interf);

			interf = new HttpInterface();
			interf.setDomain("mydomain");
			interf.setId("logout");
			system.addInterface(interf);

			return system;
		}

		@Override
		public SystemAnnotation getAnnotation() {
			return ContinuityModelTestInstance.SIMPLE.getAnnotation();
		}
	},
	THIRD("http://third/") {
		@Override
		public SystemModel getSystemModel() {
			SystemModel system = new SystemModel();
			HttpInterface interf = new HttpInterface();
			interf.setDomain("mydomain");
			interf.setId("logout");
			system.addInterface(interf);

			return system;
		}

		@Override
		public SystemAnnotation getAnnotation() {
			return ContinuityModelTestInstance.SIMPLE.getAnnotation();
		}
	};

	private final String link;

	private AnnotationValidityTestInstance(String link) {
		this.link = link;
	}

	protected abstract SystemModel getSystemModel();

	protected abstract SystemAnnotation getAnnotation();

	public ResponseEntity<SystemModel> getSystemEntity() {
		return new ResponseEntity<>(getSystemModel(), HttpStatus.OK);
	}

	public ResponseEntity<SystemAnnotation> getAnnotationEntity() {
		return new ResponseEntity<>(getAnnotation(), HttpStatus.OK);
	}

	public String getSystemLink() {
		return link + "system";
	}

	public String getAnnotationLink() {
		return link + "annotation";
	}

}
