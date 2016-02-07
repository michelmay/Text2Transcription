/**
 *
 */
package de.upb.t2t.control.database.editing;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.MatchResult;

import de.upb.t2t.control.Statics;
import de.upb.t2t.control.gui.FeedbackIndicatorController;
import de.upb.t2t.control.gui.UserFeedbackProvider;
import de.upb.t2t.control.gui.DbEditingUserFeedback;
import de.upb.t2t.control.gui.phonetic_keyboard.PhoneticKeyboardHandler;
import de.upb.t2t.control.gui.transcriber.AbbreviableListCell;
import de.upb.t2t.gui.fxml.FXMLProvider;
import de.upb.t2t.model.Abbreviable;
import de.upb.t2t.model.FeedbackMode;
import de.upb.t2t.model.TranscriptionItem;
import de.upb.t2t.model.TranscriptionType;
import de.upb.t2t.model.Variety;
import de.upb.t2t.model.WordClass;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Callback;

/**
 * A small pane that may used in various locations throughout the application. Its purpose is adding
 * to, editing inside or removing from the database a single {@link TranscriptionItem}. The
 * communication with said database is achieved via an instance of
 * {@link AbstractDatabaseEditingHandler} handed to the constructor. Consequently, the editing may
 * take pane in an offline, online or any other fashion in which the handler is implemented.
 *
 * @author Michel May (michel-may@gmx.de)
 */
public class AddTranscriptionPane extends AnchorPane {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** The pane's logger instance. */
	Logger logger;
	/** The pane's <code>TranscriptionItem</code> property. */
	private SimpleObjectProperty<TranscriptionItem> item;
	/** The handler responsible for communicating with the database. */
	private AbstractDatabaseEditingHandler inputHandler;
	/** A feedback provider that the <code>AddTranscriptionPane</code> may send updates to. */
	private DbEditingUserFeedback feedbackProvider;
	/** The label displaying the {@link TranscriptionItem}'s database ID, if it has any yet. */
	@FXML
	private Label label;
	/**
	 * The <code>ComboBox</code> to choose the {@link TranscriptionItem}'s <code>WordClass</code>
	 * from.
	 */
	@FXML
	private ComboBox<WordClass> wordClassBox;
	/**
	 * The <code>ComboBox</code> to choose the {@link TranscriptionItem}'s <code>Variety</code>
	 * from.
	 */
	@FXML
	private ComboBox<Variety> varietyBox;
	/**
	 * The <code>ComboBox</code> to choose the {@link TranscriptionItem}'s
	 * <code>TranscriptionType</code> from.
	 */
	@FXML
	private ComboBox<TranscriptionType> transTypeBox;
	/** The input field that the user types their phonetic transcription into. */
	@FXML
	private TextField transcriptionInput;
	/** The Ok button the user may press once finished typing in the phonetic transcription. */
	@FXML
	private Button okButton;
	/**
	 * The Delete button the user may press should they want to remove the item from the database.
	 */
	@FXML
	private Button deleteButton;
	/**
	 * A <code>FeedbackIndicator</code> pane located behind the Delete button and will be loaded
	 * from the <code>FeedbackIndicator.fxml</code>.
	 */
	@FXML
	private Pane feedbackIndicator;
	/**
	 * The controller for the <code>FeedbackIndicator</code> pane. Please note that the name of this
	 * variable must follow the JavaFx conventions for nested controllers as it is automatically
	 * retrieved the {@link FXMLLoader}.
	 *
	 * @see <a href=
	 *      "https://docs.oracle.com/javafx/2/api/javafx/fxml/doc-files/introduction_to_fxml.html#nested_controllers">
	 *      Oracle Introduction to FXML - Nested Controllers</a>
	 */
	@FXML
	private FeedbackIndicatorController feedbackIndicatorController;
	private Tooltip ttFeedback;

