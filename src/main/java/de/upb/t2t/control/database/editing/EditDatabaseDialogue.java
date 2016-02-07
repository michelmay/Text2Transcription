package de.upb.t2t.control.database.editing;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.MatchResult;

import de.upb.t2t.control.MainController;
import de.upb.t2t.control.Statics;
import de.upb.t2t.control.database.AbstractSQLiteBridge;
import de.upb.t2t.control.gui.FeedbackIndicatorController;
import de.upb.t2t.gui.fxml.FXMLProvider;
import de.upb.t2t.control.gui.DbEditingUserFeedback;
import de.upb.t2t.model.DatabaseEntry;
import de.upb.t2t.model.FeedbackMode;
import de.upb.t2t.model.TranscriptionItem;
import de.upb.t2t.model.Variety;
import de.upb.t2t.model.WordClass;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * <p>
 * An window that handles the editing of the database. For reasons of modality, it extends the
 * {@link Dialog} class, but does not return anything. As every modification of the database will be
 * immediately uploaded to the online database, any user who wishes to use this functionality must
 * authorise themselves in advance.
 * </p>
 * <center><b><font size="2.0em">Any unauthorised user must not have access to this
 * dialogue!</font></b></center>
 * <p>
 * Modifying the database via this graphical interface should be preferred over the use of third
 * party tools (phpMyAdmin, ...) as it implements validity features that will prevent faulty input.
 * </p>
 *
 * @author Michel May (michel-may@gmx.de)
 *
 * @see AddTranscriptionPane
 * @see OnlineHandler
 * @see LoginDialogue
 */
public class EditDatabaseDialogue extends Dialog<Void> {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** The dialogue's logger object. */
	private Logger logger;
	/** The dialgogue's database handler. Responsible for communciation with the database. */
	private AbstractDatabaseEditingHandler handler;
	/** The text field into which the user types the lemma they wish to edit. */
	@FXML
	private TextField lemmaField;
	/** The ok button the user presses after entering the lemma they wish to edit. */
	@FXML
	private Button lemmaButton;
	/** The label giving the user feedback about whether or not their lemma input was valid. */
	@FXML
	private Label lemmaFeedbackLabel;
	/**
	 * The ProgressIndicator showing the status of the database query.
	 *
	 * @see #inputValidityIconController
	 */
	@FXML
	private Pane inputValidityIcon;
	/** The {@link #inputValidityIcon}'s controller instance. */
	@FXML
	private FeedbackIndicatorController inputValidityIconController;
	/**
	 * The VBox inside the {@link ScrollPane} underneath the AddTranscriptionHeadline. All the
	 * AddTranscriptionPanes go into this box.
	 */
	@FXML
	private VBox itemsBox;
	/** The toolbar at the bottom of the {@link BorderPane} used to display feedback to the user. */
	@FXML
	private ToolBar bottomToolbar;
	/**
	 * The list view on the right side of the split pane containing all lemmata that have been
	 * edited during this session.
	 */
	@FXML
	private ListView<String> lemmataList;
	/**
	 * This variable is necessary to avoid infinite looping when a new {@link AddTranscriptionPane}
	 * is added to the {@link #itemsBox}. It functions as a token that prevents the change listener
	 * from calling itself.
	 *
	 * @see #transPaneSortingLock
	 */
	private boolean allowTransPaneSorting;
	/**
	 * This lock is necessary to avoid infinite looping when a new {@link AddTranscriptionPane} is
	 * added to the {@link #itemsBox}.
	 *
	 * @see #allowTransPaneSorting
	 */
	private ReentrantLock transPaneSortingLock;

