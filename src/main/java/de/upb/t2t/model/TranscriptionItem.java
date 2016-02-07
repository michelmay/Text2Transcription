/**
 *
 */
package de.upb.t2t.model;

import java.util.ArrayList;
import java.util.List;

import de.upb.t2t.control.SelectionListener;
import de.upb.t2t.control.database.editing.AddTranscriptionPane;

/**
 * <p>
 * One of the core classes of the application's data structure. A <code>TranscriptionItem</code>
 * consists of its ID, the lemma associated with it and a <code>String</code> holding its broad
 * phonetic transcription. Additionally, every instance of this class is associated with a
 * corresponding {@link WordClass}, {@link Variety} and {@link TranscriptionType} object to make it
 * distinguishable from others that might be transcribed in the same way but, in fact, need to be
 * analysed differently.
 * </p>
 * <p>
 * <code>TranscriptionItem</code>s are not meant to be modified after their creation. However, it is
 * possible to create an "empty" item that can function as a dummy (in order to avoid
 * {@link NullPointerException}s. This might for example be the case if a new item should be added
 * to the database and an {@link AddTranscriptionPane} is being created.
 * </p>
 * <p>
 * Lastly, other objects may register listeners to a <code>TranscriptionItem</code> to get notified
 * should e.g. the user select the item in the course of a transcription.
 * </p>
 *
 * @author Michel May (michel-may@gmx.de)
 *
 * @see #select()
 * @see #addItemSelectionListener(SelectionListener)
 * @see DatabaseEntry
 */
public class TranscriptionItem {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** The transcription item's id. */
	private int id;
	/** The transcription item's corresponding lemma. */
	private String lemma;
	/** The transcription item's phonetic <code>String</code>. */
	private String phoneticString;
	/** The transcription item's type. */
	private TranscriptionType transType;
	/** The transcription item's word class. */
	private WordClass wordClass;
	/** The transcription item's variety. */
	private Variety variety;
	/** The transcription item's list of listeners that wait for its selection. */
	private List<SelectionListener<TranscriptionItem>> listeners;

	/* Constructors */
	/**
	 * Constructor for the {@link TranscriptionItem} class. Will initialise all attributes with
	 * <code>null</code> values apart fromt he lemma.
	 *
	 * @param lemma
	 *            the <code>TranscriptionItem</code>'s lemma.
	 */
	public TranscriptionItem(String lemma) {
		this(-1, lemma, (String) null, (TranscriptionType) null, (WordClass) null, (Variety) null);
	}

	/**
	 * Constructor for the {@link TranscriptionItem} class.
	 *
	 * @param id
	 *            the transcription item's id.
	 * @param lemma
	 *            the transcription item's lemma.
	 * @param phoneticString
	 *            the transcription item's phonetic <code>String</code>.
	 * @param transType
	 *            the transcription item's <code>TranscriptionType</code>.
	 * @param wordClass
	 *            the transcription item's <code>WordClass</code>.
	 * @param variety
	 *            the transcription item's <code>Variety</code>.
	 */
	public TranscriptionItem(int id, String lemma, String phoneticString, TranscriptionType transType,
			WordClass wordClass, Variety variety) {
		this.id = id;
		this.lemma = lemma;
		this.phoneticString = phoneticString;
		this.transType = transType;
		this.wordClass = wordClass;
		this.variety = variety;
		listeners = new ArrayList<SelectionListener<TranscriptionItem>>();
	}

	/* Object Methods */
	/**
	 * This method notifies all registered listeners, that this item has been selected. This might
	 * be the case if the user chose it for a transcription in case they had several options.
	 */
	public synchronized void select() {
		for (SelectionListener<TranscriptionItem> listener : listeners) {
			new Thread(() -> {
				listener.onItemSelected(TranscriptionItem.this);
			}).start();
		}
	}

	/**
	 * This method registers adds a new <code>SelectionListener</code> to this instance.
	 *
	 * @param listener
	 *            the listener to add.
	 */
	public synchronized void addItemSelectionListener(SelectionListener<TranscriptionItem> listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	/**
	 * This method registers removes a new <code>SelectionListener</code> from this instance.
	 *
	 * @param listener
	 *            the listener to remove.
	 */
	public synchronized void removeItemSelectionListener(SelectionListener<TranscriptionItem> listener) {
		listeners.remove(listener);
	}

	/* Getters and Setters */
	/**
	 * A convenience getter to tell whether or not this <code>TranscriptionItem</code> has been
	 * retrieved fromt he SQL database.
	 *
	 * @return <code>true</code> if the item's {@link #id} is greater than 0; <code>false</code>
	 *         otherwise.
	 */
	public boolean hasSqlID() {
		return id > 0;
	}

	/**
	 * A convenience getter to determine whether or not this transcription item is a content word.
	 *
	 * @return <code>true</code> if this items is a content word; <code>false</code> if it is a
	 *         function word
	 */
	public boolean isContentWord() {
		return wordClass.isContentWord();
	}

	/**
	 * A getter for the transcription item's {@link #id} variable.
	 *
	 * @return the transcription item's id.
	 */
	public int getId() {
		return id;
	}

	/**
	 * A getter for the transcription item's {@link #lemma} variable.
	 *
	 * @return the transcription item's lemma.
	 */
	public String getLemma() {
		return lemma;
	}

	/**
	 * A getter for the transcription item's {@link #phoneticString} variable.
	 *
	 * @return the transcription item's phoneticString.
	 */
	public String getPhoneticString() {
		return phoneticString;
	}

	/**
	 * A getter for the transcription item's {@link #transType} variable.
	 *
	 * @return the transcription item's transType.
	 */
	public TranscriptionType getTransType() {
		return transType;
	}

	/**
	 * A getter for the transcription item's {@link #phoneticString} variable.
	 *
	 * @return the transcription item's wordClass.
	 */
	public WordClass getWordClass() {
		return wordClass;
	}

	/**
	 * A getter for the transcription item's {@link #phoneticString} variable.
	 *
	 * @return the transcription item's lemma.
	 */
	public Variety getVariety() {
		return variety;
	}

	/**
	 * This class's <code>toString</code> function. The <code>String</code> it returns will contain
	 * all object variables (excluding the list of listeners).
	 *
	 * @return the <code>String</code> representation of this class.
	 */
	@Override
	public String toString() {
		String result = "TranscriptionItem:";
		result += "\n\tID: " + id;
		result += "\n\tLemma: " + lemma;
		result += "\n\tPhonetic String: /" + phoneticString + "/";
		result += "\n\tTranscription Type: " + transType;
		result += "\n\tWord Class: " + wordClass;
		result += "\n\tVariety: " + variety;

		return result;
	}
}
