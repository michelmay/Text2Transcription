/**
 *
 */
package de.upb.t2t.control.gui.phonetic_keyboard;

import java.io.IOException;
import java.net.URL;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

/**
 * The {@link HBox} of {@link Button}s which the user is presented with should they type in one of
 * the characters that trigger the {@link PhoneticKeyboardHandler}.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class PhoneticSoundsChoice extends HBox {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** The event handler to send the chosen phonetic sound back to. */
	private PhoneticKeyboardHandler handler;

	/* Constructors */
	/**
	 * Constructor for the {@link PhoneticSoundsChoice} class. Since the range of buttons added to
	 * this HBox may vary, their respective FXML file must be given as a parameter.
	 *
	 * @param handler
	 *            the event handler to send the chosen phonetic sound back to.
	 * @param fxmlFile
	 *            the fxml to initialise this object with.
	 * @throws IOException
	 *             if the {@link FXMLLoader} throws one.
	 */
	public PhoneticSoundsChoice(PhoneticKeyboardHandler handler, URL fxmlFile) throws IOException {
		this.handler = handler;
		FXMLLoader loader = new FXMLLoader(fxmlFile);
		loader.setRoot(this);
		loader.setController(this);
		loader.load();
	}

	/* Object Methods */
	/**
	 * Should be called when one buttons fires. It will then pass the user's choice to the
	 * {@link #handler} instnace.
	 *
	 * @param event
	 *            the event.
	 */
	@FXML
	private void onAction(ActionEvent event) {
		handler.sendChoice(((Button) event.getSource()).getText());
	}

	/**
	 * Should be called whenever the user presses any keyboard button on one of the buttons
	 * displaying the phonetic symbols. The function will then decide whether to hide the popup,
	 * fire the respective button {@link KeyCode#ENTER} or simple tab through the available buttons
	 * for user convenience. In order to prevent confusion, tabbing will only be allowed for the
	 * {@link KeyCode#TAB} key and in those cases where e.g. the
	 * {@link PhoneticKeyboardHandler#aSounds} are showing and {@link KeyCode#A} was pressed.
	 *
	 * @param event
	 *            the event.
	 * @see #focusNextButton()
	 * @see PhoneticKeyboardHandler#getDisplayedSoundChoice()
	 */
	@FXML
	protected void onKeyPressed(KeyEvent event) {
		KeyCode keyCode = event.getCode();
		boolean focusNextButton = false;

		switch (keyCode) {
		case ESCAPE:
			handler.hidePopup();
			event.consume();
			break;
		case ENTER:
			((Button) event.getSource()).fire();
			event.consume();
			break;
		case TAB:
			event.consume();
			focusNextButton = true;
			break;
		case A:
			focusNextButton = handler.getDisplayedSoundChoice().equalsIgnoreCase("a");
			break;
		case E:
			focusNextButton = handler.getDisplayedSoundChoice().equalsIgnoreCase("e");
			break;
		case I:
			focusNextButton = handler.getDisplayedSoundChoice().equalsIgnoreCase("i");
			break;
		case O:
			focusNextButton = handler.getDisplayedSoundChoice().equalsIgnoreCase("o");
			break;
		case U:
			focusNextButton = handler.getDisplayedSoundChoice().equalsIgnoreCase("u");
			break;
		case D:
			focusNextButton = handler.getDisplayedSoundChoice().equalsIgnoreCase("d");
			break;
		case N:
			focusNextButton = handler.getDisplayedSoundChoice().equalsIgnoreCase("n");
			break;
		case S:
			focusNextButton = handler.getDisplayedSoundChoice().equalsIgnoreCase("s");
			break;
		case T:
			focusNextButton = handler.getDisplayedSoundChoice().equalsIgnoreCase("t");
			break;
		case Z:
			focusNextButton = handler.getDisplayedSoundChoice().equalsIgnoreCase("z");
			break;
		default:
			break;
		}

		if (focusNextButton) {
			focusNextButton();
		}
	}

	/**
	 * A convenience function to cycle through the displayed buttons. If the last button had focus
	 * when this function was called, the first button will be focused.
	 */
	private void focusNextButton() {
		Platform.runLater(() -> {
			ObservableList<Node> buttons = getChildren();
			for (int i = 0; i < buttons.size(); i++) {
				if (buttons.get(i).isFocused()) {
					// Select the next (or first) button.
					i = i >= buttons.size() - 1 ? 0 : ++i;
					buttons.get(i).requestFocus();
				}
			}
		});
	}

	/* Getters and Setters */
}
