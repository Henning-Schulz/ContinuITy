package org.idpa.application.model.entities;

import java.util.Date;

import org.continuity.commons.format.CommonFormats;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class SystemModelLink {

	@JsonProperty("system-model-link")
	private String modelLink;

	@JsonProperty("delta-link")
	private String deltaLink;

	private String tag;

	public SystemModelLink() {
	}

	public SystemModelLink(String applicationName, String tag, Date beforeDate) {
		this.modelLink = applicationName + "/system/" + tag;
		this.deltaLink = this.modelLink + "/delta?since=" + CommonFormats.DATE_FORMAT.format(beforeDate);
		this.tag = tag;
	}

	/**
	 * Gets {@link #modelLink}.
	 *
	 * @return {@link #modelLink}
	 */
	public String getModelLink() {
		return this.modelLink;
	}

	/**
	 * Sets {@link #modelLink}.
	 *
	 * @param modelLink
	 *            New value for {@link #modelLink}
	 */
	public void setModelLink(String modelLink) {
		this.modelLink = modelLink;
	}

	/**
	 * Gets {@link #deltaLink}.
	 *
	 * @return {@link #deltaLink}
	 */
	public String getDeltaLink() {
		return this.deltaLink;
	}

	/**
	 * Sets {@link #deltaLink}.
	 *
	 * @param deltaLink
	 *            New value for {@link #deltaLink}
	 */
	public void setDeltaLink(String deltaLink) {
		this.deltaLink = deltaLink;
	}

	/**
	 * Gets {@link #tag}.
	 *
	 * @return {@link #tag}
	 */
	public String getTag() {
		return this.tag;
	}

	/**
	 * Sets {@link #tag}.
	 *
	 * @param tag
	 *            New value for {@link #tag}
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

}
