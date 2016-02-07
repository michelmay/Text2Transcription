/**
 *
 */
package de.upb.t2t.control.database.editing;

import de.upb.t2t.control.database.AbstractSQLiteBridge;
import de.upb.t2t.model.DatabaseEntry;
import de.upb.t2t.model.TranscriptionItem;
import de.upb.t2t.model.TranscriptionType;
import de.upb.t2t.model.Variety;
import de.upb.t2t.model.WordClass;

/**
 * An abstract class that encapsulates database editing functionality. Note that its functions are
 * identical to those of the {@link AbstractSQLiteBridge} and for any offline implementation of this
 * class, they will probably simply function as wrappers of these. However, since e.g. the
 * {@link AddTranscriptionPane} may be used in an online scenario, too, other sub-classes may add
 * further capabilities.
 *
 * @author Michel May (michel-may@gmx.de)
 * @see AddTranscriptionPane
 * @see AbstractSQLiteBridge
 */
public abstract class AbstractDatabaseEditingHandler {

	/**
	 * A convenience wrapper that simply calls
	 * <code>AbstractSQLiteBridge.getInstance().queryDatabase(lemma)</code>.
	 *
	 * @param lemma
	 *            the lemma to query.
	 * @return the corresponding database entry.
	 * @throws Exception
	 *             if {@link AbstractSQLiteBridge#queryDatabase(String)} throws any.
	 */
	public DatabaseEntry queryDatabase(String lemma) throws Exception {
		return AbstractSQLiteBridge.getInstance().queryDatabase(lemma);
	}

	/**
	 * Adds a transcription item to the database.
	 *
	 * @param lemma
	 *            the transcription's corresponding lemma.
	 * @param wordClass
	 *            the transcription's corresponding word class.
	 * @param variety
	 *            the transcription's corresponding variety.
	 * @param transType
	 *            the transcription's corresponding type.
	 * @param transcription
	 *            the phonetic transcription <code>String</code>.
	 * @return the {@link TranscriptionItem} object as retrieved from the database after adding the
	 *         given data.
	 * @throws Exception
	 */
	public abstract TranscriptionItem addTranscription(String lemma, WordClass wordClass, Variety variety,
			TranscriptionType transType, String transcription) throws Exception;

	/**
	 * Updates the <code>String</code> of a lemma with the given ID.
	 *
	 * @param lemmaID
	 *            the lemma's ID.
	 * @param lemma
	 *            the <code>String</code> to update the entry with.
	 * @throws Exception
	 */
	public abstract void updateLemma(int lemmaID, String lemma) throws Exception;

	/**
	 * Updates an existing transcription item inside the database.
	 *
	 * @param transID
	 *            the transcription item's corresponding ID.
	 * @param wordClass
	 *            the transcription item's new word class.
	 * @param variety
	 *            the transcription item's new variety.
	 * @param transType
	 *            the transcription item's new type.
	 * @param transcription
	 *            the transcription item's new phonetic <code>String</code>.
	 * @return the updated {@link TranscriptionItem} object as retrieved from the database after
	 *         adding the given data.
	 * @throws Exception
	 */
	public abstract TranscriptionItem updateTranscription(int transID, WordClass wordClass, Variety variety,
			TranscriptionType transType, String transcription) throws Exception;

	/**
	 * Deletes an existing transcription item from the database.
	 *
	 * @param transID
	 *            the transcription item's corresponding ID.
	 * @throws Exception
	 */
	public abstract void deleteTranscription(int transID) throws Exception;

	/**
	 * Deletes an existing transcription item from the database.
	 *
	 * @param item
	 *            the item to delete.
	 * @throws Exception
	 */
	public abstract void deleteTranscription(TranscriptionItem item) throws Exception;
}
