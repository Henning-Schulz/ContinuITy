package org.continuity.api.rest;

import org.springframework.web.bind.annotation.RequestMethod;

public class RestApi {

	private RestApi() {
	}

	public static class Frontend {

		public static final String SERVICE_NAME = "frontend";

		private Frontend() {
		}

		public static class Annotation {

			public static final String ROOT = "/annotation";

			public static final String GET_APPLICATION_PATH = "/{tag}/system";
			public static final RestEndpoint GET_APPLICATION = RestEndpoint.of(SERVICE_NAME, ROOT, GET_APPLICATION_PATH, RequestMethod.GET);

			public static final String GET_ANNOTATION_PATH = "/{tag}/annotation";
			public static final RestEndpoint GET_ANNOTATION = RestEndpoint.of(SERVICE_NAME, ROOT, GET_ANNOTATION_PATH, RequestMethod.GET);

			private Annotation() {
			}

		}

	}

	public static class IdpaAnnotation {

		public static final String SERVICE_NAME = "idpa-annotation";

		private IdpaAnnotation() {
		}

		public static class Annotation {

			public static final String ROOT = "/ann";

			public static final String GET_PATH = "/{tag}/annotation";
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, GET_PATH, RequestMethod.GET);

			public static final String GET_BASE_PATH = "/{tag}/annotation/base";
			public static final RestEndpoint GET_BASE = RestEndpoint.of(SERVICE_NAME, ROOT, GET_BASE_PATH, RequestMethod.GET);

			public static final String UPDATE_PATH = "/{tag}/annotation";
			public static final RestEndpoint UPDATE = RestEndpoint.of(SERVICE_NAME, ROOT, UPDATE_PATH, RequestMethod.POST);

			private Annotation() {
			}
		}

		public static class Dummy {

			public static final String ROOT = "/dummy/dvdstore";

			public static final String GET_APPLICATION_PATH = "/annotation";
			public static final RestEndpoint GET_APPLICATION = RestEndpoint.of(SERVICE_NAME, ROOT, GET_APPLICATION_PATH, RequestMethod.GET);

			public static final String GET_ANNOTATION_PATH = "/system";
			public static final RestEndpoint GET_ANNOTATION_BASE = RestEndpoint.of(SERVICE_NAME, ROOT, GET_ANNOTATION_PATH, RequestMethod.GET);

			private Dummy() {
			}
		}

	}

	public static class IdpaApplication {

		public static final String SERVICE_NAME = "idpa-application";

		private IdpaApplication() {
		}

		public static class Application {

			public static final String ROOT = "/system";

			public static final String GET_PATH = "/{tag}";
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, GET_PATH, RequestMethod.GET);

			public static final String GET_DELTA_PATH = "/{tag}/delta";
			public static final RestEndpoint GET_DELTA = RestEndpoint.of(SERVICE_NAME, ROOT, GET_PATH, RequestMethod.GET);

			public static final String POST_PATH = "/{tag}";
			public static final RestEndpoint POST = RestEndpoint.of(SERVICE_NAME, ROOT, GET_PATH, RequestMethod.POST);

			private Application() {
			}
		}

		public static class OpenApi {

			public static final String ROOT = "/openapi";

			public static final String UPDATE_FROM_JSON_PATH = "/{tag}/{version}/json";
			public static final RestEndpoint UPDATE_FROM_JSON = RestEndpoint.of(SERVICE_NAME, ROOT, UPDATE_FROM_JSON_PATH, RequestMethod.POST);

			public static final String UPDATE_FROM_URL_PATH = "/{tag}/{version}/url";
			public static final RestEndpoint UPDATE_FROM_URL = RestEndpoint.of(SERVICE_NAME, ROOT, UPDATE_FROM_URL_PATH, RequestMethod.POST);

			private OpenApi() {
			}
		}

	}

	public static class JMeter {

		public static final String SERVICE_NAME = "jmeter";

		private JMeter() {
		}

		public static class TestPlan {

			public static final String ROOT = "/loadtest";

			public static final String CREATE_AND_GET_PATH = "/{type}/model/{id}/create";
			public static final RestEndpoint CREATE_AND_GET = RestEndpoint.of(SERVICE_NAME, ROOT, CREATE_AND_GET_PATH, RequestMethod.GET);

			private TestPlan() {
			}
		}

	}

	public static class SessionLogs {

		public static final String SERVICE_NAME = "session-logs";

		public static final String ROOT = "/";

		public static final String GET_PATH = "/";
		public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, GET_PATH, RequestMethod.GET);

		private SessionLogs() {
		}

	}

	public static class Wessbas {

		public static final String SERVICE_NAME = "wessbas";

		private Wessbas() {
		}

		public static class JMeter {

			public static final String ROOT = "/loadtest/jmeter";

			public static final String CREATE_PATH = "/{id}/create";
			public static final RestEndpoint CREATE = RestEndpoint.of(SERVICE_NAME, ROOT, CREATE_PATH, RequestMethod.GET);

			private JMeter() {
			}

		}

		public static class Model {

			public static final String ROOT = "/model";

			public static final String OVERVIEW_PATH = "/{id}";
			public static final RestEndpoint OVERVIEW = RestEndpoint.of(SERVICE_NAME, ROOT, OVERVIEW_PATH, RequestMethod.GET);

			public static final String GET_PATH = "/{id}/workload";
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, GET_PATH, RequestMethod.GET);

			public static final String REMOVE_PATH = "/{id}";
			public static final RestEndpoint REMOVE = RestEndpoint.of(SERVICE_NAME, ROOT, REMOVE_PATH, RequestMethod.DELETE);

			public static final String GET_APPLICATION_PATH = "/{id}/system";
			public static final RestEndpoint GET_APPLICATION = RestEndpoint.of(SERVICE_NAME, ROOT, GET_APPLICATION_PATH, RequestMethod.GET);

			public static final String GET_ANNOTATION_PATH = "/{id}/annotation";
			public static final RestEndpoint GET_ANNOTATION = RestEndpoint.of(SERVICE_NAME, ROOT, GET_ANNOTATION_PATH, RequestMethod.GET);

			private Model() {
			}

		}

	}

}
