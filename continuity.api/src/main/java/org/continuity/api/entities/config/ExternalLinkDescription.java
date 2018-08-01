package org.continuity.api.entities.config;

public class ExternalLinkDescription implements SourceDescription {

	private String link;

	@Override
	public Type getType() {
		return Type.EXTERNAL_LINK;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

}
