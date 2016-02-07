package de.upb.t2t.control.gui.transcriber;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.upb.t2t.control.MainController;
import de.upb.t2t.control.Statics;
import de.upb.t2t.control.Transcriber;
import de.upb.t2t.control.database.editing.EditDatabaseDialogue;
import de.upb.t2t.control.database.editing.LoginDialogue;
import de.upb.t2t.control.database.editing.OfflineHandler;
import de.upb.t2t.control.database.editing.OnlineHandler;
import de.upb.t2t.control.gui.UserFeedbackProvider;
import de.upb.t2t.gui.fxml.FXMLProvider;
import de.upb.t2t.model.FeedbackMode;
import de.upb.t2t.model.LoginCredentials;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

/**
 * The root {@link Node} of the primary's stages {@link Scene}. This pane is most central to the
 * application, as it features a {@link TextArea} into which the user types whatever word or
 * sentence they wish to have transcribed, as well as the {@link FlowPane} to whom all
 * <code>TranscriptionSegment</code>s will be added as soon as the transcription has started. On its
 * top side, the main {@link Menu} while a {@link ToolBar} at the bottom provides the user with
 * feedback about the status of their transcription.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 * @see Transcriber
 * @see TranscriptionSegment
 */
public class MainTranscriptionPane extends BorderPane implements UserFeedbackProvider {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** The {@link Logger} instance associated with this controller. */
	private Logger logger;
	/** The {@link Transcriber} instance associated with this controller. */
	private Transcriber transcriber;
	/**
	 * Indicates what operation the app is doing(e.g. "Transcribing ..."). Located on the bottom of
	 * the main stage.
	 */
	@FXML
	private Label infoBarLabel;
	/**
	 * Indicates the progress of whatever the application is doing at the moment.
	 */
	@FXML
	private ProgressBar infoBarProgress;
	/**
	 * Into this field the user inserts the words or sentences they wish to be transcribed.
	 */
	@FXML
	private TextArea inputField;
	/**
	 * The field in which the transcriptions are displayed.
	 */
	@FXML
	private FlowPane outputField;

