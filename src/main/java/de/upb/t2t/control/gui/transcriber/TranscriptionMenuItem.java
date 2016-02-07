/**
 *
 */
package de.upb.t2t.control.gui.transcriber;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.upb.t2t.model.TranscriptionItem;
import images.ImageProvider;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * An implementation of the {@link MenuItem} class that will function as a part of the context menu
 * of a {@link WordButton}. A <code>TranscriptionMenuItem</code> represents a
 * {@link TranscriptionItem} and should it be clicked, then the menu will call
 * {@link TranscriptionItem#select()}.
 *
 * @author Michel May (michel-may@gmx.de)
 */
public class TranscriptionMenuItem extends MenuItem {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */

	/* Constructors */
	/**
	 * Constructor for the {@link TranscriptionMenuItem} class.
	 *
	 * @param item
	 *            the <code>TranscriptionItem</code> to be represented by this menu item.
	 * @param button
	 *            the button to which this item's parent menu belongs.
	 */
	public TranscriptionMenuItem(TranscriptionItem item, WordButton button) {
		super();

		// Set the item's text string and possible icon.
		setText("/" + item.getPhoneticString() + "/");
		setTickIcon(button.getSelectedItem().equals(item));

		/*
		 * Register a listener so that the button's selection will be changed and the context menu
		 * closed as soon as the user chooses a transcription item.
		 */
		setOnAction((ActionEvent event) -> {
			Platform.runLater(() -> {
				button.getContextMenu().hide();
				item.select();
			});
		});

		/*
		 * Register a listener to the button's selected item property. If the current item is the
		 * one selected by the button/user, then this menu item will receive a little grey tick.
		 * Otherwise, any existing such image will be cleared.
		 */
		button.selectedItemProperty().addListener(new ChangeListener<TranscriptionItem>() {

			@Override
			public void changed(ObservableValue<? extends TranscriptionItem> observable, TranscriptionItem oldValue,
					TranscriptionItem newValue) {

				Platform.runLater(() -> {
					setTickIcon(newValue.equals(item));
				});
			}
		});
	}

	/* Object Methods */
	/**
	 * Enables the grey tick icon which indicates that this menu item's {@link TranscriptionItem} is
	 * selected or not as the transcription currently displayed by the {@link WordButton}.
	 *
	 * @param visible
	 *            the value to the set icon's visibility to.
	 */
	private void setTickIcon(boolean visible) {
		Platform.runLater(() -> {

			try {
				if (visible) {
					ImageView view = new ImageView(new Image(new ImageProvider().getResourceAsStream("tick_grey.png")));
					view.setFitWidth(16.0);
					view.setFitHeight(16.0);
					setGraphic(view);
				} else {
					setGraphic(null);
				}
			} catch (FileNotFoundException | URISyntaxException e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Unable to load image!", e);
			}
		});
	}

	/* Getters and Setters */
}