	/* Constructors */
	/**
	 * Convenience constructor for the {@link AddTranscriptionPane} class. Simply calls
	 * {@link AddTranscriptionPane#AddTranscriptionPane(TranscriptionItem, AbstractDatabaseEditingHandler, DbEditingUserFeedback, boolean)}
	 * with an empty implementation of {@link UserFeedbackProvider}.
	 *
	 * @param item
	 *            the item to create / edit / remove.
	 * @param inputHandler
	 *            responsible for communication with the database.
	 * @param showDeleteButton
	 *            determines whether the {@link #deleteButton} will be visible or not. In some use
	 *            cases, deletion is not desired and therefore can effectively be prevented by
	 *            simply hiding the button.
	 * @throws IOException
	 *             if the {@link FXMLLoader} throws one.
	 * @throws URISyntaxException
	 *             if the {@link FXMLProvider} throws one.
	 */
	public AddTranscriptionPane(TranscriptionItem item, AbstractDatabaseEditingHandler inputHandler,
			boolean showDeleteButton) throws IOException, URISyntaxException {
		// Instantiate the pane with an empty feedback provider.
		this(item, inputHandler, new DbEditingUserFeedback() {

			@Override
			public void itemRemoved(TranscriptionItem item) {
			}

			@Override
			public void itemAdded(TranscriptionItem item) {
			}

			@Override
			public void itemUpdated(TranscriptionItem item) {
			}

			@Override
			public void generalFeedback(FeedbackMode mode, String feedback) {
			}

			@Override
			public void progressFeedback(double progress, String feedback) {
			}
		}, showDeleteButton);
	}

