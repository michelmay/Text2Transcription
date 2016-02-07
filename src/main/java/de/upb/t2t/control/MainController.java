package de.upb.t2t.control;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import config.ConfigProvider;
import de.upb.t2t.control.gui.settings.SettingsStage;
import de.upb.t2t.control.gui.transcriber.MainTranscriptionPane;
import de.upb.t2t.control.sqlite.SQLiteBridgeDesktop;
import de.upb.t2t.model.CurrencyCharacter;
import de.upb.t2t.model.LoginCredentials;
import de.upb.t2t.model.PunctuationCharacter;
import de.upb.t2t.model.Variety;
import de.upb.t2t.model.WordClass;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * The application's main controller class. It is the central access point for all parts of the
 * application to, for example, fetch a property value (please note {@link Properties} vs
 * {@link Property}, retrieve the {@link MainTranscriptionPane} or cleanly terminate the
 * application.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class MainController {

	/* Static Variables */
	/** The application's current version. */
	public static final String VERSION = "v1.0.0";
	/** The application's main instance. */
	private static MainController instance = new MainController();

	/* Static Methods */
	/**
	 * A getter for the application's main controller {@link #instance}.
	 * 
	 * @return the main controller's <code>instance</code> object.
	 */
	public static MainController getInstance() {
		return instance;
	}

	/**
	 * A convenience getter for easy lookup of a value stored inside the {@link #appProperties}
	 * object.
	 *
	 * @param key
	 *            the key to look up.
	 * @return the corresponding value.
	 */
	public static String getProperty(String key) {
		return instance.getProperties().getProperty(key);
	}

	/**
	 * A convenience getter for easy lookup of a value stored inside the {@link #defaultProperties}
	 * object.
	 * 
	 * @param key
	 *            the key to look up.
	 * @return the corresponding value.
	 */
	public static String getDefaultProperty(String key) {
		return instance.defaultProperties.getProperty(key);
	}

	/**
	 * A convenience setter for easy updating of application properties.
	 *
	 * @param key
	 *            the key to set
	 * @param value
	 *            the value to set
	 */
	public static void setProperty(String key, String value) {
		instance.getProperties().setProperty(key, value);
	}

	/* Object Variables */
	/** The logger instance for this class. */
	private Logger logger;
	/**
	 * Indicates whether the application has already been initialised.
	 */
	private boolean initialised;
	/**
	 * Indicates whether the application is already terminating.
	 *
	 * @see #closeProgramme()
	 */
	private boolean terminating;
	/** The application's default properties. */
	private Properties defaultProperties;
	/** The application's user-shaped properties. */
	private Properties appProperties;
	/**
	 * The user's login credentials. They control the access to the add/edit word functionality.
	 */
	private LoginCredentials loginCredentials;
	/**
	 * The main stage's combined {@link Stage} and controller object.
	 */
	private MainTranscriptionPane transcriptionPanel;
	/** The settings window's combined {@link Stage} and controller object. */
	private SettingsStage settingsStage;

	/* Constructors */
	/**
	 * Constructor for the {@link MainController} class.
	 */
	private MainController() {
		logger = Logger.getLogger(getClass().getName());
		initialised = false;
	}

	/* Object Methods */
	/**
	 * Used to initialise the application and primary stage.
	 *
	 * @param primaryStage
	 *            the application's primary stage.
	 * @param args
	 *            any parameters parsed from the command line.
	 * @throws FileNotFoundException
	 *             if the application property files could not be found.
	 * @throws IOException
	 *             if the application property files could not be read or the
	 *             {@link MainTranscriptionPane#MainTranscriptionPane()} constructor threw one.
	 * @throws ClassNotFoundException
	 *             if the classes to be initialised on startup could not be loaded.
	 * @throws URISyntaxException
	 *             if {@link MainTranscriptionPane#MainTranscriptionPane()} throws one.
	 */
	public synchronized void init(Stage primaryStage, Application.Parameters args)
			throws FileNotFoundException, IOException, ClassNotFoundException, URISyntaxException {

		if (initialised) {
			throw new IllegalStateException("The application has already been initialised!");
		}

		logger.info("Initialising application ...");
		initialised = true;
		terminating = false;

		// ///////////////////// //
		// CONFIGS & LAUNCH ARGS //
		// ///////////////////// //
		try (InputStream defaultIn = new ConfigProvider().getResourceAsStream("default");
				InputStream appIn = new ConfigProvider().getResourceAsStream("application")) {

			// Default Properties
			defaultProperties = new Properties();
			defaultProperties.load(defaultIn);

			// Application Properties
			appProperties = new Properties(defaultProperties);
			appProperties.load(appIn);

		} catch (URISyntaxException e) {
			logger.log(Level.SEVERE, "Exception while loading application properties!", e);
		}

		// Process whatever command line parameter may be given.
		Map<String, String> namedArgs = args.getNamed();
		loginCredentials = new LoginCredentials(namedArgs.get("username"), namedArgs.get("password"));

		// ///////////// //
		// CLASS LOADING //
		// ///////////// //
		// Prepare the URLs for all classes that we want to load on startup.
		Class.forName(Variety.class.getName());
		Class.forName(WordClass.class.getName());
		Class.forName(CurrencyCharacter.class.getName());
		Class.forName(PunctuationCharacter.class.getName());

		// //////// //
		// MAIN GUI //
		// //////// //
		// Add all listeners to the primary stage.
		primaryStage.widthProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				appProperties.setProperty("gui.main.width", newValue.doubleValue() + "");
			}
		});
		primaryStage.heightProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				appProperties.setProperty("gui.main.height", newValue.doubleValue() + "");
			}
		});
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent we) {
				closeProgramme();
			}
		});

		// Set the initial size and position of the main window.
		primaryStage.setWidth(Double.parseDouble(appProperties.getProperty("gui.main.width")));
		primaryStage.setHeight(Double.parseDouble(appProperties.getProperty("gui.main.height")));

		// Load the parent for the primary scene and store it.
		transcriptionPanel = new MainTranscriptionPane();

		// Initialise the settings stage as null.
		settingsStage = null;

		// Set the primary scene and make the stage visible.
		primaryStage.setScene(new Scene(transcriptionPanel));
		primaryStage.setTitle("Text2Transcription " + VERSION);
		primaryStage.centerOnScreen();
		primaryStage.show();
	}

	/**
	 * Closes the application cleanly.
	 */
	public synchronized void closeProgramme() {

		// Make sure we're only shutting down once.
		if (terminating) {
			return;
		}

		// Prevent this function from being called again.
		terminating = true;

		logger.info("Closing application ...");

		// Write the application properties.
		try (FileOutputStream appOut = new FileOutputStream(new ConfigProvider().getResourceAsFile("application"))) {
			logger.fine("Saving properties ...");
			appProperties.store(appOut, "");
		} catch (IOException | URISyntaxException e) {
			logger.log(Level.WARNING, "Unable to write application properties!", e);
		}

		// Close the SQLiteHandler.
		try {
			SQLiteBridgeDesktop.getInstance().closeDatabase();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "SQLException while terminating SQLiteHandler!", e);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Exception while terminating SQLiteHandler!", e);
		}

		Platform.exit();
	}

	/**
	 * A convenience method to quickly the user's login credentials should they provide any.
	 *
	 * @param username
	 *            the username to update.
	 * @param password
	 *            the password to update.
	 */
	public void updateLoginCredentials(String username, String password) {
		loginCredentials.setUsername(username);
		loginCredentials.setPassword(password);
	}

	/**
	 * Displays the settings stage.
	 */
	public synchronized void showSettings() {
		if (settingsStage != null) {
			logger.info("Not showing settings stage as it is already being displayed!");
			return;
		}

		Platform.runLater(() -> {
			try {
				logger.info("Showing settings stage ...");
				settingsStage = new SettingsStage();
			} catch (IOException | URISyntaxException e) {
				logger.log(Level.WARNING, "Unable to show settings stage!", e);
			}
		});
	}

	/**
	 * Sets the {@link #settingsStage} to <code>null</code>.
	 */
	public synchronized void unregisterSettingsStage() {
		logger.fine("Unregistering settings stage ...");
		settingsStage = null;
	}

	/* Getters and Setters */
	/**
	 * A getter for the {@link #appProperties} object.
	 *
	 * @return the application's properties.
	 */
	public Properties getProperties() {
		return appProperties;
	}

	/**
	 * A getter for the {@link #loginCredentials} object.
	 *
	 * @return the user's login credentials
	 */
	public LoginCredentials getLoginCredentials() {
		return loginCredentials;
	}

	/**
	 * A getter for the {@link #transcriptionPanel} object.
	 *
	 * @return the main stage's controller.
	 */
	public MainTranscriptionPane getTranscriptionPanel() {
		return transcriptionPanel;
	}
}
