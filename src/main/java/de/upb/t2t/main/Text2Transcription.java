/**
 *
 */
package de.upb.t2t.main;

import de.upb.t2t.control.MainController;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * The Text 2 Transcription App's main class. Does nothing but initialising the
 * {@link MainController}.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class Text2Transcription extends Application {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */

	/* Constructors */

	/* Object Methods */
	/**
	 * This function is called on application startup by the JavaFx engine. It does nothing but
	 * calling {@link MainController#init(Stage, Parameters)}.
	 *
	 * @param primaryStage
	 *            the application's primary stage.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		MainController.getInstance().init(primaryStage, getParameters());
	}

	/**
	 * The class's main method.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		launch(args);
	}
}