	/* Constructors */
	/**
	 * Constructor for the {@link MainTranscriptionPane} class.
	 *
	 * @throws IOException
	 *             if the {@link FXMLLoader} throws one.
	 * @throws URISyntaxException
	 *             if the {@link FXMLProvider} throws one.
	 */
	public MainTranscriptionPane() throws IOException, URISyntaxException {

		// Prepare the logger and transcriber objects.
		logger = Logger.getLogger(getClass().getName());
		transcriber = new Transcriber();

		// Register this instance as root and controller of the corresponding
		// TranscriptionPanel object.
		FXMLLoader fxmlLoader = new FXMLLoader(new FXMLProvider().getResourceAsURL("MainTranscriptionPane.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		fxmlLoader.load();
	}

	/* Object Methods */
	/**
	 * Displays the given <code>feedback</code> string in the {@link #infoBarLabel}. Should the
	 * first parameter equal {@link FeedbackMode#LOADING}, the {@link #infoBarProgress} will be set
	 * to indeterminate mode. Otherwise, it will simply be hidden.
	 *
	 * @param mode
	 *            the <code>FeedbackMode</code> to set.
	 * @param feedback
	 *            the message to show to the user.
	 */
	@Override
	public void generalFeedback(FeedbackMode mode, String feedback) {
		Platform.runLater(() -> {
			switch (mode) {
			case LOADING:
				infoBarProgress.setProgress(-1.0);
				infoBarProgress.setVisible(true);
				break;
			default:
				infoBarProgress.setVisible(false);
				break;
			}
			infoBarLabel.setText(feedback);
		});
	}

	/**
	 * Sets the {@link #infoBarProgress} to the first parameter and displays the given
	 * <code>feedback</code> string in {@link #infoBarLabel}.
	 *
	 * @param progress
	 *            the progress level to set the {@link ProgressBar} to. Should meet the requirements
	 *            of {@link ProgressBar#setProgress(double)}.
	 * @param feedback
	 *            the message to show to the user.
	 */
	@Override
	public void progressFeedback(double progress, String feedback) {
		Platform.runLater(() -> {
			infoBarProgress.setProgress(progress);
			infoBarProgress.setVisible(true);
			infoBarLabel.setText(feedback);
		});
	}

	/**
	 * Simply calls <code>MainController.getInstance().closeProgramme();</code>.
	 *
	 * @param event
	 *            the corresponding action event.
	 */
	@FXML
	private void closeProgramme(ActionEvent event) {
		MainController.getInstance().closeProgramme();
	}

	/**
	 * Start the transcription process if {@link KeyCode#ENTER} was typed into the
	 * {@link #getInputField()}.
	 *
	 * @param event
	 *            the <code>KeyEvent</code>.
	 */
	@FXML
	private void onInputReceived(KeyEvent event) {
		if (event.getCode().equals(KeyCode.ENTER)) {
			event.consume();
			startTranscription();
		}
	}

	/**
	 * Starts a new transcription process from the Transcribe button.
	 *
	 * @param event
	 *            the <code>ActionEvent</code>.
	 */
	@FXML
	private void onStartTranscription(ActionEvent event) {
		startTranscription();
	}

	/**
	 * Copies any transcription present in the {@link #outputField} to the clipboard.
	 *
	 * @param event
	 *            the <code>ActionEvent</code>.
	 */
	@FXML
	private void onCopyTranscription(ActionEvent event) {

		logger.info("Copying transcription to clipboard ...");

		StringBuilder builder = new StringBuilder();
		for (Node node : outputField.getChildren()) {
			if (node instanceof TranscriptionSegment) {
				for (Node wordbutton : ((TranscriptionSegment) node).getButtonBox().getChildren()) {
					if (wordbutton instanceof Button) {
						builder.append(((Button) wordbutton).getText() + " ");
					}
				}
			}
		}

		// Copy everything to the clipboard.
		Clipboard clipboard = Clipboard.getSystemClipboard();
		ClipboardContent content = new ClipboardContent();
		content.putString(builder.toString());
		clipboard.setContent(content);
	}

	/**
	 * Shows the {@link EditDatabaseDialogue} if the user can provide the required
	 * {@link LoginCredentials}.
	 *
	 * @param event
	 *            the <code>ActionEvent</code>.
	 */
	@FXML
	private void showEditDatabaseDialogue(ActionEvent event) {

		// Are we in offline mode? No need to worry about logging in then.
		if (Statics.parseBoolean(MainController.getProperty("database.offline_mode"))) {
			try {
				new EditDatabaseDialogue(new OfflineHandler()).showAndWait();
			} catch (IOException | URISyntaxException e) {
				logger.log(Level.WARNING, "Unable to open Add/Edit Word dialogue!", e);
			}
			return;
		}

		// Prepare the login routine.
		MainController instance = MainController.getInstance();
		OnlineHandler handler = OnlineHandler.getInstance();
		Optional<LoginCredentials> tempCredentials;
		boolean firstTry = true;
		boolean authenticated = false;

		do {

			if (!firstTry) {

				// Show the login dialogue.
				tempCredentials = new LoginDialogue(instance.getLoginCredentials().getUsername()).showAndWait();

				// Has the user cancelled the operation?
				if (!tempCredentials.isPresent()) {
					logger.info("The user has cancelled the login.");
					return;
				}

				// Update the login credentials in the main controller instance.
				logger.info("The following login credentials have been read:\n" + tempCredentials.get());
				instance.updateLoginCredentials(tempCredentials.get().getUsername(),
						tempCredentials.get().getPassword());
			}

			// Try to authenticate the user.
			try {
				authenticated = handler.isAuthenticationConfirmed();
			} catch (Exception e) {
				if (!firstTry) {
					logger.log(Level.WARNING, "Exception while authenticating user!", e);
					Alert warning = new Alert(AlertType.WARNING);
					warning.setTitle("Login failed!");
					warning.setHeaderText(null);
					warning.setContentText("Unable to login! Make sure ...\n\n- your login details are correct.\n"
							+ "- you are connected to the internet.\n- the proper database server url is stored in the settings panel.");
					warning.showAndWait();
				}
			}

			firstTry = false;

		} while (!authenticated);

		// All good it seems! We are ready to operate on the database.
		try {
			new EditDatabaseDialogue(handler).showAndWait();
		} catch (IOException | URISyntaxException e) {
			logger.log(Level.WARNING, "Unable to open Add/Edit Word dialogue!", e);
		}
	}

	/**
	 * Calls {@link OnlineHandler#reloadDatabase()} in order to update the local database with the
	 * latest online updates.
	 *
	 * @param event
	 *            the <code>ActionEvent</code>.
	 */
	@FXML
	private void reloadDatabase(ActionEvent event) {
		logger.info("Reloading database ...");
		OnlineHandler.getInstance().reloadDatabase();
	}

	/**
	 * Shows the settings stage.
	 *
	 * @param event
	 *            the <code>ActionEvent</code>.
	 */
	@FXML
	private void showSettings(ActionEvent event) {
		MainController.getInstance().showSettings();
	}

	/**
	 * Shows the "Quickstart" screen.
	 *
	 * @param event
	 *            the <code>ActionEvent</code>.
	 */
	@FXML
	private void showQuickstart(ActionEvent event) {

		Alert about = new Alert(AlertType.INFORMATION);
		about.setTitle("Text2Transcription " + MainController.VERSION);
		about.setHeaderText("Quickstart Guide");
		about.setContentText("In order to start transcribing, simply type in a word or sentence and press "
				+ "ENTER. Alternatively, you may also click the \"Transcribe\" button.\n\nOnce "
				+ "your transcription is completed, you can adjust it. This means that it is "
				+ "possible, for example, to change between strong and weak forms, different "
				+ "varieties, word classes or simply choose another transcription.\n\nShould an "
				+ "item happen to display \"Unknown\" instead of phonetic symbols, this means "
				+ "that there is no transcription for that word in the database. In that case, "
				+ "you're able to provide it yourself via \"Free Input\" from the context menu if "
				+ "you wish to do so.\n\nAs soon as you have corrected everything to your liking, "
				+ "you can use \"Copy to Transcription\" to copy the entire phonetic transcription "
				+ "to the clipboard. It is then possible to paste it into another document (via ctrl + v).");
		about.showAndWait();
	}

	/**
	 * Shows the "About" screen.
	 *
	 * @param event
	 *            the <code>ActionEvent</code>.
	 */
	@FXML
	private void showAbout(ActionEvent event) {
		Alert about = new Alert(AlertType.INFORMATION);
		about.setTitle("Text2Transcription " + MainController.VERSION);
		about.setHeaderText(null);
		about.setContentText("Text2Transcription (" + MainController.VERSION + ")\n\n"
				+ "University of Paderborn\nA project of the English and American Studies Department.\n"
				+ "Author: Michel May (michel-may@gmx.de)");
		about.showAndWait();
	}

	/**
	 * A convenience method to start a new transcription inside a seperate {@link Thread}.
	 */
	private void startTranscription() {
		new Thread(() -> {
			try {
				transcriber.transcribe(inputField.getText().trim());
				generalFeedback(FeedbackMode.SUCCESS, "Transcription successful!");
			} catch (Exception e) {
				generalFeedback(FeedbackMode.ERROR, "Error while transcribing! :( " + e);
				logger.log(Level.SEVERE, "Exception while transcribing input!", e);
			}
		}).start();
	}

	/* Getters and Setters */
	/**
	 * A getter for the {@link #inputField}.
	 *
	 * @return the {@link #inputField}.
	 */
	public TextArea getInputField() {
		return inputField;
	}

	/**
	 * A getter for the {@link #outputField}.
	 *
	 * @return the {@link #outputField}.
	 */
	public FlowPane getOutputField() {
		return outputField;
	}
}
