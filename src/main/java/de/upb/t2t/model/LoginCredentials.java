package de.upb.t2t.model;

import de.upb.t2t.control.database.editing.LoginDialogue;
import de.upb.t2t.control.database.editing.OnlineHandler;

/**
 * <code>LoginCredentials</code> are used to verify whether or not a user is allowed to make changes
 * to the online (!) database. They will be sent along with every modification attempt.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 * @see OnlineHandler
 * @see LoginDialogue
 */
public class LoginCredentials {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** The user's username. */
	private String username;
	/** The user's password. */
	private String password;

	/* Constructors */
	/**
	 * Constructor for the {@link LoginCredentials} class. Will instantiate a new object with empty
	 * attribute values.
	 */
	public LoginCredentials() {
		this(null, null);
	}

	/**
	 * Constructor for the {@link LoginCredentials} class.
	 *
	 * @param username
	 *            the user's username.
	 * @param password
	 *            the user's password.
	 */
	public LoginCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}

	/* Object Methods */

	/* Getters and Setters */
	/**
	 * A convenience getter to check whether or not the {@link #username} and {@link #password}
	 * fields actually contain data.
	 *
	 * @return <code>true</code> if both fields are not empty; <code>false</code> otherwise.
	 */
	public boolean available() {
		return username != null && !username.isEmpty() && password != null && !password.isEmpty();
	}

	/**
	 * A getter for the {@link #username} attribute.
	 *
	 * @return the {@link #username} attribute.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * A getter for the {@link #password} attribute.
	 *
	 * @return the {@link #password} attribute.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * A setter for the {@link #username} attribute.
	 *
	 * @param username
	 *            the username to set.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * A setter for the {@link #password} attribute.
	 *
	 * @param password
	 *            the username to set.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * This class's <code>toString</code> function. The <code>String</code> it returns will contain
	 * both the {@link #username} and the {@link #password}.
	 *
	 * @return the <code>String</code> representation of this class.
	 */
	@Override
	public String toString() {
		return "Username:\t" + username + "\nPassword:\t" + password;
	}
}
