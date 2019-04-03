package org.continuity.session.logs.converter;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.continuity.api.entities.ApiFormats;
import org.continuity.api.rest.RestApi.IdpaApplication;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.session.logs.entities.RowObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;


/**
 * Class for converting a list of RowObjects into session logs.
 *
 * @author Alper Hidiroglu
 *
 */
public class SessionConverterCSVData {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionConverterCSVData.class);

	private final RestTemplate restTemplate;

	public SessionConverterCSVData(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	/**
	 * Creates session logs from CSV data.
	 * @param dataList
	 * @return
	 */
	public String createSessionLogsFromCSV(List<RowObject> dataList, String tag) {
		Application application = retrieveApplicationModel(tag);

		if (application != null) {
			LOGGER.info("Updating the CSV rows from the application for tag {}.", tag);
			dataList = updateRowsFromApplication(dataList, application);
		}

		LinkedHashMap<String, List<RowObject>> map = processSessions(dataList);
		boolean first = true;
		String sessionLogs = "";
		for (Entry<String, List<RowObject>> entry : map.entrySet()) {
			boolean empty = true;
			StringBuffer buffer = new StringBuffer();
			buffer.append(entry.getKey()).append(";");
			for (RowObject rowObject : entry.getValue()) {
				appendRowObjectInfo(buffer, rowObject);
				empty = false;
			}

			if (!empty) {
				if (first) {
					first = false;
				} else {
					sessionLogs += "\n";
				}
				sessionLogs += buffer.toString();
			}
		}
		return sessionLogs;
	}

	private List<RowObject> updateRowsFromApplication(List<RowObject> dataList, Application application) {
		RequestUriMapper mapper = new RequestUriMapper(application);
		List<RowObject> updatedData = new ArrayList<>();

		for (RowObject row : dataList) {
			String path = row.getRequestURL();

			if (path.startsWith("http")) {
				int pathStart = path.indexOf("/", 8);
				path = path.substring(pathStart);
			}

			HttpEndpoint endpoint = mapper.map(path, row.getMethod());

			if (endpoint != null) {
				row.setBusinessTransaction(endpoint.getId());
				row.setRequestURL(endpoint.getPath());

				updatedData.add(row);
			}
		}

		return updatedData;
	}

	protected Application retrieveApplicationModel(String tag) {
		if (tag == null) {
			LOGGER.warn("Cannot retrieve the application model for naming the Session Logs. The tag is null!");
			return null;
		}

		try {
			return restTemplate.getForObject(IdpaApplication.Application.GET.requestUrl(tag).get(), Application.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.warn("Received error status code when asking for system model with tag " + tag, e);
			return null;
		}
	}

	/**
	 * Creates sessions from the available data in RowObjects.
	 *
	 * @return
	 */
	public LinkedHashMap<String, List<RowObject>> processSessions(List<RowObject> dataList) {
		LinkedHashMap<String, List<RowObject>> sessions = null;
		if(!(dataList.get(0).getSessionID() == null)) {
			sessions = extractSessions(dataList);
		} else {
			if(!(dataList.get(0).getUserName() == null)) {
				sessions = calculateSessionsWithUserNames(dataList);
			} else {
				sessions = calculateSessions(dataList);
			}
		}
		return sessions;
	}

	/**
	 * If dataset contains session identifiers, extract sessions.
	 * Sorts requests for each session.
	 * @param dataList
	 * @return
	 */
	private LinkedHashMap<String, List<RowObject>> extractSessions(List<RowObject> dataList) {
		LinkedHashMap<String, List<RowObject>> sessions = new LinkedHashMap<>();

		for(int i = 0; i < dataList.size(); i++) {
			String sessionID = dataList.get(i).getSessionID();
			if (sessions.containsKey(sessionID)) {
				List<RowObject> existingList = sessions.get(sessionID);
				existingList.add(dataList.get(i));
				sessions.put(sessionID, existingList);
			} else {
				List<RowObject> newList = new LinkedList<RowObject>();
				newList.add(dataList.get(i));
				sessions.put(sessionID, newList);
			}
		}
		for (Entry<String, List<RowObject>> entry : sessions.entrySet()) {
			String sessionID = entry.getKey();
			List<RowObject> rowObjectList = entry.getValue();
			sortRowObjects(rowObjectList);
			sessions.put(sessionID, rowObjectList);
		}
		return sessions;
	}

	/**
	 * Sorts RowObjects in list.
	 * @param rowObjects
	 */
	private void sortRowObjects(List<RowObject> rowObjects) {
		rowObjects.sort((RowObject ro1, RowObject ro2) -> {
			long startTimeRo1 = Long.parseLong(ro1.getRequestStartTime());
			long startTimeRo2 = Long.parseLong(ro2.getRequestStartTime());
			int startTimeComparison = Long.compare(startTimeRo1, startTimeRo2);
			if (startTimeComparison != 0) {
				return startTimeComparison;
			} else {
				long durationRo1 = Long.parseLong(ro1.getRequestEndTime()) - startTimeRo1;
				long durationRo2 = Long.parseLong(ro2.getRequestEndTime()) - startTimeRo2;
				return Double.compare(durationRo1, durationRo2);
			}
		});
	}

	/**
	 * If dataset has no session identifiers, calculate them with the help of user names and request start times.
	 * @param dataList
	 * @return
	 */
	private LinkedHashMap<String, List<RowObject>> calculateSessionsWithUserNames(List<RowObject> dataList) {
		LinkedHashMap<String, List<RowObject>> sessions = new LinkedHashMap<>();

		DateFormat dateFormat = ApiFormats.DATE_FORMAT;

		String currentUserName = "";

		String currentSession = "";

		long startOfRandomNumber = 1000000000000000L;

		ArrayList<Long> listOfRandomNumbers = new ArrayList<Long>();
		for(long i = 0; i < dataList.size(); i++) {
			listOfRandomNumbers.add(startOfRandomNumber);
			startOfRandomNumber++;
		}
		Collections.shuffle(listOfRandomNumbers);

		for (int i = 0; i < dataList.size(); i++) {
			String userName = dataList.get(i).getUserName();
			if (userName.equals(currentUserName)) {
				Date date1 = null;
				Date date2 = null;
				try {
					date1 = dateFormat.parse(dataList.get(i).getRequestStartTime());
					date2 = dateFormat.parse(dataList.get(i - 1).getRequestStartTime());
				} catch (ParseException e) {
					e.printStackTrace();
				}
				long time1 = date1.getTime();
				long time2 = date2.getTime();
				long difference = time1 - time2;
				long minutes = TimeUnit.MILLISECONDS.toMinutes(difference);
				if (minutes > 30) {
					String newSession = Long.toString(listOfRandomNumbers.get(i));
					currentSession = newSession;
					LinkedList<RowObject> newList = new LinkedList<RowObject>();
					newList.add(dataList.get(i));
					sessions.put(newSession, newList);
				} else {
					List<RowObject> currentList = sessions.get(currentSession);
					currentList.add(dataList.get(i));
					sessions.put(currentSession, currentList);
				}
			} else {
				currentUserName = userName;
				String newSession = Long.toString(listOfRandomNumbers.get(i));
				currentSession = newSession;
				LinkedList<RowObject> newList = new LinkedList<RowObject>();
				newList.add(dataList.get(i));
				sessions.put(newSession, newList);
			}
		}
		return sessions;

	}

	/**
	 * If dataset has no session identifiers, calculate them with the help of request start times.
	 * @param dataList
	 * @return
	 */
	private LinkedHashMap<String, List<RowObject>> calculateSessions(List<RowObject> dataList) {
		LinkedHashMap<String, List<RowObject>> sessions = new LinkedHashMap<>();

		DateFormat dateFormat = ApiFormats.DATE_FORMAT;

		String currentSession = "";

		long startOfRandomNumber = 1000000000000000L;

		ArrayList<Long> listOfRandomNumbers = new ArrayList<Long>();
		for(long i = 0; i < dataList.size(); i++) {
			listOfRandomNumbers.add(startOfRandomNumber);
			startOfRandomNumber++;
		}
		Collections.shuffle(listOfRandomNumbers);

		for (int i = 0; i < dataList.size(); i++) {
			if(i == 0) {
				String newSession = Long.toString(listOfRandomNumbers.get(i));
				currentSession = newSession;
				LinkedList<RowObject> newList = new LinkedList<RowObject>();
				newList.add(dataList.get(i));
				sessions.put(newSession, newList);
			} else {
				Date date1 = null;
				Date date2 = null;
				try {
					date1 = dateFormat.parse(dataList.get(i).getRequestStartTime());
					date2 = dateFormat.parse(dataList.get(i - 1).getRequestStartTime());
				} catch (ParseException e) {
					e.printStackTrace();
				}
				long time1 = date1.getTime();
				long time2 = date2.getTime();
				long difference = time1 - time2;
				long minutes = TimeUnit.MILLISECONDS.toMinutes(difference);
				if (minutes > 30) {
					String newSession = Long.toString(listOfRandomNumbers.get(i));
					currentSession = newSession;
					LinkedList<RowObject> newList = new LinkedList<RowObject>();
					newList.add(dataList.get(i));
					sessions.put(newSession, newList);
				} else {
					List<RowObject> currentList = sessions.get(currentSession);
					currentList.add(dataList.get(i));
					sessions.put(currentSession, currentList);
				}
			}
		}
		return sessions;
	}

	/**
	 * Appends RowObject infos to the String buffer.
	 *
	 * @param buffer
	 * @param rowObject
	 */
	private void appendRowObjectInfo(StringBuffer buffer, RowObject rowObject) {
		DateFormat dateFormat = ApiFormats.DATE_FORMAT;

		Date startDate = null;
		Date endDate = null;
		long endTime = 0L;
		try {
			startDate = dateFormat.parse(rowObject.getRequestStartTime());
			if (!(rowObject.getRequestEndTime() == null)) {
				endDate = dateFormat.parse(rowObject.getRequestEndTime());
				endTime = endDate.getTime();
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		long startTime = startDate.getTime();
		long startMicros = TimeUnit.MILLISECONDS.toNanos(startTime);
		long endMicros = TimeUnit.MILLISECONDS.toNanos(endTime);


		if (!(rowObject.getBusinessTransaction() == null)) {
			buffer.append("\"").append(rowObject.getBusinessTransaction()).append("\":");
		} else {
			buffer.append("\"").append(rowObject.getRequestURL()).append("\":");
		}
		buffer.append(Long.toString(startMicros)).append(":");
		if (!(rowObject.getRequestEndTime() == null)) {
			buffer.append(Long.toString(endMicros)).append(":");
		} else {
			buffer.append(Long.toString(startMicros)).append(":");
		}

		appendHTTPInfo(buffer, rowObject);
	}

	/**
	 * Appends HTTP infos to the String buffer.
	 *
	 * Sets dummy values when required information is not available in the data
	 * point.
	 *
	 * @param buffer
	 * @param rowObject
	 */
	private void appendHTTPInfo(StringBuffer buffer, RowObject rowObject) {
		if (!(rowObject.getRequestURL() == null)) {
			buffer.append(rowObject.getRequestURL()).append(":");
		} else {
			buffer.append("/").append(rowObject.getBusinessTransaction()).append(":");
		}

		if (!(rowObject.getPort() == null)) {
			buffer.append(rowObject.getPort()).append(":");
		} else {
			buffer.append("8080").append(":");
		}

		if (!(rowObject.getHostIP() == null)) {
			buffer.append(rowObject.getHostIP()).append(":");
		} else {
			buffer.append("127.0.0.1").append(":");
		}

		if (!(rowObject.getProtocol() == null)) {
			buffer.append(rowObject.getProtocol()).append(":");
		} else {
			buffer.append("HTTP/1.1").append(":");
		}

		if (!(rowObject.getMethod() == null)) {
			buffer.append(rowObject.getMethod()).append(":");
		} else {
			buffer.append("GET").append(":");
		}

		if (!(rowObject.getParameter() == null)) {
			buffer.append(rowObject.getParameter()).append(":");
		} else {
			buffer.append("<no-query-string>").append(":");
		}

		if (!(rowObject.getEncoding() == null)) {
			buffer.append(rowObject.getEncoding()).append(";");
		} else {
			buffer.append("UTF-8").append(";");
		}
	}
}
