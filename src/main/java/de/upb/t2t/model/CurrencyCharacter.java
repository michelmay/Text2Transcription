package de.upb.t2t.model;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.upb.t2t.control.sqlite.SQLiteBridgeDesktop;

/**
 * A rather static class that contains a list of all currency characters that have been parsed from
 * the database on startup. A <code>CurrencyCharacter</code> consists of the actual
 * <code>char</code> primitive (e.g. '$', '€', '£', ...), a singular and a plural form. The latter
 * can be used for querying the database as look up <code>String</code>s.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class CurrencyCharacter {

	/* Static Variables */
	/** An array of all <code>CurrencyCharacter</code>s parsed from the database on startup. */
	private static CurrencyCharacter[] KNOWN_CURRENCY_CHARACTERS;

	static {
		try {
			KNOWN_CURRENCY_CHARACTERS = SQLiteBridgeDesktop.getInstance().getCurrencyCharacters();
		} catch (SQLException e) {
			Logger.getGlobal().log(Level.SEVERE, "Unable to retrieve currency characters from database!", e);
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE,
					"Unexpected exception while retrieving currency characters from database!", e);
		}
	}

	/* Static Methods */
	/**
	 * A static getter imitating the way enumeration values can be retrieved.
	 *
	 * @return the array containing all parsed <code>CurrencyCharacter</code>s.
	 */
	public static CurrencyCharacter[] values() {
		return KNOWN_CURRENCY_CHARACTERS;
	}

	/**
	 * A convenience getter that will check whether or not the given character has a corresponding
	 * {@link CurrencyCharacter} object. Contrary to {@link #getCurrencyChar(char)}, no
	 * {@link NoSuchElementException} will be thrown.
	 *
	 * @param character
	 *            the character to check.
	 * @return <code>true</code> if there is a corresponding <code>CurrencyCharacter</code> object;
	 *         <code>false</code> otherwise.
	 */
	public static boolean isCurrencyCharacter(char character) {
		try {
			return getCurrencyChar(character) != null;
		} catch (NoSuchElementException e) {
			// Simply not a (supported) currencyCharacter.
		}
		return false;
	}

	/**
	 * A convenience getter that will check whether or not the given character has a corresponding
	 * {@link CurrencyCharacter} object. Contrary to {@link #getCurrencyChar(String)}, no
	 * {@link NoSuchElementException} will be thrown.
	 *
	 * @param character
	 *            a <code>String</code> of length 1 containing the character to check.
	 * @return <code>true</code> if there is a corresponding <code>CurrencyCharacter</code> object;
	 *         <code>false</code> otherwise.
	 */
	public static boolean isCurrencyCharacter(String character) {
		try {
			return getCurrencyChar(character) != null;
		} catch (IllegalArgumentException e) {
			Logger.getGlobal().log(Level.WARNING, "Exception while checking for currency character!", e);
		} catch (NoSuchElementException e) {
			// Simply not a (supported) currencyCharacter.
		}
		return false;
	}

	/**
	 * A convenience getter that will return <code>CurrencyCharacter</code> for the given
	 * <code>char</code>.
	 *
	 * @param character
	 *            the character to look up.
	 * @return the corresponding <code>CurrencyCharacter</code> object.
	 * @throws NoSuchElementException
	 *             if there is no stored <code>CurrencyCharacter</code> object.
	 */
	public static CurrencyCharacter getCurrencyChar(char character) {
		for (CurrencyCharacter currencyChar : KNOWN_CURRENCY_CHARACTERS) {
			if (currencyChar.equals(character)) {
				return currencyChar;
			}
		}
		throw new NoSuchElementException(
				"Could not retrieve currency character object from char \"" + character + "\"!");
	}

	/**
	 * A convenience getter that will return <code>CurrencyCharacter</code> for the given
	 * <code>char</code>.
	 *
	 * @param character
	 *            a <code>String</code> of length 1 containing the character to look up.
	 * @return the corresponding <code>CurrencyCharacter</code> object.
	 * @throws IllegalArgumentException
	 *             if the given <code>String</code> was not of length 1.
	 * @throws NoSuchElementException
	 *             if there is no stored <code>CurrencyCharacter</code> object.
	 */
	public static CurrencyCharacter getCurrencyChar(String character) {
		if (character.length() != 1) {
			throw new IllegalArgumentException(
					"There must only be a single character inside the parameter string \"" + character + "\"!");
		}
		return getCurrencyChar(character.charAt(0));
	}

	/* Object Variables */
	/** The <code>CurrencyCharacter</code>'s actual character value. */
	private char character;
	/** The <code>CurrencyCharacter</code>'s singular <code>String</code> representation. */
	private String singular;
	/** The <code>CurrencyCharacter</code>'s plural <code>String</code> representation. */
	private String plural;

	/* Constructors */
	/**
	 * Convenience constructor for the {@link CurrencyCharacter} class.
	 *
	 * @param character
	 *            a <code>String</code> of length 1 containing the actual character representation
	 *            of the <code>CurrencyCharacter</code>.
	 * @param singular
	 *            the <code>String</code> representation of the character's singular form.
	 * @param plural
	 *            the <code>String</code> representation of the character's plural form.
	 */
	public CurrencyCharacter(String character, String singular, String plural) {
		if (character.length() != 1) {
			throw new IllegalArgumentException(
					"There must only be a single character inside the parameter string \"" + character + "\"!");
		}
		this.character = character.charAt(0);
		this.singular = singular;
		this.plural = plural;
	}

	/**
	 * Constructor for the {@link CurrencyCharacter} class.
	 *
	 * @param character
	 *            the actual <code>char</code> representation of the <code>CurrencyCharacter</code>
	 * @param singular
	 *            the <code>String</code> representation of the character's singular form.
	 * @param plural
	 *            the <code>String</code> representation of the character's plural form.
	 */
	public CurrencyCharacter(char character, String singular, String plural) {
		this.character = character;
		this.singular = singular;
		this.plural = plural;
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
	 * @param currencyChar
	 *            the <code>CurrencyCharacter</code> to compare this object against.
	 * @return <code>true</code> if the {@link #character} attributes match.
	 */
	public boolean equals(CurrencyCharacter currencyChar) {
		return character == currencyChar.getCharacter();
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
	 * A getter for the {@link #singular} attribute.
	 *
	 * @return the <code>singular</code> attribute.
	 */
	public String getSingularLemma() {
		return singular;
	}

	/**
	 * A getter for the {@link #plural} attribute.
	 *
	 * @return the <code>plural</code> attribute.
	 */
	public String getPluralLemma() {
		return plural;
	}

	/**
	 * This class's <code>toString()</code> function.
	 */
	@Override
	public String toString() {
		return character + " (" + singular + ", " + plural + ")";
	}
}
