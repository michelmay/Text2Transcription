/**
 *
 */
package de.upb.t2t.control.database.editing;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.upb.t2t.control.database.AbstractSQLiteBridge;
import de.upb.t2t.control.gui.DbEditingUserFeedback;
import de.upb.t2t.control.gui.transcriber.WordButton;
import de.upb.t2t.gui.fxml.FXMLProvider;
import de.upb.t2t.model.DatabaseEntry;
import de.upb.t2t.model.FeedbackMode;
import de.upb.t2t.model.TranscriptionItem;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * <p>
 * This small undecorated, dialogue-like stage provides simple offline editing functionality of a
 * single {@link TranscriptionItem}. It features the {@link AddTranscriptionPane} and a
 * {@link ToolBar} on its bottom side. Said functionality is not restricted to those users who are
 * authorised to modify the online database. Instead, everybody may employ this feature, for example
 * by selecting the 'Free Input' option in a {@link WordButton}'s context menu.
 * </p>
 * <p>
 * <b>Important:</b> As already stated, the editing capabilities of this class are strictly limited
 * to offline usage. This means that any alteration of the database will be overwritten as soon as
 * it is updated the next time.
 * </p>
 *
 * @author Michel May (michel-may@gmx.de)
 * @see OfflineHandler
 * @see AddTranscriptionPane
 */
public class SingleWordStage extends Stage implements DbEditingUserFeedback {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** A reference to the word button that opened this stage. */
	private WordButton button;
	/**
	 * A convenience reference to the {@link AddTranscriptionPane} in the centre of this stage's
	 * border pane.
	 */
	private AddTranscriptionPane transcriptionPane;
	/** The toolbar at the bottom of this stage giving feedback to the user. */
	@FXML
	private ToolBar toolbar;

	/* Constructors */
	/**
	 * Constructor for the {@link SingleWordStage}. It will call the alternate constructor with a
	 * new (empty) {@link TranscriptionItem}.
	 *
	 * @param lemma
	 *            the lemma to work with.
	 * @param button
	 *            a reference to the button that opened this stage.
	 * @throws IOException
	 *             if the {@link FXMLLoader} throws one.
	 * @throws URISyntaxException
	 *             if the {@link FXMLProvider} throws one.
	 */
	public SingleWordStage(String lemma, WordButton button) throws IOException, URISyntaxException {
		this(new TranscriptionItem(lemma), button);
	}

	/**
	 * Constructor for the {@link SingleWordStage} class. It will create a new, undecorated stage
	 * and populate it the corresponding {@link AddTranscriptionPane} and a bottom {@link ToolBar}.
	 *
	 * @param item
	 *            the transcription item to work with.
	 * @param button
	 *            a reference to the button that openend this stage.
	 * @throws IOException
	 *             if the {@link FXMLLoader} throws one.
	 * @throws URISyntaxException
	 *             if the {@link FXMLProvider} throws one.
	 */
	public SingleWordStage(TranscriptionItem item, WordButton button) throws IOException, URISyntaxException {

		this.button = button;

		// Fill the stage.
		FXMLLoader loader = new FXMLLoader(new FXMLProvider().getResourceAsURL("SingleWordPane.fxml"));
		loader.setController(this);
		BorderPane root = loader.load();
		transcriptionPane = new AddTranscriptionPane(item, new OfflineHandler(), this, false);
		root.setCenter(transcriptionPane);
		setScene(new Scene(root));

		// Undecorate the stage and make it hide itself as soon as it looses focus or ESCAPE is
		// pressed.
		initStyle(StageStyle.UNDECORATED);
		focusedProperty()
				.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
					if (!newValue.booleanValue()) {
						hide();
					}
				});
		addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent e) -> {
			if (e.getCode().equals(KeyCode.ESCAPE)) {
				hide();
			}
		});
	}

	/* Object Methods */
	@Override
	public void generalFeedback(FeedbackMode mode, String feedback) {
		Platform.runLater(() -> {
			ObservableList<Node> feedbackItems = toolbar.getItems();
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
		Platform.runLater(() -> {
			ObservableList<Node> feedbackItems = toolbar.getItems();
			feedbackItems.clear();
			feedbackItems.add(new Label("Item successfully added to the local database! " + item.getLemma() + " -> /"
					+ item.getPhoneticString() + "/ (" + item.getWordClass().getAbbreviation() + ", "
					+ item.getVariety().getAbbreviation() + ", " + item.getTransType().getAbbreviation() + ", id: "
					+ item.getId() + ")"));
			try {
				DatabaseEntry entry = AbstractSQLiteBridge.getInstance()
						.queryDatabase(transcriptionPane.getTranscriptionItem().getLemma());
				entry.getTranscriptionItemById(item.getId()).select();
				button.updateDatabaseEntry(entry);
			} catch (Exception e) {
				generalFeedback(FeedbackMode.ERROR,
						"An error has occurred while reloading this transcription button! :(");
				Logger.getLogger(getClass().getName()).log(Level.WARNING,
						"Unable to query database after item has already been added!", e);
			}
		});
	}

	@Override
	public void itemUpdated(TranscriptionItem item) {
		Platform.runLater(() -> {
			ObservableList<Node> feedbackItems = toolbar.getItems();
			feedbackItems.clear();
			feedbackItems
					.add(new Label("Item successfully updated! " + item.getLemma() + " -> /" + item.getPhoneticString()
							+ "/ (" + item.getWordClass().getAbbreviation() + ", " + item.getVariety().getAbbreviation()
							+ ", " + item.getTransType().getAbbreviation() + ", id: " + item.getId() + ")"));
			try {
				DatabaseEntry entry = AbstractSQLiteBridge.getInstance()
						.queryDatabase(transcriptionPane.getTranscriptionItem().getLemma());
				button.updateDatabaseEntry(entry);
			} catch (Exception e) {
				generalFeedback(FeedbackMode.ERROR,
						"An error has occurred while reloading this transcription button! :(");
				Logger.getLogger(getClass().getName()).log(Level.WARNING,
						"Unable to query database after item has already been updated!", e);
			}
		});
	}

	@Override
	public void itemRemoved(TranscriptionItem item) {
		throw new UnsupportedOperationException();
	}

	/* Getters & Setters */
}
