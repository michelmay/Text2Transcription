package de.upb.t2t.control.gui;

import java.io.IOException;
import java.net.URISyntaxException;

import de.upb.t2t.control.database.editing.EditDatabaseDialogue;
import de.upb.t2t.model.FeedbackMode;
import images.ImageProvider;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

/**
 * The control class of a <code>FeedbackIndicator</code> {@link Pane}. Please note that there is no
 * such Java class as a <code>FeedbackIndicator</code>. However, the
 * <code>FeedbackIndicator.fxml</code> may be included inside any other fxml file and its controller
 * obtained by the JavaFx controller naming conventions.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 * @see EditDatabaseDialogue#inputValidityIconController
 * @see <a href=
 *      "https://docs.oracle.com/javafx/2/api/javafx/fxml/doc-files/introduction_to_fxml.html#nested_controllers">
 *      Oracle Introduction to FXML - Nested Controllers</a>
 */
public class FeedbackIndicatorController {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** The feedback indicator's root pane. */
	@FXML
	private Pane root;
	/** The feedback indicator's progress indicator. */
	private final ProgressIndicator progressIndicator;
	/** The feedback indicator's image view. */
	private final ImageView imageView;
	/** An image displaying a green tick (arrow). */
	private final Image tick;
	/** An image displaying a red cross. */
	private final Image cross;

	/* Constructors */
	/**
	 * Constructor for the {@link FeedbackIndicatorController} class.
	 *
	 * @throws IOException
	 *             if the indicator's images could not be loaded.
	 * @throws URISyntaxException
	 */
	public FeedbackIndicatorController() throws IOException, URISyntaxException {

		// Prepare the GUI sub-components, but do not add them yet.
		progressIndicator = new ProgressIndicator();
		progressIndicator.setProgress(-1);
		progressIndicator.getStyleClass().add("feedbackProgressIndicator");
		imageView = new ImageView();
		ImageProvider imgProv = new ImageProvider();
		tick = new Image(imgProv.getResourceAsStream("tick_green.png"));
		cross = new Image(imgProv.getResourceAsStream("cross_red.png"));
	}

	/* Object Methods */

	/* Getters and Setters */
	/**
	 * Determines whether or not the feedback indicator displays any image or the progress indicator
	 * depending on the given feedback mode.
	 *
	 * @param mode
	 *            the feedback to give the user.
	 */
	public void setFeedbackMode(FeedbackMode mode) {
		Platform.runLater(() -> {

			// First of all remove any existent nodes on this pane.
			ObservableList<Node> children = root.getChildren();
			children.clear();

			switch (mode) {
			case LOADING:
				children.add(progressIndicator);
				break;
			case SUCCESS:
				imageView.setImage(tick);
				children.add(imageView);
				break;
			case INVALID:
			case ERROR:
				imageView.setImage(cross);
				children.add(imageView);
				break;
			default:
				break;
			}
		});
	}
}
