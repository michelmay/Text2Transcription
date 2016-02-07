package de.upb.t2t.control.gui;

import de.upb.t2t.model.TranscriptionItem;

/**
 * An interface that provides functionality for GUI classes that allow for modifying the database in
 * order to present the user with feedback about said editing process.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public interface DbEditingUserFeedback extends UserFeedbackProvider {

	/**
	 * Should be called whenever a new transcription item has been added to the database.
	 *
	 * @param item
	 *            the item that was added.
	 */
	public void itemAdded(TranscriptionItem item);

	/**
	 * Should be called whenever a new transcription item has been updated.
	 *
	 * @param item
	 *            the item that was added.
	 */
	public void itemUpdated(TranscriptionItem item);

	/**
	 * Should be called whenever a new transcription item has been removed from the database.
	 *
	 * @param item
	 *            the item that was added.
	 */
	public void itemRemoved(TranscriptionItem item);
}
