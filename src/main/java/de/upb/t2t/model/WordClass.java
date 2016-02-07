package de.upb.t2t.model;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.upb.t2t.control.sqlite.SQLiteBridgeDesktop;

/**
 * A rather static class that contains a list of all word classes that have been parsed from the
 * database on startup. A <code>WordClass</code> consists of its proper name and said names
 * abbreviation. Additionally, it has an ID and a flag indicating whether or not the world class
 * belongs to the set of content words.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class WordClass implements Abbreviable, Comparable<WordClass> {

	/* Static Variables */
	/** An array of all <code>WordClass</code>es parsed from the database on startup. */
	private static WordClass[] KNOWN_WORD_CLASSES;

	static {
		try {
			KNOWN_WORD_CLASSES = SQLiteBridgeDesktop.getInstance().getWordClasses();
		} catch (SQLException e) {
			Logger.getGlobal().log(Level.SEVERE, "Unable to retrieve word classes from database!", e);
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE, "Unexpected exception while retrieving word classes from database!",
					e);
		}
	}

	/* Static Methods */
	/**
	 * A static getter imitating the way enumeration values can be retrieved.
	 *
	 * @return the array containing all parsed word classes.
	 */
	public static WordClass[] values() {
		return KNOWN_WORD_CLASSES;
	}

	/**
	 * A getter to retrieve a stored <code>WordClass</code> object by its name or abbreviation.
	 * 
	 * @param string
	 *            the <code>String</code> to look up.
	 * @return the corresponding <code>WordClass</code> object.
	 * @throws NoSuchElementException
	 *             if no corresponding <code>WordClass</code> could be found.
	 */
	public static WordClass getWordClass(String string) throws NoSuchElementException {
		string = string.trim();
		for (WordClass wordClass : KNOWN_WORD_CLASSES) {
			if (wordClass.getAbbreviation().equalsIgnoreCase(string) || wordClass.getProperName().equals(string)) {
				return wordClass;
			}
		}
		throw new NoSuchElementException("Could not retrieve word class from string \"" + string + "\"!");
	}

	/**
	 * A getter to retrieve a stored <code>WordClass</code> object by its ID.
	 * 
	 * @param id
	 *            the ID to look up.
	 * @return the corresponding <code>WordClass</code> object.
	 * @throws NoSuchElementException
	 *             if no corresponding <code>WordClass</code> could be found.
	 */
	public static WordClass getWordClass(int id) throws NoSuchElementException {
		for (WordClass wordClass : KNOWN_WORD_CLASSES) {
			if (wordClass.getID() == id) {
				return wordClass;
			}
		}
		throw new NoSuchElementException("No word class stored for id \"" + id + "\"!");
	}

	/* Static Methods */

	/* Object Variables */
	/** The word class's ID. */
	private int id;
	/** The word class's name, e.g. "Common Noun". */
	private String properName;
	/** The word class's abbreviation, e.g. "comN". */
	private String abbreviation;
	/** Indicates whether or not this <code>WordClass</code> object indicates a content word. */
	private boolean contentWord;

	/* Constructors */
	/**
	 * Constructor for the {@link WordClass} class.
	 * 
	 * @param id
	 *            the word class's ID.
	 * @param properName
	 *            the word class's proper name.
	 * @param abbreviation
	 *            the word class's abbreviated name.
	 * @param contentWord
	 *            whether or not this word class indicates a content word.
	 */
	public WordClass(int id, String properName, String abbreviation, boolean contentWord) {
		this.properName = properName;
		this.abbreviation = abbreviation;
		this.id = id;
		this.contentWord = contentWord;
	}

	/* Object Methods */
	/**
	 * Compares this <code>WordClass</code> instance to another one by {@link #id}.
	 * 
	 * @return a value smaller, equal to or greater than 0 according to
	 *         {@link Comparable#compareTo(Object)}.
	 */
	@Override
	public int compareTo(WordClass otherWordClass) {
		int otherID = otherWordClass.getID();
		if (id < otherID) {
			return -1;
		}
		if (id == otherID) {
			return 0;
		}
		return 1;
	}

	/* Getters and Setters */
	/**
	 * A getter for the {@link #properName} attribute.
	 * 
	 * @return the <code>properName</code> attribute.
	 */
	public String getProperName() {
		return properName;
	}

	@Override
	public String getAbbreviation() {
		return abbreviation;
	}

	/**
	 * A getter for the {@link #id} attribute.
	 * 
	 * @return the <code>id</code> attribute.
	 */
	public int getID() {
		return id;
	}

	/**
	 * A getter for the {@link #contentWord} attribute.
	 * 
	 * @return the <code>contentWord</code> attribute.
	 */
	public boolean isContentWord() {
		return contentWord;
	}

	@Override
	public String toString() {
		return properName + " (" + abbreviation + ", " + id + ")";
	}
}
