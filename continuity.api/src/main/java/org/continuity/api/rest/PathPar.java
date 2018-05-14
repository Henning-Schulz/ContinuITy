package org.continuity.api.rest;

public class PathPar {

	private final String name;

	private PathPar(String name) {
		this.name = name;
	}

	public static PathPar of(String name) {
		return new PathPar(name);
	}

	public String generic() {
		return "{" + name + "}";
	}

}
