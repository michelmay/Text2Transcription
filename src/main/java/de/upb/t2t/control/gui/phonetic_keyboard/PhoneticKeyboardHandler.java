package de.upb.t2t.control.gui.phonetic_keyboard;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

import de.upb.t2t.gui.fxml.FXMLProvider;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;
import javafx.util.Pair;

/**
 * <p>
 * This event handler offers functionality to transform any control that accepts {@link KeyEvent}s
 * into a phonetic keyboard. This means that nothing will change for input such as "b", "d", "f",
 * etc. However, as soon as the user type in for example the character "t", a
 * {@link PhoneticSoundsChoice} will pop up and present them with a choice as to which phonetic
 * symbol they may want to type. In this case, the <code>PhoneticSoundsChoice</code> would offer the
 * following symbols: /t/, /tʃ/, /dʒ/, /θ/ and /ð/.
 * </p>
 * <p>
 * Please note that for the sake of easier coding, the term 'character' is not always used
 * consistently for the Java primitive <code>char</code> throughout this class. This is because the
 * JavaFx <code>KeyEvent</code> class by nature of its design oftentimes works with
 * <code>String</code> objects of length 1 instead of <code>char</code>s, as for example in
 * {@link KeyEvent#getCharacter()}.
 * </p>
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class PhoneticKeyboardHandler implements EventHandler<KeyEvent> {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** The input control that the listener listens to. */
	private final TextInputControl inputControl;
	/**
	 * While the {@link PhoneticSoundsChoice} is showing, all {@link KeyEvent}s sent to the
	 * {@link #inputControl} will be consumed. Should the user have decided on a phonetic symbol,
	 * however, this token is used to allow the corresponding event to pass through to the
	 * <code>inputControl</code>.
	 */
	private final AtomicBoolean keyToken;
	/** The popup to be displayed by the listener. */
	private final Popup popup;
	/** A <code>PhoneticSoundsChoice</code> containing all "a"-related phonetic symbols. */
	private PhoneticSoundsChoice aSounds;
	/** A <code>PhoneticSoundsChoice</code> containing all "e"-related phonetic symbols. */
	private PhoneticSoundsChoice eSounds;
	/** A <code>PhoneticSoundsChoice</code> containing all "i"-related phonetic symbols. */
	private PhoneticSoundsChoice iSounds;
	/** A <code>PhoneticSoundsChoice</code> containing all "o"-related phonetic symbols. */
	private PhoneticSoundsChoice oSounds;
	/** A <code>PhoneticSoundsChoice</code> containing all "u"-related phonetic symbols. */
	private PhoneticSoundsChoice uSounds;
	/** A <code>PhoneticSoundsChoice</code> containing all "d"-related phonetic symbols. */
	private PhoneticSoundsChoice dSounds;
	/** A <code>PhoneticSoundsChoice</code> containing all "n"-related phonetic symbols. */
	private PhoneticSoundsChoice nSounds;
	/** A <code>PhoneticSoundsChoice</code> containing all "s"-related phonetic symbols. */
	private PhoneticSoundsChoice sSounds;
	/** A <code>PhoneticSoundsChoice</code> containing all "t"-related phonetic symbols. */
	private PhoneticSoundsChoice tSounds;
	/** A <code>PhoneticSoundsChoice</code> containing all "z"-related phonetic symbols. */
	private PhoneticSoundsChoice zSounds;

	/* Constructors */
	/**
	 * Constructor for the {@link PhoneticKeyboardHandler} class.
	 *
	 * @param inputControl
	 *            the input control to listen to.
	 * @throws IOException
	 *             if any of the {@link PhoneticKeyboardHandler#popuplate(String)} calls throws one.
	 * @throws URISyntaxException
	 */
	public PhoneticKeyboardHandler(TextInputControl inputControl) throws IOException, URISyntaxException {
		this.inputControl = inputControl;
		keyToken = new AtomicBoolean(false);
		popup = new Popup();

		FXMLProvider fxmlProv = new FXMLProvider();
		aSounds = new PhoneticSoundsChoice(this, fxmlProv.getResourceAsURL("SoundsChoiceA.fxml"));
		eSounds = new PhoneticSoundsChoice(this, fxmlProv.getResourceAsURL("SoundsChoiceE.fxml"));
		iSounds = new PhoneticSoundsChoice(this, fxmlProv.getResourceAsURL("SoundsChoiceI.fxml"));
		oSounds = new PhoneticSoundsChoice(this, fxmlProv.getResourceAsURL("SoundsChoiceO.fxml"));
		uSounds = new PhoneticSoundsChoice(this, fxmlProv.getResourceAsURL("SoundsChoiceU.fxml"));
		dSounds = new PhoneticSoundsChoice(this, fxmlProv.getResourceAsURL("SoundsChoiceD.fxml"));
		nSounds = new PhoneticSoundsChoice(this, fxmlProv.getResourceAsURL("SoundsChoiceN.fxml"));
		sSounds = new PhoneticSoundsChoice(this, fxmlProv.getResourceAsURL("SoundsChoiceS.fxml"));
		tSounds = new PhoneticSoundsChoice(this, fxmlProv.getResourceAsURL("SoundsChoiceT.fxml"));
		zSounds = new PhoneticSoundsChoice(this, fxmlProv.getResourceAsURL("SoundsChoiceZ.fxml"));
	}

	/* Object Methods */
	/**
	 * This function has two purposes: displaying the correct {@link PhoneticSoundsChoice} if
	 * required, and blocking any input headed towards the {@link #inputControl} while said popup is
	 * showing.
	 *
	 * @param event
	 *            the key event.
	 */
	@Override
	public void handle(KeyEvent event) {

		String typedCha = event.getCharacter().toLowerCase();

		/*
		 * If the popup is currently displayed and thus the user is making their choice, prevent
		 * keys strokes from interfering with the input field. This means that we must consume the
		 * events in general, but let a few characters with "special permission" pass (as they are
		 * part of the broad phonetic transcription).
		 */
		if (popup.isShowing()) {
			if (keyToken.get() && isSpecialPermissonCharacter(typedCha)) {
				keyToken.set(false);
			} else {
				// Consume the event.
				event.consume();
			}
			return;
		}

		// The popup is not showing at the moment. Do we need to display it?
		switch (typedCha) {
		case "a":
		case "e":
		case "i":
		case "o":
		case "u":
		case "d":
		case "n":
		case "s":
		case "t":
		case "z":
			popuplate(typedCha);
			event.consume();
			break;
		default:
			break;
		}
	}

	/**
	 * Should be called when the user has made their decision in the {@link PhoneticSoundsChoice}
	 * and now that character needs to be sent to the {@link #inputControl}.
	 *
	 * @param phoneticChas
	 *            the character(s) to send to the input control.
	 * @see PhoneticSoundsChoice#onAction(ActionEvent)
	 */
	protected void sendChoice(String phoneticChas) {
		Platform.runLater(() -> {
			keyToken.set(true);
			inputControl.fireEvent(new KeyEvent(null, inputControl, KeyEvent.KEY_TYPED, phoneticChas,
					"phonetic char selected", KeyCode.UNDEFINED, false, false, false, false));
		});

		hidePopup();
	}

	/**
	 * Hides the {@link #popup}.
	 *
	 * @see PhoneticSoundsChoice#onKeyPressed(KeyEvent)
	 */
	protected void hidePopup() {
		Platform.runLater(() -> {
			popup.hide();
		});
	}

	/**
	 * A convenience function that computes where the popup should be displayed.
	 *
	 * @return the pair of coordinates at which the popup should be displayed.
	 */
	private Pair<Double, Double> computePopupCoords() {
		Double x, y;
		Point2D p = inputControl.localToScene(0.0, 0.0);
		x = p.getX() + inputControl.getScene().getX() + inputControl.getScene().getWindow().getX();
		y = p.getY() + inputControl.getScene().getY() + inputControl.getScene().getWindow().getY() - 27;
		return new Pair<Double, Double>(x, y);
	}

	/**
	 * Popuplates the {@link #popup} with the appropriate {@link PhoneticSoundsChoice} pane based on
	 * what character is given as a parameter. Passing this function an "a" for example would lead
	 * to the {@link #aSounds} being added to the <code>popup</code>.
	 *
	 * @param typedCha
	 *            the character typed by the user.
	 */
	private void popuplate(String typedCha) {

		if (popup.isShowing()) {
			return;
		}

		Platform.runLater(() -> {

			// Calculate where to display the popup and clear it.
			Pair<Double, Double> popupCoords = computePopupCoords();
			popup.getContent().clear();

			// Add the correct HBox depending on which button was pressed.
			switch (typedCha) {
			case "a":
				popup.getContent().add(aSounds);
				break;
			case "e":
				popup.getContent().add(eSounds);
				break;
			case "i":
				popup.getContent().add(iSounds);
				break;
			case "o":
				popup.getContent().add(oSounds);
				break;
			case "u":
				popup.getContent().add(uSounds);
				break;
			case "d":
				popup.getContent().add(dSounds);
				break;
			case "n":
				popup.getContent().add(nSounds);
				break;
			case "s":
				popup.getContent().add(sSounds);
				break;
			case "t":
				popup.getContent().add(tSounds);
				break;
			case "z":
				popup.getContent().add(zSounds);
				break;
			default:
				return;
			}

			// Finally display the popup.
			popup.setAutoHide(true);
			popup.show(inputControl, popupCoords.getKey(), popupCoords.getValue());
		});
	}

	/**
	 * Determines whether or not the given character has special permission to be passed to the
	 * {@link #inputControl} while the popup is showing or, to be exact, when the user has made the
	 * choice and the <code>popup</code> will be hidden soon.
	 *
	 * @param character
	 *            the character to evaluate.
	 * @return <code>true</code> if the given character is has special permission;
	 *         <code>false</code> otherwise.
	 */
	private boolean isSpecialPermissonCharacter(String character) {

		switch (character) {
		case "ɑː":
		case "ʌ":
		case "æ":
		case "ɜː":
		case "e":
		case "ə":
		case "iː":
		case "i":
		case "ɪ":
		case "ɔː":
		case "ɒ":
		case "uː":
		case "u":
		case "ʊ":
		case "eɪ":
		case "aɪ":
		case "ɔɪ":
		case "əʊ":
		case "aʊ":
		case "ɪə":
		case "eə":
		case "ʊə":
		case "d":
		case "n":
		case "ŋ":
		case "s":
		case "ʃ":
		case "t":
		case "tʃ":
		case "θ":
		case "ð":
		case "z":
		case "ʒ":
		case "dʒ":
			return true;
		default:
			return false;
		}
	}

	/* Getters and Setters */
	/**
	 * Should the {@link #popup} be showing at the moment of calling this function and should it be
	 * populated, it will return the character that corresponds with the
	 * {@link PhoneticSoundsChoice} currently populating the <code>popup</code>. If, for example,
	 * {@link #aSounds} constitutes its content, <code>"a"</code> will be returned.
	 *
	 * @return the character corresponding to the current content of the <code>popup</code>.
	 */
	protected String getDisplayedSoundChoice() {

		// Is the popup being displayed at all?
		if (!popup.isShowing()) {
			return "";
		}

		// Is the popup currently empty?
		Node content;
		try {
			content = popup.getContent().get(0);
		} catch (IndexOutOfBoundsException e) {
			return "";
		}

		// Return the character depending on the current content.
		if (content.equals(aSounds)) {
			return "a";
		}
		if (content.equals(eSounds)) {
			return "e";
		}
		if (content.equals(iSounds)) {
			return "i";
		}
		if (content.equals(oSounds)) {
			return "o";
		}
		if (content.equals(uSounds)) {
			return "u";
		}
		if (content.equals(dSounds)) {
			return "d";
		}
		if (content.equals(nSounds)) {
			return "n";
		}
		if (content.equals(sSounds)) {
			return "s";
		}
		if (content.equals(tSounds)) {
			return "t";
		}
		if (content.equals(zSounds)) {
			return "z";
		}

		return "";
	}
}
