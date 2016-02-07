/**
 *
 */
package de.upb.t2t.model;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.upb.t2t.control.sqlite.SQLiteBridgeDesktop;

/**
 * A rather static class that contains a list of all punctuation characters that have been parsed
 * from the database on startup. A <code>PunctuationCharacter</code> consists of the actual
 * <code>char</code> primitive (e.g. '.', ':', '?', ...) and a delimiter mode. The latter indicates,
 * which type of notational bar ('|' vs. '||') to use should the character happen to be transcribed.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class PunctuationCharacter {

	/* Static Variables */
	/** An array of all <code>PunctuationCharacter</code>s parsed from the database on startup. */
	private static PunctuationCharacter[] KNOWN_PUNCTUATION_CHARACTERS;

	static {
		try {
			KNOWN_PUNCTUATION_CHARACTERS = SQLiteBridgeDesktop.getInstance().getPunctuationCharacters();
		} catch (SQLException e) {
			Logger.getGlobal().log(Level.SEVERE, "Unable to retrieve punctuation characters from database!", e);
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE,
					"Unexpected exception while retrieving punctuation characters from database!", e);
		}
	}

	/* Static Methods */
	/**
	 * A static getter imitating the way enumeration values can be retrieved.
	 *
	 * @return the array containing all parsed punctuation characters.
	 */
	public static PunctuationCharacter[] values() {
		return KNOWN_PUNCTUATION_CHARACTERS;
	}

	/**
	 * A convenience getter that will check whether or not the given character has a corresponding
	 * {@link PunctuationCharacter} object. Contrary to {@link #getPunctChar(char)}, no
	 * {@link NoSuchElementException} will be thrown.
	 *
	 * @param character
	 *            the character to check.
	 * @return <code>true</code> if there is a corresponding <code>PunctuationCharacter</code>
	 *         object; <code>false</code> otherwise.
	 */
	public static boolean isPunctuationCharacter(char character) {
		try {
			return getPunctChar(character) != null;
		} catch (NoSuchElementException e) {
			// Simply not a (supported) currencyCharacter.
		}
		return false;
	}

	/**
	 * A convenience getter that will check whether or not the given character has a corresponding
	 * {@link PunctuationCharacter} object. Contrary to {@link #getPunctChar(String)}, no
	 * {@link NoSuchElementException} will be thrown.
	 *
	 * @param character
	 *            a <code>String</code> of length 1 containing the character to check.
	 * @return <code>true</code> if there is a corresponding punctuation character object;
	 *         <code>false</code> otherwise.
	 * @throws IllegalArgumentException
	 *             if the parameter <code>String</code> was no of length 1.
	 */
	public static boolean isPunctuationCharacter(String character)
			throws IllegalArgumentException, NoSuchElementException {
		try {
			return getPunctChar(character) != null;
		} catch (IllegalArgumentException e) {
			Logger.getGlobal().log(Level.WARNING, "Exception while checking for punctuation character!", e);
		} catch (NoSuchElementException e) {
			// Simply not a (supported) currencyCharacter.
		}
		return false;
	}

	/**
	 * A convenience getter that will return <code>PunctuationCharacter</code> for the given
	 * <code>char</code>.
	 *
	 * @param character
	 *            the character to look up.
	 * @return the corresponding <code>PunctuationCharacter</code> object.
	 * @throws NoSuchElementException
	 *             if there is no stored <code>PunctuationCharacter</code> object.
	 */
	public static PunctuationCharacter getPunctChar(char character) throws NoSuchElementException {
		for (PunctuationCharacter punctChar : KNOWN_PUNCTUATION_CHARACTERS) {
			if (punctChar.equals(character)) {
				return punctChar;
			}
		}
		throw new NoSuchElementException(
				"Could not retrieve punctuation character object from char \"" + character + "\"!");
	}

	/**
	 * A convenience getter that will return <code>PunctuationCharacter</code> for the given
	 * <code>char</code>.
	 *
	 * @param character
	 *            a <code>String</code> of length 1 containing the character to look up.
	 * @return the corresponding <code>PunctuationCharacter</code> object.
	 * @throws IllegalArgumentException
	 *             if the given <code>String</code> was not of length 1.
	 * @throws NoSuchElementException
	 *             if there is no stored <code>PunctuationCharacter</code> object.
	 */
	public static PunctuationCharacter getPunctChar(String character)
			throws IllegalArgumentException, NoSuchElementException {
		if (character.length() != 1) {
			throw new IllegalArgumentException(
					"There must only be a single character inside the parameter string \"" + character + "\"!");
		}
		return getPunctChar(character.charAt(0));
	}

	/* Object Variables */
	/** The <code>PunctuationCharacter</code>'s actual character value. */
	private char character;
	/**
	 * The punctuation character's delimiter mode. A value of 0 indicates that no notational bar is
	 * required when transcribing this character, a 1 signifies a single ("|"), and a 2 a double bar
	 * ("||").
	 */
	private int delimiterMode;

	/* Constructors */
	/**
	 * Convenience constructor for the {@link PunctuationCharacter} class.
	 *
	 * @param character
	 *            a <code>String</code> of length 1 containing the actual character representation
	 *            of the <code>PunctuationCharacter</code>.
	 * @param delimiterMode
	 *            the <code>PunctuationCharacter</code>'s delimiter mode.
	 * @see #character
	 * @see #delimiterMode
	 */
	public PunctuationCharacter(String character, int delimiterMode) {
		if (character.length() != 1) {
			throw new IllegalArgumentException(
					"There must only be a single character inside the parameter string \"" + character + "\"!");
		}
		this.character = character.charAt(0);
		this.delimiterMode = delimiterMode;
	}

	/**
	 * Constructor for the {@link PunctuationCharacter} class.
	 *
	 * @param character
	 *            the <code>PunctuationCharacter</code>'s actual character representation.
	 * @param delimiterMode
	 *            the <code>PunctuationCharacter</code>'s delimiter mode.
	 * @see #character
	 * @see #delimiterMode
	 */
	public PunctuationCharacter(char character, int delimiterMode) {
		this.character = character;
		this.delimiterMode = delimiterMode;
	}

	/* Object Methods */
	/**
	 * A convenience function used to check whether this instance's {@link #character} attribute
	 * matches the given <code>char</code>. Please note that this function does not override
	 * {@link Object#equals(Object)}. Therefore, calling {@link #equals(Object)} may lead to
	 * different results.
	 *
	 * @param character
	 *            the <code>char</code> to compare this object against.
	 * @return <code>true</code> if {@link #character} equals the parameter.
	 */
	public boolean equals(char character) {
		return this.character == character;
	}

	/**
	 * A convenience function used to compare two instances of this class. Returns <code>true</code>
	 * if the two objects' {@link #character} attributes match. Please note that this function does
	 * not override {@link Object#equals(Object)}. Therefore, calling {@link #equals(Object)} may
	 * lead to different results.
	 *
	 * @param punctChar
	 *            the <code>PunctuationCharacter</code> to compare this object against.
	 * @return <code>true</code> if the {@link #character} attributes match.
	 */
	public boolean equals(PunctuationCharacter punctChar) {
		return character == punctChar.getCharacter();
	}

	/* Getters and Setters */
	/**
	 * A getter for the {@link #character} attribute.
	 *
	 * @return the <code>character</code>} attribute.
	 */
	public char getCharacter() {
		return character;
	}

	/**
	 * A getter for the {@link #delimiterMode} attribute.
	 *
	 * @return the <code>delimiterMode</code> attribute.
	 */
	public int getDelimiterMode() {
		return delimiterMode;
	}

	/**
	 * This class's <code>toString()</code> function.
	 */
	@Override
	public String toString() {
		return character + " (" + delimiterMode + ")";
	}
}
