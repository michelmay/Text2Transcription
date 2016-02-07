package de.upb.t2t.control.gui.transcriber;

import javafx.scene.control.Button;

/**
 * An implementation of the {@link Button} class that will automatically adapt the
 * {@link #textProperty()} and CSS style classes.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class DelimiterButton extends Button {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */

	/* Constructors */
	/**
	 * Constructor for the {@link DelimiterButton} class.
	 *
	 * @param text
	 *            the text to set as the {@link #textProperty()}.
	 * @param enclosing
	 *            indicates whether or not this <code>DelimiterButton</code> will function as an
	 *            enclosing delimiter ('/').
	 */
	public DelimiterButton(String text, boolean enclosing) {
		super(text);
		setDisable(true);
		getStyleClass().addAll("transcriptionButton", enclosing ? "enclosingDelimiter" : "innerDelimiter");
	}

	/* Object Methods */

	/* Getters and Setters */
}
