/**
 *
 */
package de.upb.t2t.control.database.editing;

import de.upb.t2t.control.database.AbstractSQLiteBridge;
import de.upb.t2t.model.TranscriptionItem;
import de.upb.t2t.model.TranscriptionType;
import de.upb.t2t.model.Variety;
import de.upb.t2t.model.WordClass;

/**
 * An implementation of the {@link AbstractDatabaseEditingHandler} that provides offline editing
 * functionality. Therefore, it is nothing more than a wrapper for the {@link AbstractSQLiteBridge}
 * class. This may seem unnecessary at first sight. However, components such as the
 * {@link AddTranscriptionPane} which offer database editing functionality may do so in both an
 * online, offline, or yet another fashion - hence this class.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class OfflineHandler extends AbstractDatabaseEditingHandler {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */

	/* Constructors */

	/* Object Methods */
	/**
	 * A simple wrapper for
	 * {@link AbstractSQLiteBridge#addTranscription(String, WordClass, Variety, TranscriptionType, String)}
	 * .
	 */
	@Override
	public TranscriptionItem addTranscription(String lemma, WordClass wordClass, Variety variety,
			TranscriptionType transType, String transcription) throws Exception {
		return AbstractSQLiteBridge.getInstance().addTranscription(lemma, wordClass, variety, transType, transcription);
	}

	/**
	 * A simple wrapper for {@link AbstractSQLiteBridge#updateLemma(int, String)}.
	 */
	@Override
	public void updateLemma(int lemmaID, String lemma) throws Exception {
		AbstractSQLiteBridge.getInstance().updateLemma(lemmaID, lemma);
	}

	/**
	 * A simple wrapper for
	 * {@link AbstractSQLiteBridge#updateTranscription(int, WordClass, Variety, TranscriptionType, String)}
	 * .
	 */
	@Override
	public TranscriptionItem updateTranscription(int transID, WordClass wordClass, Variety variety,
			TranscriptionType transType, String transcription) throws Exception {
		return AbstractSQLiteBridge.getInstance().updateTranscription(transID, wordClass, variety, transType,
				transcription);
	}

	/**
	 * A simple wrapper for {@link AbstractSQLiteBridge#deleteTranscription(int)}.
	 */
	@Override
	public void deleteTranscription(int transID) throws Exception {
		AbstractSQLiteBridge.getInstance().deleteTranscription(transID);
	}

	/**
	 * A simple wrapper for {@link AbstractSQLiteBridge#deleteTranscription(TranscriptionItem)}.
	 */
	@Override
	public void deleteTranscription(TranscriptionItem item) throws Exception {
		AbstractSQLiteBridge.getInstance().deleteTranscription(item);
	}

	/* Getters & Setters */
}