	/**
	 * Constructor for the {@link AddTranscriptionPane} class. It will initialise all its components
	 * appropriately based on the given parameters. This means that should the
	 * <code>TranscriptionItem</code> be empty, for example, the {@link #deleteButton} will
	 * automatically be disabled (as it has obviously not been retrieved from the database). In what
	 * way the database is modified depends on the type of {@link AbstractDatabaseEditingHandler}
	 * that is passed to this constructor.
	 *
	 * @param item
	 *            the item to create / edit / remove.
	 * @param inputHandler
	 *            responsible for communication with the database.
	 * @param feedbackProvider
	 *            any feedback will be sent to this object.
	 * @param showDeleteButton
	 *            determines whether the {@link #deleteButton} will be visible or not. In some use
	 *            cases, deletion is not desired and therefore can effectively be prevented by
	 *            simply hiding the button.
	 * @throws IOException
	 *             if the {@link FXMLLoader} throws one.
	 * @throws URISyntaxException
	 *             if the {@link FXMLProvider} throws one.
	 */
	public AddTranscriptionPane(TranscriptionItem item, AbstractDatabaseEditingHandler inputHandler,
			DbEditingUserFeedback feedbackProvider, boolean showDeleteButton) throws IOException, URISyntaxException {

		// Prepare the logger object and set the pane's parent.
		logger = Logger.getLogger(getClass().getName());
		this.item = new SimpleObjectProperty<TranscriptionItem>(item);
		this.inputHandler = inputHandler;
		this.feedbackProvider = feedbackProvider;

		// Register this instance as root and controller of the corresponding
		// TranscriptionPanel object.
		FXMLLoader fxmlLoader = new FXMLLoader(new FXMLProvider().getResourceAsURL("AddTranscriptionPane.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		fxmlLoader.load();

		transcriptionInput.textProperty()
				.addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
					checkInputValidity(newValue);
				});

		Platform.runLater(() -> {

			// Are we showing the delete button?
			deleteButton.setVisible(showDeleteButton);

			// Set the combo boxes' cell factory and populate them.
			wordClassBox.setCellFactory(new Callback<ListView<WordClass>, ListCell<WordClass>>() {

				@Override
				public ListCell<WordClass> call(ListView<WordClass> param) {
					return new AbbreviableListCell<WordClass>();
				}
			});
			wordClassBox.setItems(FXCollections.observableArrayList(WordClass.values()));
			wordClassBox.setButtonCell(new AbbreviableListCell<WordClass>());
			wordClassBox.requestFocus();
			varietyBox.setCellFactory(new Callback<ListView<Variety>, ListCell<Variety>>() {

				@Override
				public ListCell<Variety> call(ListView<Variety> param) {
					return new AbbreviableListCell<Variety>();
				}
			});
			varietyBox.setItems(FXCollections.observableArrayList(Variety.values()));
			varietyBox.setButtonCell(new AbbreviableListCell<Variety>());
			transTypeBox.setCellFactory(new Callback<ListView<TranscriptionType>, ListCell<TranscriptionType>>() {

				@Override
				public ListCell<TranscriptionType> call(ListView<TranscriptionType> param) {
					return new AbbreviableListCell<TranscriptionType>();
				}
			});
			transTypeBox.setItems(FXCollections.observableArrayList(TranscriptionType.values()));
			transTypeBox.setButtonCell(new AbbreviableListCell<TranscriptionType>());

			// Create the feedback tooltip.
			ttFeedback = new Tooltip();
			ttFeedback.setWrapText(true);
			ttFeedback.setPrefWidth(200.0);
			Tooltip.install(transcriptionInput, ttFeedback);
			Tooltip.install(feedbackIndicator, ttFeedback);

			// Register the required listeners to the input field.
			try {
				transcriptionInput.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
					// Fire the Ok button when pressing ENTER.
					if (event.getCode().equals(KeyCode.ENTER) && !okButton.isDisabled()) {
						Platform.runLater(() -> {
							okButton.fire();
						});
					}
				});
				transcriptionInput.addEventHandler(KeyEvent.KEY_TYPED, new PhoneticKeyboardHandler(transcriptionInput));
			} catch (IOException | URISyntaxException e) {
				logger.log(Level.WARNING,
						"Unable to register PhoneticKeyboardHandler with the transcription input field!", e);
			}
		});

		insertData();
	}

	/* Object Methods */
	/**
	 * Should be called whenever the user sends a key to one of the {@link ComboBox}es. For
	 * convenience, it will then try to find the next (or first) entry beginning with the typed
	 * letter.
	 *
	 * @param event
	 *            the <code>KeyEvent</code>.
	 */
	@SuppressWarnings("unchecked")
	@FXML
	private void onHotkeyPressed(KeyEvent event) {

		logger.fine("Hotkey pressed on combobox!");

		EventTarget target = event.getTarget();
		if (target instanceof ComboBox<?>) {

			// Prepare the required objects, including the class cast of the combo box.
			ComboBox<Abbreviable> comboBox;
			try {
				comboBox = (ComboBox<Abbreviable>) target;
			} catch (ClassCastException e) {
				logger.log(Level.WARNING, "Unable to cast to ComboBox<Abbreviable>!", e);
				return;
			}
			int index = comboBox.getSelectionModel().getSelectedIndex();
			int i = index;

			// Iterate over the list of items and select the next one.
			ObservableList<Abbreviable> items = comboBox.getItems();
			do {
				// If we have reached the end of the list, start at the beginning.
				if (++i >= items.size()) {
					i = 0;
				}

				// Check if the given item starts with the respective key. Keep the key code's upper
				// case in mind.
				if (items.get(i).getAbbreviation().toUpperCase().startsWith(event.getCode().toString())) {
					comboBox.getSelectionModel().select(i);
					break;
				}
			} while (i != index);

		}
	}

	/**
	 * Should be called when the {@link #okButton} is pressed. It will then check whether to create
	 * a new or update an existing {@link TranscriptionItem}, based on the input.
	 *
	 * @param event
	 *            the <code>ActionEvent</code>.
	 */
	@FXML
	private void onOkButtonPressed(ActionEvent event) {

		Platform.runLater(() -> {

			// Prepare all the objects we might need.
			WordClass wordClass = wordClassBox.getSelectionModel().getSelectedItem();
			Variety variety = varietyBox.getSelectionModel().getSelectedItem();
			TranscriptionType transType = transTypeBox.getSelectionModel().getSelectedItem();
			String transcription = transcriptionInput.getText().trim();
			TranscriptionItem tempItem = item.get();

			try {
				/*
				 * We have to consider two user cases here. The first of which would be creating an
				 * entirely new transcription item. Therefore, we must make sure we are not storing
				 * duplicates in the database.
				 */
				if (!tempItem.hasSqlID()) {

					try {

						logger.info("Creating new transcription item ...");

						tempItem = inputHandler.addTranscription(tempItem.getLemma(), wordClass, variety, transType,
								transcription);
						item.set(tempItem);
						label.setText("ID: " + tempItem.getId());
						feedbackIndicatorController.setFeedbackMode(FeedbackMode.SUCCESS);
						feedbackProvider.itemAdded(tempItem);

					} catch (IllegalArgumentException e) {

						logger.info("Dublicate entry found!");

						feedbackIndicatorController.setFeedbackMode(FeedbackMode.INVALID);
						feedbackProvider.generalFeedback(FeedbackMode.INVALID,
								"Duplicate entry found for: /" + transcription + "/ (" + wordClass.getAbbreviation()
										+ ", " + variety.getAbbreviation() + ", " + transType.getAbbreviation() + ")");
						deleteButton.setDisable(true);
						return;
					}

				} else {

					// The second use case is rather straight forward, as we'll simply try to update
					// an existing entry.
					logger.info("Updating transcription item (id: " + tempItem.getId() + ") ...");

					tempItem = inputHandler.updateTranscription(tempItem.getId(), wordClass, variety, transType,
							transcription);
					item.set(tempItem);
					feedbackIndicatorController.setFeedbackMode(FeedbackMode.SUCCESS);
					feedbackProvider.itemUpdated(tempItem);
				}

				// Whatever the case before, we'll have to enable the delete button.
				deleteButton.setDisable(false);

			} catch (SQLException e) {
				logger.log(Level.WARNING, "An SQLException has occurred while processing the user input!", e);
				feedbackIndicatorController.setFeedbackMode(FeedbackMode.ERROR);
				feedbackProvider.generalFeedback(FeedbackMode.ERROR, "An unexpected error has occurred! " + e);
			} catch (Exception e) {
				logger.log(Level.WARNING, "An unexpected exception has occurred while processing the user input!", e);
				feedbackIndicatorController.setFeedbackMode(FeedbackMode.ERROR);
				feedbackProvider.generalFeedback(FeedbackMode.ERROR, "An unexpected error has occurred! " + e);
			}
		});
	}

	/**
	 * Should be called when a key is pressed on either the {@link #okButton} or
	 * {@link #deleteButton}. The event will then be caught (to avoid any conflict with this pane's
	 * parent) the respective button fired manually instead.
	 *
	 * @param event
	 *            the <code>KeyEvent</code>.
	 */
	@FXML
	private void onKeyPressedOnButton(KeyEvent event) {

		// Consume the event and fire the respective button.
		if (event.getCode().equals(KeyCode.ENTER)) {
			event.consume();
			Platform.runLater(() -> {
				((Button) event.getTarget()).fire();
			});
			return;
		}
	}

	@FXML
	private void deleteItem(ActionEvent event) {

		// Ask the user for confirmation first.
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Delete Item");
		alert.setHeaderText("Confirm deletion");
		alert.setContentText("Are you sure you want to remove the following item?\n" + item);

		Optional<ButtonType> result = alert.showAndWait();
		if (!result.get().equals(ButtonType.OK)) {
			logger.info("The user cancelled the item's deletion.");
			feedbackProvider.generalFeedback(FeedbackMode.NONE, "");
			return;
		}

		// Remove the item.
		try {
			inputHandler.deleteTranscription(item.get().getId());
		} catch (SQLException e) {
			logger.log(Level.WARNING, "SQLException while deleting item!", e);
			feedbackProvider.generalFeedback(FeedbackMode.ERROR, "Unable to delete item from database! " + item);
			return;
		} catch (Exception e) {
			logger.log(Level.WARNING, "Unexpected exception while deleting item!", e);
			feedbackProvider.generalFeedback(FeedbackMode.ERROR, "Unable to delete item from database! " + item);
			return;
		}

		// Give the user feedback.
		feedbackProvider.itemRemoved(item.get());

		// Unset the item property just in case.
		item.set(null);
	}

	/**
	 * A convenience method to fill the {@link ComboBox}es and the {@link #transcriptionInput} with
	 * the data from the current {@link #transcriptionItemProperty()}.
	 */
	private void insertData() {
		Platform.runLater(() -> {

			TranscriptionItem tempItem = item.get();

			// Are we creating a new entry?
			if (!tempItem.hasSqlID()) {
				label.setText("ID: ?");
				wordClassBox.getSelectionModel().select(0);
				varietyBox.getSelectionModel().select(Variety.getPreferredVariety());
				transTypeBox.getSelectionModel().select(0);
				transcriptionInput.setText("");
			} else {
				label.setText("ID: " + tempItem.getId());
				wordClassBox.getSelectionModel().select(tempItem.getWordClass());
				varietyBox.getSelectionModel().select(tempItem.getVariety());
				transTypeBox.getSelectionModel().select(tempItem.getTransType());
				transcriptionInput.setText(tempItem.getPhoneticString());
				deleteButton.setDisable(false);
			}
		});
	}

	/**
	 * A convenience method to check whether the current input inside the
	 * {@link #transcriptionInput} is a valid phonetic transcription or not. Additionally, it will
	 * notify the user about how the evaluation turned out.
	 *
	 * @param input
	 *            the input <code>String</code> to evaluate.
	 */
	private void checkInputValidity(String input) {

		logger.fine("Checking input validity ...");

		input = input.trim();

		// Give the user feedback about the validity of their feedback starting with an empty input
		// string.
		if (input.isEmpty()) {
			Platform.runLater(() -> {
				okButton.setDisable(true);
				feedbackIndicatorController.setFeedbackMode(FeedbackMode.NONE);
				ttFeedback.setText("Please enter your transcription.");
			});
			return;
		}

		// Are there invalid characters inside the input?
		List<MatchResult> invalidChas = Statics.getAllRegexMatches(input,
				"[^" + Statics.REGEX_BROAD_PHONETIC_SYMBOLS + "]");
		if (!invalidChas.isEmpty()) {
			Platform.runLater(() -> {
				okButton.setDisable(true);
				feedbackIndicatorController.setFeedbackMode(FeedbackMode.INVALID);
				ttFeedback.setText("Invalid characters found!");
				String feedbackString = "The following invalid characters have been found: ";
				for (int i = 0; i < invalidChas.size(); i++) {
					feedbackString += "\"" + invalidChas.get(i).group() + "\""
							+ (i < invalidChas.size() - 1 ? "," : "");
				}
				feedbackProvider.generalFeedback(FeedbackMode.INVALID, feedbackString);
				return;
			});
			return;
		}

		// None of the above? Well all good then it seems.
		Platform.runLater(() -> {
			okButton.setDisable(false);
			feedbackIndicatorController.setFeedbackMode(FeedbackMode.NONE);
			ttFeedback.setText("Everything seems fine =)");
		});
	}

	/* Getters and Setters */
	/**
	 * A getter for the {@link #transcriptionItemProperty()}'s value.
	 *
	 * @return the <code>transcriptionItemProperty</code>'s value.
	 */
	public TranscriptionItem getTranscriptionItem() {
		return item.get();
	}

	/**
	 * A getter for the {@link #transcriptionItemProperty()}.
	 *
	 * @return the <code>transcriptionItemProperty</code>.
	 */
	public SimpleObjectProperty<TranscriptionItem> transcriptionItemProperty() {
		return item;
	}
}
