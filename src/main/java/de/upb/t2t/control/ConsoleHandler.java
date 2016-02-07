package de.upb.t2t.control;

import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * This class is an almost one-to-one copy from <a href="http://stackoverflow.com/a/2906222">this
 * page</a>. Its sole purpose is to redirect the default logging output into the System.out stream.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class ConsoleHandler extends Handler {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */

	/* Constructors */

	/* Object Methods */
	/**
	 * Does nothing.
	 */
	@Override
	public void close() throws SecurityException {
		// Do nothing.
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void flush() {
		// Do nothing.
	}

	/**
	 * Writes messages of {@link Level#WARNING} or higher into the {@link System#err} and all others
	 * into the {@link System#out} stream.
	 */
	@Override
	public void publish(LogRecord record) {
		if (getFormatter() == null) {
			setFormatter(new SimpleFormatter());
		}

		try {
			String message = getFormatter().format(record);
			if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
				System.err.write(message.getBytes());
			} else {
				System.out.write(message.getBytes());
			}
		} catch (Exception exception) {
			reportError(null, exception, ErrorManager.FORMAT_FAILURE);
		}
	}

	/* Getters and Setters */
}
