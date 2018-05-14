package org.continuity.api.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import org.springframework.web.bind.annotation.RequestMethod;

public class RestEndpoint {

	private final String serviceName;

	private final String root;

	private final List<StringOrPar> elements;

	private final RequestMethod method;

	private RestEndpoint(String serviceName, String root, List<StringOrPar> elements, RequestMethod method) {
		this.serviceName = serviceName;
		this.root = root;
		this.elements = elements;
		this.method = method;
	}

	public static RestEndpoint of(String serviceName, String root, String path, RequestMethod method) {
		String[] pathElements = path.split("\\/");
		List<StringOrPar> elements = new ArrayList<>();

		for (String elem : pathElements) {
			if (elem.startsWith("{")) {
				elements.add(StringOrPar.of(PathPar.of(elem.substring(1, elem.length() - 1))));
			} else {
				elements.add(StringOrPar.of(elem));
			}
		}

		return new RestEndpoint(serviceName, root, elements, method);
	}

	public String genericPath() {
		return elements.stream().reduce(StringOrPar.of(root), StringOrPar::concat).toString();
	}

	public String path(Object... values) {
		List<Object> valueList = Arrays.asList(values);
		Collections.reverse(valueList);

		Stack<Object> valueStack = new Stack<>();
		valueStack.addAll(valueList);

		return elements.stream().reduce(StringOrPar.of(root), (a, b) -> StringOrPar.concatWithValues(a, b, valueStack)).toString();
	}

	public RequestBuilder requestUrl(Object... values) {
		return new RequestBuilder(serviceName, path(values));
	}

	public RequestMethod method() {
		return method;
	}

	public static class StringOrPar {

		private final String string;
		private final PathPar par;

		private StringOrPar(String string, PathPar par) {
			this.string = string;
			this.par = par;
		}

		public static StringOrPar of(String string) {
			return new StringOrPar(string, null);
		}

		public static StringOrPar of(PathPar par) {
			return new StringOrPar(null, par);
		}

		public static StringOrPar concat(StringOrPar first, StringOrPar second) {
			return StringOrPar.of(first + "/" + second);
		}

		/**
		 * Note: Assumes that the first element is always string!
		 *
		 * @param first
		 * @param second
		 * @param values
		 * @return
		 */
		public static StringOrPar concatWithValues(StringOrPar first, StringOrPar second, Stack<Object> values) {
			if ("/".equals(second.toString()) || "".equals(second.toString())) {
				return first;
			}

			if (second.isPar()) {
				return StringOrPar.of(first + "/" + values.pop());
			} else {
				return StringOrPar.of(first + "/" + second);
			}
		}

		public boolean isString() {
			return string != null;
		}

		public boolean isPar() {
			return par != null;
		}

		public String getString() {
			return string;
		}

		public PathPar getPar() {
			return par;
		}

		public String asValue(Object value) {
			if (isPar()) {
				return Objects.toString(value);
			} else {
				return toString();
			}
		}

		@Override
		public String toString() {
			if (isPar()) {
				return par.generic();
			} else {
				return string;
			}
		}

	}

}
