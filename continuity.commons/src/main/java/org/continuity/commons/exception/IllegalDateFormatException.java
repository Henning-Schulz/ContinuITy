package org.continuity.commons.exception;

import java.text.DateFormat;
import java.util.Date;

public class IllegalDateFormatException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 5216072575229940335L;

	public IllegalDateFormatException(String dateString, String dateFormat) {
		super("Cannot parse date '" + dateString + "'! The expected format is '" + dateFormat + "'.");
	}

	public IllegalDateFormatException(String dateString, DateFormat dateFormat) {
		this(dateString, dateFormat.format(new Date()));
	}

}
