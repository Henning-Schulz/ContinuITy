package org.continuity.idpa.application.changes;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.continuity.idpa.application.entities.SystemChange;
import org.continuity.idpa.application.entities.SystemChangeReport;
import org.continuity.idpa.application.entities.SystemChangeType;

/**
 * @author Henning Schulz
 *
 */
public class SystemChangeReportBuilder {

	private Date beforeChange;
	private Date afterChange;

	private final Set<SystemChange> changes = new HashSet<>();

	private final Set<SystemChange> ignoredChanges = new HashSet<>();

	private final EnumSet<SystemChangeType> ignoredChangeTypes;

	public SystemChangeReportBuilder(EnumSet<SystemChangeType> ignoredChangeTypes, Date beforeChange, Date afterChange) {
		this.ignoredChangeTypes = ignoredChangeTypes;
		this.beforeChange = beforeChange;
		this.afterChange = afterChange;
	}

	public SystemChangeReportBuilder(EnumSet<SystemChangeType> ignoredChangeTypes, Date afterChange) {
		this(ignoredChangeTypes, new Date(0), afterChange);
	}

	public SystemChangeReportBuilder(Date beforeChange, Date afterChange) {
		this(EnumSet.noneOf(SystemChangeType.class), beforeChange, afterChange);
	}

	public void addChange(SystemChange violation) {
		if (!ignoredChangeTypes.contains(violation.getType())) {
			changes.add(violation);
		} else {
			ignoredChanges.add(violation);
		}
	}

	/**
	 * Sets {@link #beforeChange}.
	 * 
	 * @param beforeChange
	 *            New value for {@link #beforeChange}
	 */
	public void setBeforeChange(Date beforeChange) {
		this.beforeChange = beforeChange;
	}

	public SystemChangeReport buildReport() {
		return new SystemChangeReport(changes, ignoredChanges, beforeChange, afterChange);
	}

}
