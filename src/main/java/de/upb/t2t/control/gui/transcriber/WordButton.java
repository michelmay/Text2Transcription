package de.upb.t2t.control.gui.transcriber;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.upb.t2t.control.database.editing.SingleWordStage;
import de.upb.t2t.model.DatabaseEntry;
import de.upb.t2t.model.TranscriptionItem;
import de.upb.t2t.model.Variety;
import de.upb.t2t.model.WordClass;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * An implementation of the {@link Button} class. <code>WordButton</code>s hold a
 * {@link DatabaseEntry} object whose {@link TranscriptionSegment}s they display as their text
 * property - one at a time. If the user wishes, he may click on the button to display its context
 * menu from which they can choose either a different item or adding a new one via the "Free Input"
 * option.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 * @see TranscriptionSegment
 * @see TranscriptionMenuItem
 */
public class WordButton extends Button {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** The button's database entry attribute. */
	private DatabaseEntry data;
	/**
	 * The button's listener responsible for updating the button should a new
	 * {@link TranscriptionItem} be selected.
	 */
	private SelectedItemListener selectionListener;

	/* Constructors */
	/**
	 * Constructor for the {@link WordButton} class.
	 *
	 * @param data
	 *            the corresponding <code>DatabaseEntry</code> object.
	 */
	public WordButton(DatabaseEntry data) {

		this.data = data;
		selectionListener = new SelectedItemListener();

		// Add the CSS style class and set the button's initial configuration.
		getStyleClass().add("transcriptionButton");
		updateDatabaseEntry(data);

		// Set set behaviour that will facilitate the user's editing process in case of word class
		// conflicts.
		setOnKeyPressed((KeyEvent e) -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				getContextMenu().show(WordButton.this, Side.BOTTOM, 0.0, 0.0);
			}
		});

		focusedProperty()
				.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
					if (!newValue.booleanValue()) {
						Platform.runLater(() -> {
							getStyleClass().remove("conflict");
						});
					}
				});
	}

	/* Object Methods */
	/**
	 * Should be called when the {@link DatabaseEntry} object associated with this button changes.
	 * This might happen, for example, when the user provides a free input. However, since database
	 * entry instances are not meant to be updated, this function will simply replace the old
	 * {@link #data} object with the new one. While doing so, it will furthermore unregister the
	 * {@link #selectionListener} from the old entry's {@link #selectedItemProperty()} and instead
	 * add it to the new one's.
	 *
	 * @param entry
	 *            the database entry to replace the previous one with
	 */
	public synchronized void updateDatabaseEntry(DatabaseEntry entry) {

		data.selectedItemProperty().removeListener(selectionListener);
		data = entry;
		data.selectedItemProperty().addListener(selectionListener);

		// Manually update the button in case there was only one item in the database entry and thus
		// the button would not have been updated.
		updateButton();
	}

	/**
	 * Updates the button's text property, CSS and context menu.
	 */
	private void updateButton() {
		Platform.runLater(() -> {
			if (data.isEmpty()) {
				setText("Unknown");
			} else {
				setText(data.getSelectedItem().getPhoneticString());
				setContentWordCSS(data.getSelectedItem().isContentWord());
			}
			createAndSetContextMenu();
		});
	}

	/**
	 * A convenience method create and set a new context menu for this button.
	 */
	private void createAndSetContextMenu() {

		ContextMenu contextMenu = new ContextMenu();
		Menu tempMenuWordClass, tempMenuVariety;

		// Add all transcription items potentially held by the databse entry.
		Map<WordClass, Map<Variety, List<TranscriptionItem>>> matchedItems = data.getMatchedItems();
		for (Entry<WordClass, Map<Variety, List<TranscriptionItem>>> wordClassEntry : matchedItems.entrySet()) {
			tempMenuWordClass = new Menu(wordClassEntry.getKey().getProperName());
			for (Entry<Variety, List<TranscriptionItem>> varietyEntry : wordClassEntry.getValue().entrySet()) {
				tempMenuVariety = new Menu(varietyEntry.getKey().getAbbreviation());
				for (TranscriptionItem item : varietyEntry.getValue()) {
					tempMenuVariety.getItems().add(new TranscriptionMenuItem(item, this));
				}
				tempMenuWordClass.getItems().add(tempMenuVariety);
			}
			contextMenu.getItems().add(tempMenuWordClass);
		}

		// Add the free input item.
		MenuItem freeInput = new MenuItem("Free Input");
		freeInput.setOnAction((ActionEvent event) -> {
			Platform.runLater(() -> {
				try {
					SingleWordStage addItemStage = new SingleWordStage(data.getLemma(), this);
					addItemStage.showAndWait();
				} catch (IOException | URISyntaxException e) {
					Logger.getGlobal().log(Level.WARNING, "Unable to show SingleWordStage!", e);
				}
			});
		});
		contextMenu.getItems().add(freeInput);

		setContextMenu(contextMenu);

		// Register a listener to the button so that the context menu gets displayed.
		setOnAction((ActionEvent event) -> {
			contextMenu.show(WordButton.this, Side.BOTTOM, 0.0, 0.0);
		});
	}

	/* Getters and Setters */
	/**
	 * A convenience getter for the {@link DatabaseEntry#matchesMultipleWordClasses()} getter.
	 *
	 * @return <code>data.matchesMultipleWordClasses()</code>.
	 */
	public boolean isInWordClassConflict() {
		return data.matchesMultipleWordClasses();
	}

	/**
	 * A convenience getter for the {@link #data} attribute's lemma attribute.
	 *
	 * @return the <code>data</code> attribute's lemma attribute.
	 */
	public String getLemma() {
		return data.getLemma();
	}

	/**
	 * A convenience getter for the {@link #data} attribute's selected
	 * <code>TransctiptionItem</code>.
	 *
	 * @return <code>data</code> attribute's selected <code>TransctiptionItem</code>.
	 */
	public TranscriptionItem getSelectedItem() {
		return data.getSelectedItem();
	}

	/**
	 * A convenience getter for the {@link #data} attribute's selected
	 * <code>TransctiptionItem</code> property.
	 *
	 * @return <code>data</code> attribute's selected <code>TransctiptionItem</code> property.
	 */
	public SimpleObjectProperty<TranscriptionItem> selectedItemProperty() {
		return data.selectedItemProperty();
	}

	/**
	 * A getter for the {@link #data} attribute.
	 *
	 * @return the <code>data</code> attribute.
	 */
	public DatabaseEntry getData() {
		return data;
	}

	/**
	 * A convenience method that sets the word class CSS of this button depending on the given
	 * parameter.
	 *
	 * @param contentWord
	 *            if <code>true</code> then the <code>contentWord</code> CSS class will be added;
	 *            otherwise <code>functionWord</code> will be set.
	 */
	private void setContentWordCSS(boolean contentWord) {
		Platform.runLater(() -> {
			ObservableList<String> cssClasses = getStyleClass();
			cssClasses.removeAll("contentWord", "functionWord");
			getStyleClass().add(contentWord ? "contentWord" : "functionWord");
		});
	}

	/* Sub-Classes */
	/**
	 * An implementation of the {@link ChangeListener} interface. Its use is to update the
	 * {@link WordButton} depending on what {@link TranscriptionItem} is selected.
	 *
	 * @author Michel May (michel-may@gmx.de)
	 *
	 */
	private class SelectedItemListener implements ChangeListener<TranscriptionItem> {

		/**
		 * Calls {@link WordButton#updateButton()}.
		 *
		 * @param observable
		 *            the observable value.
		 * @param oldValue
		 *            the old value.
		 * @param newValue
		 *            the new value.
		 */
		@Override
		public void changed(ObservableValue<? extends TranscriptionItem> observable, TranscriptionItem oldValue,
				TranscriptionItem newValue) {
			updateButton();
		}
	}
}
