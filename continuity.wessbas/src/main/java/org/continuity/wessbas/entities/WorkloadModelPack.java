package org.continuity.wessbas.entities;

import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi.Wessbas;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelPack extends LinkExchangeModel {

	private static final String DEFAULT_MODEL_TYPE = "wessbas";

	public WorkloadModelPack(String hostname, String id, String tag) {
		setWorkloadType(DEFAULT_MODEL_TYPE);
		setWorkloadLink(hostname + Wessbas.Model.OVERVIEW.path(id));
		setApplicationLink(hostname + Wessbas.Model.GET_APPLICATION.path(id));
		setInitialAnnotationLink(hostname + Wessbas.Model.GET_ANNOTATION.path(id));
		setJmeterLink(hostname + Wessbas.JMeter.CREATE.path(id));

		setTag(tag);
	}

}
