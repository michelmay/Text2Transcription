package de.upb.t2t.model;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.upb.t2t.control.MainController;
import de.upb.t2t.control.sqlite.SQLiteBridgeDesktop;

/**
 * A rather static class that contains a list of all varieties that have been parsed from the
 * database on startup. A <code>Variety</code> consists of an ID, its proper name and said names
 * abbreviation.
 * 
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class Variety implements Abbreviable, Comparable<Variety> {

	/* Static Variables */
	/** An array of all <code>Variety</code>es parsed from the database on startup. */
	private static Variety[] KNOWN_VARIETIES;

	static {
		try {
			KNOWN_VARIETIES = SQLiteBridgeDesktop.getInstance().getVarieties();
		} catch (SQLException e) {
			Logger.getGlobal().log(Level.SEVERE, "Unable to retrieve varieties from database!", e);
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE, "Unexpected exception while retrieving varieties from database!", e);
		}
	}

	/* Static Methods */
	/**
	 * A static getter imitating the way enumeration values can be retrieved.
	 *
	 * @return the array containing all parsed varieties.
	 */
	public static Variety[] values() {
		return KNOWN_VARIETIES;
	}

	/**
	 * A getter to retrieve a stored <code>Variety</code> object by its name or abbreviation.
	 * 
	 * @param string
	 *            the <code>String</code> to look up.
	 * @return the corresponding <code>Variety</code> object.
	 * @throws NoSuchElementException
	 *             if no corresponding <code>Variety</code> could be found.
	 */
	public static Variety getVariety(String string) throws NoSuchElementException {
		string = string.trim();
		for (Variety variety : KNOWN_VARIETIES) {
			if (variety.getAbbreviation().equalsIgnoreCase(string) || variety.getProperName().equals(string)) {
				return variety;
			}
		}
		throw new NoSuchElementException("Could not retrieve variety from string \"" + string + "\"!");
	}

	/**
	 * A getter to retrieve a stored <code>Variety</code> object by its ID.
	 * 
	 * @param id
	 *            the ID to look up.
	 * @return the corresponding <code>Variety</code> object.
	 * @throws NoSuchElementException
	 *             if no corresponding <code>Variety</code> could be found.
	 */
	public static Variety getVariety(int id) throws NoSuchElementException {
		for (Variety variety : KNOWN_VARIETIES) {
			if (variety.getID() == id) {
				return variety;
			}
		}
		throw new NoSuchElementException("No variety stored for id \"" + id + "\"!");
	}

	/**
	 * A convenience getter that will look up what <code>Variety</code> the user has currently
	 * selected as their preference and then retrieve it.
	 * 
	 * @return the preferred <code>Variety</code>.
	 * @throws NoSuchElementException
	 *             if {@link #getVariety(String)} throws one.
	 */
	public static Variety getPreferredVariety() throws NoSuchElementException {
		return getVariety(MainController.getProperty("variety.preferred"));
	}

	/* Object Variables */
	/** The variety's id as it's stored in the database. */
	private int id;
	/** The variety's name, e.g. "British English". */
	private String properName;
	/** The variety's abbreviated name, e.g. "BrE". */
	private String abbreviation;

	/* Constructors */
	/**
	 * Constructor for the {@link Variety} class.
	 * 
	 * @param id
	 *            the variety's ID.
	 * @param name
	 *            the variety's proper name.
	 * @param abbreviation
	 *            the variety's abbreviated name.
	 */
	public Variety(int id, String name, String abbreviation) {
		this.id = id;
		this.properName = name;
		this.abbreviation = abbreviation;
	}

	/* Object Methods */
	/**
	 * Compares this <code>Variety</code> instance to another one by {@link #id}.
	 * 
	 * @return a value smaller, equal to or greater than 0 according to
	 *         {@link Comparable#compareTo(Object)}.
	 */
	@Override
	public int compareTo(Variety otherVariety) {
		int otherID = otherVariety.getID();
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

	@Override
	public boolean equals(Object obj) {

		// Not even of the same instance?
		if (!(obj instanceof Variety)) {
			return false;
		}

		Variety var = (Variety) obj;

		return super.equals(var) || (id == var.getID() && properName.equals(var.getProperName())
				&& abbreviation.equals(var.getAbbreviation()));
	};

	@Override
	public String toString() {
		return properName + " (" + abbreviation + ", " + id + ")";
	}
}
