package de.upb.t2t.control.gui.settings;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;

import de.upb.t2t.control.MainController;
import de.upb.t2t.control.Statics;
import de.upb.t2t.control.gui.transcriber.MainTranscriptionPane;
import de.upb.t2t.gui.fxml.FXMLProvider;
import de.upb.t2t.model.Variety;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * The stage displaying the application's configurable settings. It can be accessed from the
 * {@link MainTranscriptionPane}'s menu.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class SettingsStage extends Stage {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */
	/** The stage's logger object. */
	private Logger logger;
	/** The <code>ComboBox</code> to choose the preferred {@link Variety} from. */
	@FXML
	private ComboBox<Variety> prefVarBox;
	/** The <code>TextField</code> containing the URL to the database update server. */
	@FXML
	private TextField databaseServerInput;
	/**
	 * The <code>CheckBox</code> responsible for determining whether or not editing the database
	 * will take place in an offline fashion.
	 */
	@FXML
	private CheckBox offlineCheckBox;

	/* Constructors */
	/**
	 * Constructor for the {@link SettingsStage} class.
	 *
	 * @throws IOException
	 *             if the {@link FXMLLoader} throws one.
	 * @throws URISyntaxException
	 *             if the {@link FXMLProvider} throws one.
	 */
	public SettingsStage() throws IOException, URISyntaxException {

		logger = Logger.getLogger(getClass().getName());

		BorderPane root = new BorderPane();

		FXMLLoader loader = new FXMLLoader(new FXMLProvider().getResourceAsURL("SettingsPane.fxml"));
		loader.setRoot(root);
		loader.setController(this);
		loader.load();

		Platform.runLater(() -> {

			logger.fine("Populating settings stage ...");

			// Fill the combo boxes with life.
			ArrayList<Variety> varieties = new ArrayList<Variety>(Arrays.asList(Variety.values()));
			varieties.sort(new Comparator<Variety>() {
				@Override
				public int compare(Variety o1, Variety o2) {
					return o1.compareTo(o2);
				}
			});
			prefVarBox.getItems().addAll(varieties);
			prefVarBox.getSelectionModel().select(Variety.getPreferredVariety());

			// Bind the the database server field to the offline checkbox and populate it.
			databaseServerInput.disableProperty().bind(offlineCheckBox.selectedProperty());
			databaseServerInput.setText(MainController.getProperty("database.server.url"));
			offlineCheckBox.setSelected(Statics.parseBoolean(MainController.getProperty("database.offline_mode")));

		});

		setScene(new Scene(root));
		setTitle("Settings");
		show();
	}

	/* Object Methods */
	/**
	 * Should be called when the Ok button is pressed. Will save all changes.
	 *
	 * @param e
	 *            the <code>ActionEvent</code>.
	 */
	@FXML
	private void onOk(ActionEvent e) {

		logger.info("Saving settings ...");

		// Save all properties.
		MainController.setProperty("variety.preferred",
				prefVarBox.getSelectionModel().getSelectedItem().getAbbreviation());
		MainController.setProperty("database.server.url", databaseServerInput.getText().trim());
		MainController.setProperty("database.offline_mode", offlineCheckBox.isSelected() ? "1" : "0");

		hide();
	}

	/**
	 * Should be called when the Cancel button is pressed. Will discard all changes.
	 *
	 * @param e
	 *            the <code>ActionEvent</code>.
	 */
	@FXML
	private void onCancel(ActionEvent e) {

		// Discard all changes and simply close the window.
		logger.info("Discarding changes ...");
		hide();
	}

	/**
	 * Should be called when the Restore Defaults button is pressed. Will reset all values to those
	 * held in the {@link MainController#defaultProperties} object.
	 *
	 * @param e
	 *            the <code>ActionEvent</code>.
	 */
	@FXML
	private void onRestoreDefaults(ActionEvent e) {

		logger.info("Restoring settings default ...");

		Platform.runLater(() -> {
			prefVarBox.getSelectionModel()
					.select(Variety.getVariety(MainController.getDefaultProperty("variety.preferred")));
			databaseServerInput.setText(MainController.getDefaultProperty("database.server.url"));
			offlineCheckBox
					.setSelected(Statics.parseBoolean(MainController.getDefaultProperty("database.offline_mode")));
		});
	}

	/**
	 * A wrapper for the super-type function that will additionally call
	 * {@link MainController#unregisterSettingsStage()}.
	 */
	@Override
	public void hide() {
		MainController.getInstance().unregisterSettingsStage();
		super.hide();
	}

	/* Getters and Setters */
}
