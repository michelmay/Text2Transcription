package de.upb.t2t.control.gui.transcriber;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import de.upb.t2t.control.Transcriber;
import de.upb.t2t.gui.fxml.FXMLProvider;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * <p>
 * One of the central classes when it comes to displaying a piece of transcription to the user. A
 * {@link TranscriptionSegment} is visually divided into to layers, the first of which featuring the
 * lemma that a user has typed in. Said lemma is surrounded by a potential leading and trailing
 * <code>String</code> which may contain any non-word characters. In general, there is no user
 * action happening in this part.
 * </p>
 * <p>
 * The second layer presents the user with a list of (usually) {@link WordButton}s representing the
 * broad phonetic transcription of the lemma. The most likely case is that only one
 * <code>WordButton</code> is required to do so. However, it may also be possible that a numeral
 * consisting of digits or any other special input has to be transcribed. With "200" serving as an
 * example, the {@link #buttonBox} would then be filled with two <code>WordButton</code>s showing
 * /tuː hʌndred/.
 * </p>
 *
 * @author Michel May (michel-may@gmx.de)
 *
 * @see Transcriber
 * @see DelimiterButton
 */
public class TranscriptionSegment extends VBox {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** The label showing any non-word characters preceding the actual lemma. */
	@FXML
	private Label leading;
	/** The label showing any non-word characters trailing behind the actual lemma. */
	@FXML
	private Label trailing;
	/** The button displaying the lemma - usually disabled. */
	@FXML
	private Button lemmaButton;
	/** The <code>HBox</code> containing the buttons necessary for the lemma's transcription. */
	@FXML
	private HBox buttonBox;

	/* Constructors */
	/**
	 * Convenience constructor for the {@link TranscriptionSegment} class. Will instantiate an empty
	 * / blank segment.
	 *
	 * @throws IOException
	 *             if {@link #TranscriptionSegment(String, String, String)} throws one.
	 * @throws URISyntaxException
	 *             if {@link #TranscriptionSegment(String, String, String)} throws one.
	 */
	public TranscriptionSegment() throws IOException, URISyntaxException {
		this(null, "", null);
	}

	/**
	 * Convenience constructor for the {@link TranscriptionSegment} class. Will automatically set
	 * the {@link #lemmaButton}'s text property.
	 *
	 * @param lemma
	 *            the lemma to be displayed.
	 * @throws IOException
	 *             if {@link #TranscriptionSegment(String, String, String)} throws one.
	 * @throws URISyntaxException
	 *             if {@link #TranscriptionSegment(String, String, String)} throws one.
	 */
	public TranscriptionSegment(String lemma) throws IOException, URISyntaxException {
		this(null, lemma, null);
	}

	/**
	 * A constructor for the {@link TranscriptionSegment} class. It uses an {@link FXMLLoader} to
	 * parse all its contents and then sets itself as both root and controller of the resulting
	 * object.
	 *
	 * @param leading
	 *            the <code>String</code> preceding the lemma (e.g. punctuation characters).
	 * @param lemma
	 *            the lemma to transcribe.
	 * @param trailing
	 *            the <code>String</code> following the lemma (e.g. punctuation characters).
	 * @throws IOException
	 *             if the {@link FXMLLoader} throws one.
	 * @throws URISyntaxException
	 *             if the {@link FXMLProvider} throws one.
	 */
	public TranscriptionSegment(String leading, String lemma, String trailing) throws IOException, URISyntaxException {

		// Register this instance as root and controller of the corresponding TranscriptionSegment
		// object.
		FXMLLoader fxmlLoader = new FXMLLoader(new FXMLProvider().getResourceAsURL("TranscriptionSegment.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		fxmlLoader.load();

		this.leading.setText(leading);
		this.trailing.setText(trailing);
		lemmaButton.setText(lemma);
		lemmaButton.setDisable(true);
	}

	/* Object Methods */

	/* Getters and Setters */
	/**
	 * A convenience getter indicating whether or not the last {@link Node} in the
	 * {@link #buttonBox} is an instance of {@link DelimiterButton}.
	 *
	 * @return <code>true</code> if the last item in the <code>buttonBox</code> is an instance of
	 *         <code>DelimiterButton</code>; <code>false</code> otherwise.
	 */
	public boolean hasTrailingDelimiter() {
		List<Node> buttons = buttonBox.getChildren();

		if (buttons.isEmpty()) {
			return false;
		}

		return buttons.get(buttons.size() - 1) instanceof DelimiterButton;
	}

	/**
	 * A convenience getter indicating whether or not the {@link #buttonBox} has any child elements.
	 *
	 * @return <code>true</code> if the <code>buttonBox</code> has child elements;
	 *         <code>false</code> otherwise.
	 */
	public boolean hasTranscriptionButtons() {
		return !buttonBox.getChildren().isEmpty();
	}

	/**
	 * A getter for the {@link #leading} attribute's text property.
	 *
	 * @return the <code>leading</code> attribute.
	 */
	public String getLeadingString() {
		return leading.getText();
	}

	/**
	 * A getter for the {@link #lemmaButton} attribute.
	 *
	 * @return the <code>lemmaButton</code> attribute.
	 */
	public Button getLemmaButton() {
		return lemmaButton;
	}

	/**
	 * A getter for the {@link #trailing} attribute's text property.
	 *
	 * @return the <code>trailing</code> attribute.
	 */
	public String getTrailingString() {
		return trailing.getText();
	}

	/**
	 * A getter for the {@link #buttonBox} attribute.
	 *
	 * @return the <code>buttonBox</code> attribute.
	 */
	public HBox getButtonBox() {
		return buttonBox;
	}

	/**
	 * A setter for the {@link #leading} attribute's text property.
	 *
	 * @param string
	 *            the <code>String</code> to set as the <code>leading</code> attribute's text
	 *            property.
	 */
	public void setLeadingString(String string) {
		leading.setText(string);
	}

	/**
	 * A setter for the {@link #trailing} attribute's text property.
	 *
	 * @param string
	 *            the <code>String</code> to set as the <code>trailing</code> attribute's text
	 *            property.
	 */
	public void setTrailingString(String string) {
		trailing.setText(string);
	}
}