	/* Constructors */
	/**
	 * Constructor for the {@link EditDatabaseDialogue}.
	 *
	 * @param handler
	 *            the handler to use for communicating with the database.
	 * @throws IOException
	 *             if the {@link FXMLLoader} throws one.
	 * @throws URISyntaxException
	 *             if the {@link FXMLProvider} throws one.
	 */
	public EditDatabaseDialogue(AbstractDatabaseEditingHandler handler) throws IOException, URISyntaxException {

		// Instantiate the logger.
		logger = Logger.getLogger(getClass().getName());
		this.handler = handler;
		String username = handler instanceof OnlineHandler
				? MainController.getInstance().getLoginCredentials().getUsername() : "offline";
		allowTransPaneSorting = true;
		transPaneSortingLock = new ReentrantLock(true);

		// Load the GUI and set its properties as required.
		FXMLLoader loader = new FXMLLoader(new FXMLProvider().getResourceAsURL("EditWordPane.fxml"));
		loader.setController(this);
		SplitPane root = (SplitPane) loader.load();

		Platform.runLater(() -> {
			getDialogPane().setContent(root);
			getDialogPane().getButtonTypes().add(new ButtonType("Done", ButtonData.OK_DONE));
			setResizable(true);
			setTitle("Database Editing (" + username + ")");
			setX(Double.parseDouble(MainController.getProperty("gui.addWord.x")));
			setY(Double.parseDouble(MainController.getProperty("gui.addWord.y")));
			setWidth(Double.parseDouble(MainController.getProperty("gui.addWord.width")));
			setHeight(Double.parseDouble(MainController.getProperty("gui.addWord.height")));

			// Make sure that pressing ENTER or ESCAPE does not close the dialogue.
			root.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
				KeyCode code = event.getCode();
				if (code.equals(KeyCode.ENTER) || code.equals(KeyCode.ESCAPE)) {
					event.consume();
				}
			});

			// Register listeners to store width/height properties for the next session.
			xProperty()
					.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
				MainController.setProperty("gui.addWord.x", newValue.doubleValue() + "");
			});
			yProperty()
					.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
				MainController.setProperty("gui.addWord.y", newValue.doubleValue() + "");
			});
			widthProperty()
					.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
				MainController.setProperty("gui.addWord.width", newValue.doubleValue() + "");
			});
			heightProperty()
					.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
				MainController.setProperty("gui.addWord.height", newValue.doubleValue() + "");
			});

			// Register a listener to the items box.
			final ObservableList<Node> children = itemsBox.getChildren();
			children.addListener((javafx.collections.ListChangeListener.Change<? extends Node> c) -> {

				transPaneSortingLock.lock();
				try {

					// Has the listener called itself (via #setAll())? Avoid looping then.
					if (!allowTransPaneSorting) {
						allowTransPaneSorting = true;
						return;
					}

					Platform.runLater(() -> {

						/*
						 * In order to avoid IllegalArgument exceptions, make a copy of the
						 * currently added child nodes, sort them and then override the itemsBox'
						 * children with that list. Credits to this thread:
						 * https://stackoverflow.com/questions/18667297/javafx- changing-order-of-
						 * children-in-a-flowpane
						 */
						ObservableList<Node> sortingCollection = FXCollections
								.observableArrayList(itemsBox.getChildren());

						Collections.sort(sortingCollection, (Node node1, Node node2) -> {

							// No AddTranscriptionPane for some reason?
							if (!(node1 instanceof AddTranscriptionPane && node2 instanceof AddTranscriptionPane)) {
								return 0;
							}

							int id1 = ((AddTranscriptionPane) node1).getTranscriptionItem().getId();
							int id2 = ((AddTranscriptionPane) node2).getTranscriptionItem().getId();

							// Sort empty item panes to the end.
							if (id1 < 0) {
								return 1;
							}
							if (id2 < 0) {
								return -1;
							}

							// Finally compare the ids.
							if (id1 < id2) {
								return -1;
							}
							if (id1 == id2) {
								return 0;
							}
							return 1;
						});

						allowTransPaneSorting = false;
						itemsBox.getChildren().setAll(sortingCollection);
					});
				} finally {
					transPaneSortingLock.unlock();
				}
			});

			// Register a listener to the lemmata list.
			MultipleSelectionModel<String> model = lemmataList.getSelectionModel();
			model.setSelectionMode(SelectionMode.SINGLE);
			model.selectedItemProperty()
					.addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
				Platform.runLater(() -> {
					lemmaField.setText(newValue);
					lemmaField.fireEvent(new KeyEvent(lemmataList, lemmaField, KeyEvent.KEY_PRESSED, "",
							"Lemma selected from list.", KeyCode.ENTER, false, false, false, false));
				});
			});
		});

	}

	/* Object Methods */
	/**
	 * Should be called when a key is pressed inside the {@link #lemmaField}. If the {@link KeyCode}
	 * belonging to the given key event equals ENTER or TAB, {@link #lemmaButton} is fired for user
	 * convenience.
	 *
	 * @param event
	 *            the corresponding key event.
	 */
	@FXML
	private void onInputReceived(KeyEvent event) {
		if (event.getCode().equals(KeyCode.ENTER) || event.getCode().equals(KeyCode.TAB)) {
			lemmaButton.fire();
			// Make sure the "Done" button is not triggered and thus the whole dialogue closes.
			event.consume();
		}
	}

	/**
	 * Should be called when the {@link #lemmaButton} fires. Starts the editing process for the
	 * currently typed in lemma unless there are for example illegal characters found inside of it
	 * or any exceptions are thrown database-wise.
	 *
	 * @param event
	 *            the corresponding action event.
	 */
	@FXML
	private void onOkButtonPressed(ActionEvent event) {

		// Let the user know we're working on it.
		setLemmaFeedback(FeedbackMode.LOADING, "");

		// Check if we were presented with valid input.
		String input = lemmaField.getText().trim();
		if (!validateInput(input)) {
			setLemmaFeedback(FeedbackMode.INVALID, "Illegal characters found!");
			return;
		}

		// Check if we already have something in our database for that lemma.
		try {
			DatabaseEntry entry = AbstractSQLiteBridge.getInstance().queryDatabase(input);
			setLemmaFeedback(FeedbackMode.SUCCESS,
					entry.isEmpty() ? "Ready to create a new entry." : "Corresponding entry found in database.");
			populateEditingPane(entry);

			// Add the lemma to the list of those that we have edited in this session.
			ObservableList<String> lemmata = lemmataList.getItems();
			boolean addToList = true;
			for (String lemma : lemmata) {
				if (lemma.equalsIgnoreCase(input)) {
					// Lemma already listed.
					addToList = false;
					break;
				}
			}
			if (addToList) {
				Platform.runLater(() -> {
					lemmata.add(input);
				});
			}

		} catch (SQLException e) {
			logger.log(Level.WARNING, "Unable to lookup input: \"" + input + "\"", e);
			setLemmaFeedback(FeedbackMode.ERROR, "Error while looking up lemma in database! :(");
		} catch (IOException e) {
			logger.log(Level.WARNING, "IOException while creating AddTranscriptionPane(s)!", e);
			setLemmaFeedback(FeedbackMode.ERROR, "Error while preparing the editor! :(");
		} catch (Exception e) {
			logger.log(Level.WARNING, "Exception while creating AddTranscriptionPane(s)!", e);
			setLemmaFeedback(FeedbackMode.ERROR, "Error while preparing the editor! :(");
		}
	}

	/**
	 * Determines whether or not the given <code>String</code> can function as a valid lemma. This
	 * means that it must not containt any non-word characters or digits.
	 *
	 * @param input
	 *            the <code>String</code> to evaluate.
	 * @return <code>true</code> if the given <code>String</code> is a valid lemma;
	 *         <code>false</code> otherwise.
	 */
	private boolean validateInput(String input) {

		logger.fine("Validating input \"" + input + "\" ...");
		List<MatchResult> illegalChas = Statics.getAllRegexMatches(input, "[\\W\\d_&&[^'-]]");
		if (!illegalChas.isEmpty()) {
			String logString = "Non-word characters found in input!";
			for (MatchResult matchResult : illegalChas) {
				logString += "\n\t\"" + matchResult.group() + "\"";
			}
			logger.fine(logString);
			return false;
		}
		return true;
	}

	/**
	 * Populates the {@link #itemsBox} with the appropriate <code>AddTranscriptionPane</code>s if
	 * the given <code>DatabaseEntry</code> is not empty. Otherwise, an empty add transcription pane
	 * is added at least.
	 *
	 * @param entry
	 *            the database entry to populate the items box with.
	 * @throws IOException
	 *             if any call to the <code>AddTranscriptionPane</code> constructor should throw
	 *             one.
	 * @see AddTranscriptionPane
	 * @see DatabaseEntry
	 */
	private void populateEditingPane(DatabaseEntry entry) throws IOException {

		Platform.runLater(() -> {

			// Clear the editing area so that we can add items anew.
			ObservableList<Node> list = itemsBox.getChildren();
			list.clear();

			// If we already have items in the database entry, add them to
			// the editing area.
			for (Entry<WordClass, Map<Variety, List<TranscriptionItem>>> wcEntry : entry.getMatchedItems().entrySet()) {
				for (Entry<Variety, List<TranscriptionItem>> varEntry : wcEntry.getValue().entrySet()) {
					for (TranscriptionItem item : varEntry.getValue()) {
						try {
							itemsBox.getChildren()
									.add(new AddTranscriptionPane(item, handler, new FeedbackProvider(), true));
						} catch (IOException | URISyntaxException e) {
							logger.log(Level.WARNING, "Unable to create AddTranscriptionPane for item: " + item, e);
						}
					}
				}
			}
		});

		// Create an empty transcription pane so that the user may add more
		// items.
		addEmptyTranscriptionPane(entry.getLemma());

	}

	/**
	 * Adds an empty {@link AddTranscriptionPane} to the {@link #itemsBox}, meaning that the
	 * respective constructor will be called with a newly-created {@link DatabaseEntry} causing the
	 * transcription pane to be initialised with empty values.
	 *
	 * @param lemma
	 *            the lemma to use.
	 */
	private void addEmptyTranscriptionPane(String lemma) {
		Platform.runLater(() -> {
			try {
				itemsBox.getChildren().add(
						new AddTranscriptionPane(new TranscriptionItem(lemma), handler, new FeedbackProvider(), true));
			} catch (IOException | URISyntaxException e) {
				logger.log(Level.WARNING, "Unable to create empty AddTranscriptionPane!", e);
			}
		});
	}

	/**
	 * Removes the {@link AddTranscriptionPane} from the {@link #itemsBox} whose
	 * {@link TranscriptionItem}'s id matches the given one. If there is no such element, nothing
	 * will happen.
	 *
	 * @param transID
	 *            the ID of the transcription item that should be removed.
	 */
	private void removeTranscriptionPane(int transID) {
		ObservableList<Node> children = itemsBox.getChildren();
		for (Node child : children) {
			if (child instanceof AddTranscriptionPane
					&& ((AddTranscriptionPane) child).getTranscriptionItem().getId() == transID) {
				Platform.runLater(() -> {
					children.remove(child);
				});
				return;
			}
		}
	}

	/* Getters and Setters */
	/**
	 * Getter for the currently typed in lemma. Note that this function does not perform any
	 * validity check.
	 *
	 * @return the current lemma.
	 * @see #lemmaField
	 */
	public String getCurrentLemma() {
		return lemmaField.getText().trim();
	}

	/**
	 * Updates the {@link #lemmaFeedbackLabel} and {@link #inputValidityIcon} based on the given
	 * parameters. Should be called by instances of the {@link FeedbackProvider}.
	 *
	 * @param mode
	 *            the desired feedback mode.
	 * @param message
	 *            the message to be displayed.
	 */
	private void setLemmaFeedback(FeedbackMode mode, String message) {
		Platform.runLater(() -> {
			lemmaFeedbackLabel.setText(message);
			inputValidityIconController.setFeedbackMode(mode);
		});
	}

	/* Sub-Classes */
	/**
	 * A sub-class that takes care of the feedback for the user.
	 *
	 * @author Michel May (michel-may@gmx.de)
	 *
	 */
	private class FeedbackProvider implements DbEditingUserFeedback {

		@Override
		public void generalFeedback(FeedbackMode mode, String feedback) {

			/*
			 * Clear whatever old message may still be displayed and overwrite it with our new one.
			 * Since we're only displaying a little text label, we can ignore the feedback mode.
			 */
			Platform.runLater(() -> {
				ObservableList<Node> feedbackItems = bottomToolbar.getItems();
				feedbackItems.clear();
				feedbackItems.add(new Label(feedback));
			});
		}

		@Override
		public void progressFeedback(double progress, String feedback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void itemAdded(TranscriptionItem item) {
			generalFeedback(FeedbackMode.SUCCESS,
					"Item successfully added to the database! " + item.getLemma() + " -> /" + item.getPhoneticString()
							+ "/ (" + item.getWordClass().getAbbreviation() + ", " + item.getVariety().getAbbreviation()
							+ ", " + item.getTransType().getAbbreviation() + ", id: " + item.getId() + ")");
			addEmptyTranscriptionPane(item.getLemma());

		}

		@Override
		public void itemUpdated(TranscriptionItem item) {
			generalFeedback(FeedbackMode.SUCCESS,
					"Item successfully updated! " + item.getLemma() + " -> /" + item.getPhoneticString() + "/ ("
							+ item.getWordClass().getAbbreviation() + ", " + item.getVariety().getAbbreviation() + ", "
							+ item.getTransType().getAbbreviation() + ", id: " + item.getId() + ")");
		}

		@Override
		public void itemRemoved(TranscriptionItem item) {
			removeTranscriptionPane(item.getId());
			generalFeedback(FeedbackMode.SUCCESS, "Item successfully deleted from database! /"
					+ item.getPhoneticString() + "/ (" + item.getId() + ")");
		}

	}
}
