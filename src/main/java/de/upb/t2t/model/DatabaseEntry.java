package de.upb.t2t.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import de.upb.t2t.control.SelectionListener;
import de.upb.t2t.control.database.AbstractSQLiteBridge;

import java.util.NoSuchElementException;

import javafx.beans.property.SimpleObjectProperty;

/**
 * 
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class DatabaseEntry implements SelectionListener<TranscriptionItem> {

	/* Static Variables */
	private static boolean isOfPreferredVariety(TranscriptionItem item) {
		return item.getVariety().equals(Variety.getPreferredVariety());
	}

	/* Static Methods */

	/* Object Variables */
	/** The entry's corresponding lemma. */
	private String lemma;
	/**
	 * The entry's main data structure. The information is organised seemingly complicated at first
	 * sight. However, since one of the main purposes of a {@link DatabaseEntry} object is to
	 * intuitively present the user with all possible choices they have when transcribing an item,
	 * this way of holding the data saves a lot of sorting in the long run. Therefore, the
	 * transcription items are held inside a {@link Map} that orderes them by {@link WordClass}. The
	 * map's values are then stored in yet another map, linking {@link Variety} objects and
	 * {@link List}s of {@link TranscriptionItem}s.
	 */
	private Map<WordClass, Map<Variety, List<TranscriptionItem>>> matchedItems;
	/**
	 * The entry's selected item property. Its value will automatically be updated, should
	 * {@link TranscriptionItem#select()} happen to be called on any of the entry's stored items.
	 */
	private SimpleObjectProperty<TranscriptionItem> selectedItem;
	private ReentrantLock lock;

	/* Constructors */
	public DatabaseEntry(String lemma) {
		this.lemma = lemma.toLowerCase();
		matchedItems = new HashMap<WordClass, Map<Variety, List<TranscriptionItem>>>();
		selectedItem = new SimpleObjectProperty<TranscriptionItem>();
		lock = new ReentrantLock(true);
	}

	/* Object Methods */
	/**
	 * Adds a <code>TranscriptionItem</code> to this <code>DatabaseEntry</code> object.
	 * 
	 * @param item
	 *            the item to add.
	 */
	public void addTranscriptionItem(TranscriptionItem item) {

		lock.lock();
		try {

			getTranscriptionsList(item.getWordClass(), item.getVariety()).add(item);
			item.addItemSelectionListener(this);

			/*
			 * Use the new item as the selected one in one of the following cases: We don't have any
			 * selected item yet; contrary to the selected item, the new one is of the preferred
			 * variety; the new item is of the preferred variety, but additioannly also a weak form.
			 */
			TranscriptionItem currentlySelected = selectedItem.get();

			if (currentlySelected == null || (!isOfPreferredVariety(currentlySelected) && isOfPreferredVariety(item))
					|| (isOfPreferredVariety(item) && item.getTransType().equals(TranscriptionType.WEAK))) {
				selectedItem.set(item);
			}

		} finally {
			lock.unlock();
		}
	}

	@Override
	public void onItemSelected(TranscriptionItem item) {
		lock.lock();
		try {
			selectedItem.set(item);
		} finally {
			lock.unlock();
		}
	}

	/* Getters and Setters */
	/**
	 * <p>
	 * A convenience getter to check whether or not this database entry actually holds any
	 * transcription information. If this function returns <code>true</code> and the object
	 * originates from a call to {@link AbstractSQLiteBridge#queryDatabase(String)} then this means
	 * that <b>there was no corresponding entry for the given lemma in the database</b>.
	 * </p>
	 * <p>
	 * Other than that, this function should not return <code>true</code>. If it still does then
	 * this {@link DatabaseEntry} instance has most likely just been created.
	 * </p>
	 * <p>
	 * Calling this function is preferred over <code>getMatchedItems().isEmpty()</code>, because
	 * {@link #getMatchedItems()} will create a (superfluous) immutable instance of the currently
	 * held data structure.
	 * </p>
	 *
	 *
	 * @return <code>true</code> if this database entry holds no transcription data;
	 *         <code>false</code> otherwise
	 */
	public boolean isEmpty() {
		return matchedItems.isEmpty();
	}

	/**
	 * This is getter crucial to the question whether or not multiple items have been retrieved from
	 * the database for the given lemma. It might, for instance, be possible for the word
	 * <code>process</code> to appear both as a noun and as a verb, resulting in different
	 * transcriptions. In that case, the user must determine which one should be used.
	 *
	 * @return <code>true</code> if the entry can be assigned to more than one word class;
	 *         <code>false</code> otherwise.
	 */
	public boolean matchesMultipleWordClasses() {
		return matchedItems.size() > 1;
	}

	/**
	 * A convenience getter that will search the entire entry for a <code>TranscriptionItem</code>
	 * with the given id.
	 * 
	 * @param id
	 *            the ID to search for.
	 * @return the corresponding <code>TranscriptionItem</code>.
	 * @throws NoSuchElementException
	 *             if no corresponding <code>TranscriptionItem</code> could be found.
	 */
	public TranscriptionItem getTranscriptionItemById(int id) throws NoSuchElementException {
		for (Entry<WordClass, Map<Variety, List<TranscriptionItem>>> wordClassEntry : matchedItems.entrySet()) {
			for (Entry<Variety, List<TranscriptionItem>> varietyEntry : wordClassEntry.getValue().entrySet()) {
				for (TranscriptionItem item : varietyEntry.getValue()) {
					if (item.getId() == id) {
						return item;
					}
				}
			}
		}
		throw new NoSuchElementException("No transcription stored in this database entry for id: " + id + "!");
	}

	/**
	 * A convenience getter that will return an immutable <code>List</code> of all
	 * <code>TranscriptionItem</code>s whose <code>WordClass</code> and <code>Variety</code> match
	 * the given parameters.
	 * 
	 * @param wordClass
	 *            the <code>WordClass</code> to search for.
	 * @param variety
	 *            the <code>Variety</code> to search for.
	 * @return an unmodifiable <code>List</code> of the corresponding
	 *         <code>TranscriptionItem</code>s.
	 * @throws NoSuchElementException
	 *             if no <code>TranscriptionItem</code> matches the parameters.
	 */
	public List<TranscriptionItem> getTranscriptionItems(WordClass wordClass, Variety variety)
			throws NoSuchElementException {
		try {
			return Collections.unmodifiableList(matchedItems.get(wordClass).get(variety));
		} catch (NullPointerException e) {
			throw new NoSuchElementException("No transcriptions found for " + wordClass + " + " + variety);
		}
	}

	/**
	 * A getter for the {@link #lemma} attribute. Please note that lemmata will always be stored in
	 * lower case.
	 * 
	 * @return the <code>lemma String</code>.
	 */
	public String getLemma() {
		return lemma;
	}

	/**
	 * The getter for the {@link #matchedItems} variable. Note that the returned instance is
	 * read-only!
	 *
	 * @return an immutable version of the map containing the matched items.
	 */
	public Map<WordClass, Map<Variety, List<TranscriptionItem>>> getMatchedItems() {

		// Prepare the temporary objects we need to build an immutable collection.
		List<TranscriptionItem> tempList;
		Map<Variety, List<TranscriptionItem>> tempMapVarieties;
		Map<WordClass, Map<Variety, List<TranscriptionItem>>> tempMapWordClasses = new HashMap<>();

		// Now run through the entire data structure, copy the values into the temp objects and them
		// in an umodifiable version to their respective super-structure.
		for (Entry<WordClass, Map<Variety, List<TranscriptionItem>>> wordClassEntry : matchedItems.entrySet()) {
			tempMapVarieties = new HashMap<>();
			for (Entry<Variety, List<TranscriptionItem>> varietyEntry : wordClassEntry.getValue().entrySet()) {
				tempList = new ArrayList<>();
				for (TranscriptionItem item : varietyEntry.getValue()) {
					tempList.add(item);
				}
				tempMapVarieties.put(varietyEntry.getKey(), Collections.unmodifiableList(tempList));
			}
			tempMapWordClasses.put(wordClassEntry.getKey(), Collections.unmodifiableMap(tempMapVarieties));
		}

		return Collections.unmodifiableMap(tempMapWordClasses);
	}

	/**
	 * A getter for the {@link #selectedItem} property's value.
	 * 
	 * @return the currently selected <code>TranscriptionItem</code>.
	 */
	public TranscriptionItem getSelectedItem() {
		return selectedItem.get();
	}

	/**
	 * A getter for the {@link #selectedItem} property.
	 * 
	 * @return the <code>selectedItem</code> property.
	 */
	public SimpleObjectProperty<TranscriptionItem> selectedItemProperty() {
		return selectedItem;
	}

	/**
	 * A convenience getter that will return the value (i.e., the varieties <code>Map</code>) for
	 * the given key <code>WordClass</code>. Should no such entry exist, an empty <code>Map</code>
	 * will be created in order to avoid {@link NullPointerException}s.
	 * 
	 * @param wordClass
	 *            the <code>WordClass</code> whose value to retrieve.
	 * @return the correspondingly mapped <code>Map</code> object.
	 */
	private Map<Variety, List<TranscriptionItem>> getVarietiesMap(WordClass wordClass) {

		// Any need to take action at all?
		Map<Variety, List<TranscriptionItem>> varMap = matchedItems.get(wordClass);
		if (varMap == null) {
			varMap = new HashMap<Variety, List<TranscriptionItem>>();
			matchedItems.put(wordClass, varMap);
		}

		return varMap;
	}

	/**
	 * A convenience getter that will return the value (i.e., the transcription items
	 * <code>List</code>) for the given key <code>WordClass</code> and <code>Variety</code>. Should
	 * no such entry exist, an empty <code>List</code> will be created in order to avoid
	 * {@link NullPointerException}s.
	 * 
	 * @param wordClass
	 *            the <code>WordClass</code> whose value to retrieve.
	 * @param variety
	 *            the <code>Variety</code> whose value to retrieve.
	 * @return the correspondingly mapped <code>List</code> object.
	 */
	private List<TranscriptionItem> getTranscriptionsList(WordClass wordClass, Variety variety) {
		Map<Variety, List<TranscriptionItem>> varMap = getVarietiesMap(wordClass);
		List<TranscriptionItem> list = varMap.get(variety);
		if (list == null) {
			list = new ArrayList<TranscriptionItem>();
			varMap.put(variety, list);
		}
		return list;
	}

	/**
	 * This class's <code>toString</code> function. It will contain all
	 * <code>TranscriptionItem</code>s currently held by the object.
	 * 
	 * @return this class's <code>String</code> representation.
	 */
	@Override
	public String toString() {
		String result = "Database Item (\"" + lemma + "\"):";

		// Empty entry?
		if (matchedItems.isEmpty()) {
			result += " Empty!";
			return result;
		}

		// Prepare to iterate over the entire data strucutre.
		Map<Variety, List<TranscriptionItem>> varMap;
		List<TranscriptionItem> transList;

		// Go through all word class mappings.
		for (WordClass wordClass : matchedItems.keySet()) {
			result += "\n\t" + wordClass.getProperName() + ":";

			// Nothing stored?
			varMap = matchedItems.get(wordClass);
			if (varMap == null || varMap.isEmpty()) {
				result += "\n\t\tNo items stored for this word class!";
				continue;
			}

			// Go through all varieties.
			for (Variety variety : varMap.keySet()) {
				result += "\n\t\t" + variety.getProperName() + ":";

				// Nothing stored?
				transList = varMap.get(variety);
				if (transList == null || transList.isEmpty()) {
					result += "\n\t\t\tNo items stored for this variety!";
					continue;
				}

				// Go through all transcription items.
				for (TranscriptionItem item : transList) {
					result += "\n\t\t\t/" + item.getPhoneticString() + "/ (" + item.getTransType().getDescription()
							+ ")";
				}
			}
		}
		return result;
	}
}
