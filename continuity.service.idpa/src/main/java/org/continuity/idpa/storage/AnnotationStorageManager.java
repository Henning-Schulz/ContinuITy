package org.continuity.idpa.storage;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.commons.idpa.AnnotationValidityChecker;
import org.continuity.idpa.Idpa;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.storage.IdpaStorage.IdpaEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Manages the annotations stored in an {@link IdpaStorage}.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationStorageManager implements IdpaStorageListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationStorageManager.class);

	private final IdpaStorage storage;

	@Autowired
	public AnnotationStorageManager(IdpaStorage storage) {
		this.storage = storage;
		this.storage.registerListener(this);
	}

	/**
	 * Retrieves the specified annotation if present.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @return A {@link ApplicationAnnotation}. If there is no annotation for the tag, {@code null} will
	 *         be returned.
	 * @throws IOException
	 */
	public ApplicationAnnotation read(String tag) throws IOException {
		return storage.readLatest(tag).getAnnotation();
	}

	/**
	 * Retrieves the specified annotation for a given timestamp if present.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param timestamp
	 *            The timestamp for which an application model is searched.
	 * @return A {@link ApplicationAnnotation}. If there is no annotation for the tag, {@code null}
	 *         will be returned.
	 * @throws IOException
	 */
	public ApplicationAnnotation read(String tag, Date timestamp) throws IOException {
		return storage.readLatestBefore(tag, timestamp).getAnnotation();
	}

	/**
	 * Updates the annotation stored with the specified tag. If the annotation is invalid with
	 * respect to the application model, it is rejected. It does <b>not</b> try to fix it. If you
	 * want to store an annotation that covers application parts that are not part of the current
	 * application model, please update the application model first.<br>
	 * Assumes a corresponding application model to be present.
	 *
	 * @param tag
	 * @param annotation
	 * @return A report holding information about the changes and if the new annotation is broken.
	 * @throws IOException
	 */
	public AnnotationValidityReport saveOrUpdate(String tag, Date timestamp, ApplicationAnnotation annotation) throws IOException {
		Idpa latest = storage.readLatestBefore(tag, timestamp);

		if (latest == null) {
			throw new IllegalStateException("There is no application model with tag " + tag);
		}

		AnnotationValidityChecker checker = new AnnotationValidityChecker(latest.getApplication());
		checker.checkAnnotation(annotation);
		AnnotationValidityReport report = checker.getReport();

		if (!report.isBreaking()) {
			storage.save(tag, timestamp, annotation);

			storage.unmarkAsBroken(tag, timestamp);
		}

		return report;
	}

	/**
	 * Returns whether the annotation with the specified tag is broken.
	 *
	 * @param tag
	 * @return {@code true} if it is broken or {@code false} otherwise.
	 */
	public boolean isBroken(String tag) {
		return storage.readLatest(tag).checkAdditionalFlag(IdpaStorage.FLAG_BROKEN);
	}

	/**
	 * {@inheritDoc} <br>
	 *
	 * Checks whether the annotations affected by the change are broken and marks them accordingly.
	 */
	@Override
	public void onApplicationChanged(String tag, Date timestamp) {
		Iterator<IdpaEntry> it = storage.iterate(tag).iterator();

		IdpaEntry curr = null;

		while (it.hasNext() && ((curr == null) || curr.getApplication().getTimestamp().after(timestamp))) {
			curr = it.next();
		}

		if ((curr != null)) {
			adjustBrokenMark(tag, curr);
		}

		while (it.hasNext() && (curr != null) && curr.getApplication().getTimestamp().equals(timestamp)) {
			curr = it.next();
			adjustBrokenMark(tag, curr);
		}
	}

	private boolean isBroken(Idpa idpa) {
		AnnotationValidityChecker checker = new AnnotationValidityChecker(idpa.getApplication());
		checker.checkAnnotation(idpa.getAnnotation());
		AnnotationValidityReport report = checker.getReport();

		return report.isBreaking();
	}

	private void adjustBrokenMark(String tag, Idpa idpa) {
		if (isBroken(idpa)) {
			try {
				storage.markAsBroken(tag, idpa.getTimestamp());
			} catch (IOException e) {
				LOGGER.error("Could not mark annotation " + tag + " (" + idpa.getTimestamp() + ") as broken!", e);
			}
		} else {
			try {
				storage.unmarkAsBroken(tag, idpa.getTimestamp());
			} catch (IOException e) {
				LOGGER.error("Could not unmark annotation " + tag + " (" + idpa.getTimestamp() + ") as broken!", e);
			}
		}
	}

	@Override
	public void onAnnotationChanged(String tag, Date timestamp) {
		// do nothing
	}

}
