package org.continuity.idpa.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.continuity.api.entities.ApiFormats;
import org.continuity.idpa.Idpa;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores IDPAs in different versions in a folder structure. For versioning, the date when a model
 * was created is used.
 *
 * @author Henning Schulz
 *
 */
public class IdpaStorage {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdpaStorage.class);

	private static final String APPLICATION_FILE_NAME = "application.yml";
	private static final String ANNOTATION_FILE_NAME = "application.yml";

	private static final DateFormat DATE_FORMAT = ApiFormats.DATE_FORMAT;

	private final IdpaYamlSerializer<Application> appSerializer;
	private final IdpaYamlSerializer<ApplicationAnnotation> annSerializer;

	private final Path storagePath;

	public IdpaStorage(String storagePath) {
		this(Paths.get(storagePath));
	}

	public IdpaStorage(Path storagePath) {
		this(storagePath, new IdpaYamlSerializer<>(Application.class), new IdpaYamlSerializer<>(ApplicationAnnotation.class));
	}

	public IdpaStorage(Path storagePath, IdpaYamlSerializer<Application> appSerializer, IdpaYamlSerializer<ApplicationAnnotation> annSerializer) {
		this.storagePath = storagePath;
		this.appSerializer = appSerializer;
		this.annSerializer = annSerializer;

		LOGGER.info("Using storage path {}.", storagePath.toAbsolutePath());
	}

	/**
	 * Stores the specified application model with the specified tag.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param application
	 *            The application model.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public void save(String tag, Application application) throws IOException {
		Path path = getDirPath(tag, application.getTimestamp()).resolve(APPLICATION_FILE_NAME);
		appSerializer.writeToYaml(application, path);

		LOGGER.debug("Wrote application model to {}.", path);
	}

	/**
	 * Stores the specified annoation with the specified tag.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param application
	 *            The application model.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public void save(String tag, Date timestamp, ApplicationAnnotation annotation) throws IOException {
		Path path = getDirPath(tag, timestamp).resolve(APPLICATION_FILE_NAME);
		annSerializer.writeToYaml(annotation, path);

		LOGGER.debug("Wrote application model to {}.", path);
	}

	/**
	 * Reads the latest IDPA.
	 *
	 * @param tag
	 *            The tag of the IDPA.
	 * @return The latest IDPA.
	 */
	public Idpa readLatest(String tag) {
		for (IdpaEntry entry : iterate(tag)) {
			return entry;
		}

		return null;
	}

	/**
	 * Reads the latest IDPA that is older than the specified date.
	 *
	 * @param tag
	 *            The tag of the IDPA.
	 * @param date
	 *            The date to compare with.
	 * @return An IDPA.
	 * @throws IOException
	 *             If an error during reading the IDPA occurs.
	 */
	public Idpa readLatestBefore(String tag, Date date) {
		for (IdpaEntry entry : iterate(tag)) {
			if (!date.before(entry.getDate())) {
				return entry;
			}
		}

		return null;
	}

	/**
	 * Reads the oldest application model that is newer than the specified date.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param date
	 *            The date to compare with.
	 * @return A application model.
	 * @throws IOException
	 *             If an error during reading the application model occurs.
	 */
	public Application readOldestAfter(String tag, Date date) {
		Application next = null;

		for (IdpaEntry entry : iterate(tag)) {
			if (!date.before(entry.getDate())) {
				return next;
			}

			next = entry.getApplication();
		}

		return next;
	}

	/**
	 * Updates the timestamp of a application model. The new date is expected to be before the old
	 * date.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param oldTimestamp
	 *            The old timestamp.
	 * @param newTimestamp
	 *            The new timestamp.
	 *
	 * @throws IllegalArgumentException
	 *             If {@code newTimestamp} is after {@link oldTimestamp} or if there is no
	 *             application model at {@link oldTimestamp}.
	 * @throws IOException
	 *             If something goes wrong during changing the timestamp.
	 */
	public void updateApplicationChange(String tag, Date oldTimestamp, Date newTimestamp) throws IllegalArgumentException, IOException {
		if (!newTimestamp.before(oldTimestamp)) {
			throw new IllegalArgumentException("Cannot update application model with tag " + tag + " to date " + newTimestamp + "! This date is not before the original one: " + oldTimestamp);
		}

		Idpa idpa = readLatestBefore(tag, oldTimestamp);
		Application application = idpa.getApplication();

		if (!oldTimestamp.equals(application.getTimestamp())) {
			throw new IllegalArgumentException("There is no application model with tag " + tag + " at date " + oldTimestamp + "!");
		}

		application.setTimestamp(newTimestamp);
		save(tag, application);
		save(tag, newTimestamp, idpa.getAnnotation());
		delete(tag, oldTimestamp);
	}

	private boolean delete(String tag, Date date) throws NotDirectoryException {
		return getDirPath(tag).resolve(DATE_FORMAT.format(date)).resolve(APPLICATION_FILE_NAME).toFile().delete();
	}

	private Path getDirPath(String tag) throws NotDirectoryException {
		Path dirPath = storagePath.resolve(tag);
		checkAndCreateDirs(dirPath);
		return dirPath;
	}

	private Path getDirPath(String tag, Date timestamp) throws NotDirectoryException {
		Path dirPath = getDirPath(tag).resolve(DATE_FORMAT.format(timestamp));
		checkAndCreateDirs(dirPath);
		return dirPath;
	}

	private void checkAndCreateDirs(Path dirPath) throws NotDirectoryException {
		File dir = dirPath.toFile();

		if (dir.exists() && !dir.isDirectory()) {
			LOGGER.error("{} is not a directory!", dir.getAbsolutePath());
			throw new NotDirectoryException(dir.getAbsolutePath());
		}

		dir.mkdirs();
	}

	/**
	 * Returns an {@link Iterable} allowing to iterate over all IDPAs in combination
	 * with the created date. The models are traversed in ascending order. That is, the newest model
	 * comes first.
	 *
	 * @param tag
	 *            The tag of the application models to be iterated.
	 * @return An iterator.
	 */
	public Iterable<IdpaEntry> iterate(String tag) {
		return new ApplicationIterable(tag);
	}

	private class ApplicationIterable implements Iterable<IdpaEntry> {

		private final String tag;

		public ApplicationIterable(String tag) {
			this.tag = tag;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator<IdpaEntry> iterator() {
			try {
				return new ApplicationIterator(tag);
			} catch (NotDirectoryException e) {
				LOGGER.error("Cannot iterate over application models of tag {}!", tag);
				return null;
			}
		}

	}

	private class ApplicationIterator implements Iterator<IdpaEntry> {

		private final String tag;
		private final List<Date> dates;
		private final Iterator<Date> datesIterator;

		public ApplicationIterator(String tag) throws NotDirectoryException {
			this.tag = tag;
			Path dir = getDirPath(tag);
			this.dates = Arrays.stream(dir.toFile().list()).map(this::extractDate).collect(Collectors.toList());
			Collections.sort(this.dates, Collections.reverseOrder());
			this.datesIterator = dates.iterator();
		}

		private Date extractDate(String dateString) {
			try {
				return DATE_FORMAT.parse(dateString);
			} catch (ParseException e) {
				LOGGER.error("Could not parse date {}! Returning 1990/01/01", dateString);
				e.printStackTrace();
			}

			return new Date(0);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return datesIterator.hasNext();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IdpaEntry next() {
			Date date = datesIterator.next();
			String folder = DATE_FORMAT.format(date);

			try {
				return IdpaEntry.of(IdpaStorage.this, date, getDirPath(tag).resolve(folder));
			} catch (NotDirectoryException e) {
				LOGGER.error("Could not read application {} for tag {}! Returning null.", folder, tag);
				LOGGER.error("Expetion: ", e);
				return IdpaEntry.of(IdpaStorage.this, date, null);
			}
		}

	}

	/**
	 * Holds a application model in combination with the date when it was created.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class IdpaEntry extends Idpa {

		private final IdpaYamlSerializer<Application> appSerializer;

		private final IdpaYamlSerializer<ApplicationAnnotation> annSerializer;

		private final Path path;
		private final Date date;

		private IdpaEntry(IdpaStorage storage, Date date, Path path) {
			this.path = path;
			this.date = date;

			this.appSerializer = storage.appSerializer;
			this.annSerializer = storage.annSerializer;
		}

		private static IdpaEntry of(IdpaStorage storage, Date date, Path path) {
			return new IdpaEntry(storage, date, path);
		}

		public Date getDate() {
			return this.date;
		}

		@Override
		public Application getApplication() {
			if (path == null) {
				return null;
			}

			try {
				return appSerializer.readFromYaml(path.resolve(APPLICATION_FILE_NAME));
			} catch (IOException e) {
				LOGGER.error("Could not read application model from {}! Returning null.", path);
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public ApplicationAnnotation getAnnotation() {
			if ((path == null) || !path.resolve(ANNOTATION_FILE_NAME).toFile().exists()) {
				return null;
			}

			try {
				return annSerializer.readFromYaml(path.resolve(ANNOTATION_FILE_NAME));
			} catch (IOException e) {
				LOGGER.error("Could not read annotation from {}! Returning null.", path);
				e.printStackTrace();
				return null;
			}
		}

	}

}
