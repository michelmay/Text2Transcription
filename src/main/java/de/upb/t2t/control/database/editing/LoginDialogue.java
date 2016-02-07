/**
 *
 */
package de.upb.t2t.control.database.editing;

import de.upb.t2t.model.LoginCredentials;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * <p>
 * A simple dialogue that prompts the user for their username and password. If both are given, it
 * converts them into a {@link LoginCredentials} object. By appropriately disabling its
 * {@link #loginButton}, it will prevent invalid input, such as empty or <code>null String</code>s.
 * </p>
 * <p>
 * This class is heavily inspired by a sample provided by Marco Jakob on
 * <a href="http://code.makery.ch/blog/javafx-dialogs-official/">his website</a> with only a few
 * adjustments to it.
 * </p>
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class LoginDialogue extends Dialog<LoginCredentials> {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** The dialogue's username text field. */
	private TextField username;
	/** The dialogue's password field. */
	private PasswordField password;
	/** The dialogue's login button. */
	private Node loginButton;

	/* Constructors */
	/**
	 * Constructor for the {@link LoginDialogue} class. In case a username is already known to the
	 * application (e.g. from the command line or from a previous login attempt), it may be passed
	 * to the constructor and automatically be injected into the {@link #username} field.
	 *
	 * @param givenName
	 *            a username for convenience. May be empty or <code>null</code>.
	 */
	public LoginDialogue(String givenName) {
		super();
		setTitle("Login");
		setHeaderText("In order to be able to edit the databse, please log in.");

		// Set the button types.
		ButtonType loginButtonType = new ButtonType("Login", ButtonData.OK_DONE);
		getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

		// Create the username and password labels and fields.
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 150, 10, 10));

		username = new TextField();
		username.setPromptText("Username");
		username.setText(givenName != null ? givenName : "");
		password = new PasswordField();
		password.setPromptText("Password");

		grid.add(new Label("Username:"), 0, 0);
		grid.add(username, 1, 0);
		grid.add(new Label("Password:"), 0, 1);
		grid.add(password, 1, 1);

		// Enable/Disable login button depending on whether a username was
		// entered.
		loginButton = getDialogPane().lookupButton(loginButtonType);
		loginButton.setDisable(true);

		// Do some validation (using the Java 8 lambda syntax).
		username.textProperty().addListener((observable, oldValue, newValue) -> {
			updateLoginButtonEnabled();
		});
		password.textProperty().addListener((observable, oldValue, newValue) -> {
			updateLoginButtonEnabled();
		});

		getDialogPane().setContent(grid);

		// Request focus on the username field by default.
		Platform.runLater(() -> {
			username.requestFocus();
		});

		// Convert the result to a username-password-pair when the login button
		// is clicked.
		setResultConverter(dialogButton -> {
			if (dialogButton == loginButtonType) {
				LoginCredentials result = new LoginCredentials();
				result.setUsername(username.getText().trim());
				result.setPassword(password.getText());
				return result;
			}
			return null;
		});
	}

	/* Object Methods */
	/**
	 * A convenience method to appropriately enable or disable the {@link #loginButton}.
	 */
	private void updateLoginButtonEnabled() {
		loginButton.setDisable(username.getText().trim().isEmpty() || password.getText().trim().isEmpty());
	}

	/* Getters and Setters */
}
