package org.continuity.api.rest;

import java.util.HashMap;
import java.util.Map;

import org.continuity.api.rest.RestApi.JMeter.TestPlan;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Holds the REST API of all ContinuITy services. This does not conform the principle of
 * microservices, but allows us as a small number of developers to handle multiple microservice-like
 * services.
 *
 * @author Henning Schulz
 *
 */
public class RestApi {

	private RestApi() {
	}

	/**
	 * REST API of the frontend service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Frontend {

		public static final String SERVICE_NAME = "frontend";

		private Frontend() {
		}

		/**
		 * IDPA API of the frontend service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Idpa {

			public static final String ROOT = "/annotation";

			public static final RestEndpoint GET_APPLICATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_APPLICATION, RequestMethod.GET);

			public static final RestEndpoint GET_ANNOTATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_ANNOTATION, RequestMethod.GET);

			public static final RestEndpoint UPDATE_APPLICATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_APPLICATION, RequestMethod.POST);

			public static final RestEndpoint UPDATE_APP_FROM_OPEN_API_JSON = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_APP_FROM_OPEN_API_JSON, RequestMethod.POST);

			public static final RestEndpoint UPDATE_APP_FROM_OPEN_API_URL = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_APP_FROM_OPEN_API_URL, RequestMethod.POST);

			public static final RestEndpoint UPDATE_ANNOTATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_ANNOTATION, RequestMethod.POST);

			public static final RestEndpoint REPORT = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.REPORT, RequestMethod.GET);

			private Idpa() {
			}

			public static class Paths {

				public static final String GET_APPLICATION = "/{tag}/system";
				public static final String GET_ANNOTATION = "/{tag}/annotation";
				public static final String UPDATE_APPLICATION = "/{tag}/system";
				public static final String UPDATE_APP_FROM_OPEN_API_JSON = "/{tag}/openapi/{version}/json";
				public static final String UPDATE_APP_FROM_OPEN_API_URL = "/{tag}/openapi/{version}/url";
				public static final String UPDATE_ANNOTATION = "/{tag}/annotation";
				public static final String REPORT = "/report";

				private Paths() {
				}
			}

		}

		/**
		 * Loadtest API of the frontend service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Loadtest {

			public static final String ROOT = "/loadtest";

			public static final RestEndpoint CREATE_AND_EXECUTE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.CREATE_AND_EXECUTE, RequestMethod.POST);

			public static final RestEndpoint EXECUTE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.EXECUTE, RequestMethod.POST);

			public static final RestEndpoint CREATE_AND_GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.CREATE_AND_GET, RequestMethod.GET);

			public static final RestEndpoint REPORT = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.REPORT_PATH, RequestMethod.GET);


			private Loadtest() {
			}

			public static class Paths {

				public static final String CREATE_AND_EXECUTE = "/{type}/createandexecute";
				public static final String EXECUTE = "/{type}/execute";
				public static final String CREATE_AND_GET = "/{lt-type}/{wm-type}/model/{id}/create";
				public static final String REPORT_PATH = "/report";

				private Paths() {
				}
			}

		}

		/**
		 * Workload model API of the frontend service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class WorkloadModel {

			public static final String ROOT = "/workloadmodel";

			public static final RestEndpoint CREATE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.CREATE, RequestMethod.GET);

			public static final RestEndpoint WAIT = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.WAIT, RequestMethod.GET);

			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			private WorkloadModel() {
			}

			public static class Paths {

				public static final String CREATE = "/{type}/create";
				public static final String WAIT = "/wait/{type}/model/{id}";
				public static final String GET = "/get/{type}/model/{id}";

				private Paths() {
				}
			}

		}

	}

	/**
	 * REST API of the IDPA annotation service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class IdpaAnnotation {

		public static final String SERVICE_NAME = "idpa-annotation";

		private IdpaAnnotation() {
		}

		/**
		 * Annotation API of the IDPA annotation service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Annotation {

			public static final String ROOT = "/ann";

			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			public static final RestEndpoint GET_BASE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_BASE, RequestMethod.GET);

			public static final RestEndpoint UPDATE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE, RequestMethod.POST);

			private Annotation() {
			}

			public static class Paths {

				public static final String GET = "/{tag}/annotation";
				public static final String GET_BASE = "/{tag}/annotation/base";
				public static final String UPDATE = "/{tag}/annotation";

				private Paths() {
				}
			}
		}

		/**
		 * Dummy API of the IDPA annotation service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Dummy {

			public static final String ROOT = "/dummy/dvdstore";

			public static final RestEndpoint GET_APPLICATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_APPLICATION, RequestMethod.GET);

			public static final RestEndpoint GET_ANNOTATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_ANNOTATION, RequestMethod.GET);

			private Dummy() {
			}

			public static class Paths {

				public static final String GET_APPLICATION = "/annotation";
				public static final String GET_ANNOTATION = "/system";

				private Paths() {
				}
			}
		}

	}

	/**
	 * REST API of the IDPA application service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class IdpaApplication {

		public static final String SERVICE_NAME = "idpa-application";

		private IdpaApplication() {
		}

		/**
		 * Application API of the IDPA application service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Application {

			public static final String ROOT = "/system";

			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			public static final RestEndpoint GET_DELTA = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_DELTA, RequestMethod.GET);

			public static final RestEndpoint UPDATE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE, RequestMethod.POST);

			private Application() {
			}

			public static class Paths {

				public static final String GET = "/{tag}";
				public static final String GET_DELTA = "/{tag}/delta";
				public static final String UPDATE = "/{tag}";

				private Paths() {
				}
			}
		}

		/**
		 * OpenAPI API of the IDPA application service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class OpenApi {

			public static final String ROOT = "/openapi";

			public static final RestEndpoint UPDATE_FROM_JSON = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_FROM_JSON, RequestMethod.POST);

			public static final RestEndpoint UPDATE_FROM_URL = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_FROM_URL, RequestMethod.POST);

			private OpenApi() {
			}

			public static class Paths {

				public static final String UPDATE_FROM_JSON = "/{tag}/{version}/json";
				public static final String UPDATE_FROM_URL = "/{tag}/{version}/url";

				private Paths() {
				}
			}
		}

	}

	/**
	 * REST API of the JMeter service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class JMeter {

		public static final String SERVICE_NAME = "jmeter";

		private JMeter() {
		}

		/**
		 * Test plan API of the JMeter service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class TestPlan {

			public static final String ROOT = "/loadtest";

			public static final RestEndpoint CREATE_AND_GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.CREATE_AND_GET, RequestMethod.GET);

			private TestPlan() {
			}

			public static class Paths {

				public static final String CREATE_AND_GET = "/{type}/model/{id}/create";

				private Paths() {
				}
			}
		}

	}

	/**
	 * REST API of the Session Logs service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class SessionLogs {

		public static final String SERVICE_NAME = "session-logs";

		public static final String ROOT = "/";

		public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

		private SessionLogs() {
		}

		public static class Paths {

			public static final String GET = "/";

			private Paths() {
			}
		}

	}

	/**
	 * REST API of the WESSBAS service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Wessbas {

		public static final String SERVICE_NAME = "wessbas";

		private Wessbas() {
		}

		/**
		 * JMeter API of the WESSBAS service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class JMeter {

			public static final String ROOT = "/loadtest/jmeter";

			public static final RestEndpoint CREATE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.CREATE, RequestMethod.GET);

			private JMeter() {
			}

			public static class Paths {

				public static final String CREATE = "/{id}/create";

				private Paths() {
				}
			}

		}

		/**
		 * Workload model API of the WESSBAS service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Model {

			public static final String ROOT = "/model";

			public static final RestEndpoint OVERVIEW = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.OVERVIEW, RequestMethod.GET);

			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			public static final RestEndpoint REMOVE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.REMOVE, RequestMethod.DELETE);

			public static final RestEndpoint GET_APPLICATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_APPLICATION, RequestMethod.GET);

			public static final RestEndpoint GET_ANNOTATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_ANNOTATION, RequestMethod.GET);

			public static final RestEndpoint RESERVE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.RESERVE, RequestMethod.GET);

			private Model() {
			}

			public static class Paths {

				public static final String OVERVIEW = "/{id}";
				public static final String GET = "/{id}/workload";
				public static final String REMOVE = "/{id}";
				public static final String GET_APPLICATION = "/{id}/system";
				public static final String GET_ANNOTATION = "/{id}/annotation";
				public static final String RESERVE = "/{tag}/reserve";

				private Paths() {
				}
			}

		}

	}

	/**
	 * Generic, abstract REST endpoints. The implementing endpoints can be retrieved via the
	 * {@link Map#get(Object)} method, e.g., the JMeter {@link TestPlan#CREATE_AND_GET} via
	 * {@link Generic#GET_AND_CREATE_LOAD_TEST}<code>.get("jmeter")</code>.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Generic {

		public static final Map<String, RestEndpoint> GET_AND_CREATE_LOAD_TEST = new HashMap<>();

		public static final Map<String, RestEndpoint> GET_WORKLOAD_MODEL = new HashMap<>();
		public static final Map<String, RestEndpoint> RESERVE_WORKLOAD_MODEL = new HashMap<>();

		static {
			GET_AND_CREATE_LOAD_TEST.put("jmeter", JMeter.TestPlan.CREATE_AND_GET);

			GET_WORKLOAD_MODEL.put("wessbas", Wessbas.Model.GET);

			RESERVE_WORKLOAD_MODEL.put("wessbas", Wessbas.Model.RESERVE);
		}

		private Generic() {
		}

	}

}
