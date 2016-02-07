/**
 *
 */
package de.upb.t2t.control.database.editing;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import config.ConfigProvider;
import de.upb.t2t.control.MainController;
import de.upb.t2t.control.database.AbstractSQLiteBridge;
import de.upb.t2t.control.gui.transcriber.MainTranscriptionPane;
import de.upb.t2t.model.FeedbackMode;
import de.upb.t2t.model.LoginCredentials;
import de.upb.t2t.model.TranscriptionItem;
import de.upb.t2t.model.TranscriptionType;
import de.upb.t2t.model.Variety;
import de.upb.t2t.model.WordClass;
import javafx.application.Platform;

/**
 * <p>
 * An implementation of the {@link AbstractDatabaseEditingHandler} that provides online editing
 * functionality. Its {@link #reloadDatabase()} method can be employed by any user to overwrite the
 * local database with the latest version provided by the database server. More importantly,
 * however, this class's functions also enable authorised users, i.e. those who can produce valid
 * login credentials, to contribute to the online database. In order to do so, every little step of
 * editation, for example adding a new {@link TranscriptionItem}, will be communicated instantly to
 * the server, sending the currently stored {@link LoginCredentials} along every time.
 * </p>
 * <p>
 * With this class employing a singleton pattern, its instance can be obtained by calling
 * {@link #getInstance()}.
 * </p>
 *
 * @author Michel May (michel-may@gmx.de)
 *
 * @see OfflineHandler
 * @see MainController#getLoginCredentials()
 */
public class OnlineHandler extends AbstractDatabaseEditingHandler {

	/* Static Variables */
	/** The online handler's instance. */
	private static final OnlineHandler instance = new OnlineHandler();
	/**
	 * The separator <code>String</code> used to distinguish parameters when communicating with the
	 * server.
	 */
	private static final String SEPARATOR = "&";

	/* Static Methods */
	/**
	 * A getter for the online handler's {@link #instance}.
	 *
	 * @return the online handler's <code>instance</code> object.
	 */
	public static OnlineHandler getInstance() {
		return instance;
	}

	/* Object Variables */
	/** The handler's logger variable. */
	private Logger logger;
	/** Indicates whether or not the database is currently being reloaded from the online server. */
	private boolean reloadingDB;
	/** A lock used to make sure that intersecting operations do not interfer with one another. */
	private ReentrantLock lock;

	/**
	 * Constructor for the {@link OnlineHandler} class.
	 */
	/* Constructors */
	private OnlineHandler() {
		logger = Logger.getLogger(getClass().getName());
		reloadingDB = false;
		lock = new ReentrantLock(true);
	}

	/* Object Methods */
	@Override
	public TranscriptionItem addTranscription(String lemma, WordClass wordClass, Variety variety,
			TranscriptionType transType, String transcription) throws Exception {
		lock.lock();
		try {

			// Prepare and post the request.
			StringBuilder builder = getStringBuilderWithCredentials("ADD_TRANS_ITEM");
			builder.append("lemma=" + lemma + SEPARATOR);
			builder.append("wordClassID=" + wordClass.getID() + SEPARATOR);
			builder.append("varietyID=" + variety.getID() + SEPARATOR);
			builder.append("transType=" + transType.getID() + SEPARATOR);
			builder.append("transcription=" + transcription);
			postServerRequest(builder.toString());

			// Store the item in the local database, too.
			TranscriptionItem item = AbstractSQLiteBridge.getInstance().addTranscription(lemma, wordClass, variety,
					transType, transcription);

			return item;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void updateLemma(int lemmaID, String lemma) throws Exception {
		lock.lock();
		try {

			StringBuilder builder = getStringBuilderWithCredentials("UPDATE_LEMMA");
			builder.append("id=" + lemmaID + SEPARATOR);
			builder.append("lemma=" + lemma);
			postServerRequest(builder.toString());
			AbstractSQLiteBridge.getInstance().updateLemma(lemmaID, lemma);

		} finally {
			lock.unlock();
		}
	}

	@Override
	public TranscriptionItem updateTranscription(int transID, WordClass wordClass, Variety variety,
			TranscriptionType transType, String transcription) throws Exception {
		lock.lock();
		try {

			StringBuilder builder = getStringBuilderWithCredentials("UPDATE_TRANS_ITEM");
			builder.append("transID=" + transID + SEPARATOR);
			builder.append("wordClassID=" + wordClass.getID() + SEPARATOR);
			builder.append("varietyID=" + variety.getID() + SEPARATOR);
			builder.append("transType=" + transType.getID() + SEPARATOR);
			builder.append("transcription=" + transcription);
			postServerRequest(builder.toString());

			return AbstractSQLiteBridge.getInstance().updateTranscription(transID, wordClass, variety, transType,
					transcription);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void deleteTranscription(int transID) throws Exception {
		lock.lock();
		try {

			StringBuilder builder = getStringBuilderWithCredentials("DELETE_TRANS_ITEM");
			builder.append("transID=" + transID);
			postServerRequest(builder.toString());

			AbstractSQLiteBridge.getInstance().deleteTranscription(transID);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void deleteTranscription(TranscriptionItem item) throws Exception {
		deleteTranscription(item.getId());
	}

	/**
	 * This method should be called in order to get the latest version of the online database. It
	 * makes sure that there will not be two or more update processes taking place at the same time.
	 */
	public void reloadDatabase() {

		lock.lock();
		try {

			if (reloadingDB) {
				logger.info("Not reloading the database, as another reload process is still pending!");
				return;
			}

			reloadingDB = true;
			new DBReloadThread().start();

		} finally {
			lock.unlock();
		}
	}

	/**
	 * A convenience function that will post the parameter request <code>String</code> to the server
	 * and return its reponse.
	 *
	 * @param request
	 *            the request to send.
	 * @return the server's response.
	 * @throws Exception
	 */
	private String postServerRequest(String request) throws Exception {

		// Prepare the url connection and post the request.
		URLConnection conn = getURLConnectionToServer();
		OutputStream out = conn.getOutputStream();
		out.write(request.getBytes("UTF-8"));
		out.flush();
		out.close();

		// Read the response.
		String response = IOUtils.toString(conn.getInputStream());

		// Invalid login credentials?
		if (response.equalsIgnoreCase("wrong credentials")) {
			throw new Exception("Invalid login credentials!");
		}

		return response;
	}

	/* Getters & Setters */
	/**
	 * A convencience getter to quickly check, whether the currently held {@link LoginCredentials}
	 * are valid. This function should for example be called at least once prior to opening the
	 * {@link EditDatabaseDialogue} to make sure the user does not waste their time editing the
	 * database when they actually are not allowed to do so.
	 *
	 * @return <code>true</code> if the <code>LoginCredentials</code> are valid; <code>false</code>
	 *         otherwise.
	 * @throws Exception
	 * @see MainController#getLoginCredentials()
	 */
	public boolean isAuthenticationConfirmed() throws Exception {
		lock.lock();
		try {

			// Prepare and post the request.
			StringBuilder builder = getStringBuilderWithCredentials("CHECK_LOGIN");
			String response = postServerRequest(builder.toString());
			return response.equalsIgnoreCase("valid credentials");

		} finally {
			lock.unlock();
		}
	}

	/**
	 * A convenience function that opens a new connection to the database server.
	 *
	 * @return a <code>URLConnection</code> object able to communicate with the server.
	 * @throws MalformedURLException
	 *             if {@link URL#URL(String)} throws one.
	 * @throws IOException
	 *             of {@link URL#openConnection()} throws one.
	 */
	private URLConnection getURLConnectionToServer() throws MalformedURLException, IOException {
		URLConnection conn = new URL(MainController.getProperty("database.server.url")).openConnection();
		conn.setDoOutput(true);
		return conn;
	}

	/**
	 * A convenience function to instantiate a new <code>StringBuilder</code> object that has
	 * already been fed the user's login credentials and the method that should be invoked on the
	 * server (e.g. <code>ADD_TRANS_ITEM</code>.
	 *
	 * @param method
	 *            the method to invoke on the server.
	 * @return an appropriately prepared instance of <code>StringBuilder</code>.
	 * @throws Exception
	 */
	private StringBuilder getStringBuilderWithCredentials(String method) throws Exception {

		// Do we even have all login credentials?
		LoginCredentials credentials = MainController.getInstance().getLoginCredentials();
		if (!credentials.available()) {
			throw new Exception("No login credentials available!");
		}

		StringBuilder builder = new StringBuilder();
		builder.append(method + " ");
		builder.append("username=" + credentials.getUsername() + SEPARATOR);
		builder.append("password=" + credentials.getPassword() + SEPARATOR);
		return builder;
	}

	/* Sub-Classes */
	/**
	 * A convenience implementationo of the {@link Thread} class that is responsible for reloading
	 * the database from the server. This needs to be done outside the main application thread as it
	 * may otherwise freeze he user interface.
	 *
	 * @author Michel May (michel-may@gmx.de)
	 */
	private class DBReloadThread extends Thread {

		/**
		 * Disables the main GUI so that the user is unable to interfer while the update is pending.
		 * It then disconnects the {@link AbstractSQLiteBridge} instance from the database, update
		 * the latter and reconnect the bridge to it. Eventually, the GUI is being re-enabled.
		 */
		@Override
		public void run() {

			// First of all fetch the reference to the main transcription pane.
			final MainTranscriptionPane pane = MainController.getInstance().getTranscriptionPanel();

			// Now disable the GUI and inform the user.
			Platform.runLater(() -> {
				pane.setDisable(true);
				pane.progressFeedback(-1, "Reloading database ...");
			});

			// Open the connection to the server.
			try {
				URLConnection conn = getURLConnectionToServer();
				BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());

				// Post the request.
				out.write("DATABASE".getBytes("UTF-8"));
				out.flush();
				out.close();

				// Read the server's response.
				byte[] dbBytes = IOUtils.toByteArray(conn.getInputStream());

				// Close the database SQLite bridge's connection to the database file.
				AbstractSQLiteBridge bridge = AbstractSQLiteBridge.getInstance();
				bridge.closeDatabase();

				// Overwrite the database file.
				FileOutputStream dbOut = new FileOutputStream(
						new ConfigProvider().getResourceAsFile("T2T.db").getPath(), false);
				dbOut.write(dbBytes);
				dbOut.flush();
				dbOut.close();

				// Reconnect the bridge to the database file.
				bridge.openDatabase();

			} catch (IOException e) {
				logger.log(Level.WARNING, "Unable to establish connection to online database!", e);
				Platform.runLater(() -> {
					pane.setDisable(false);
					pane.generalFeedback(FeedbackMode.ERROR, "Unable to establish connection to online database! :(");
				});
				return;
			} catch (URISyntaxException e) {
				logger.log(Level.WARNING, "Unable to parse db location URI!", e);
				Platform.runLater(() -> {
					pane.setDisable(false);
					pane.generalFeedback(FeedbackMode.ERROR, "Unable to write changes to local database! :(");
				});
				return;
			} catch (Exception e) {
				logger.log(Level.WARNING, "Unknown exception while updating database!", e);
				Platform.runLater(() -> {
					pane.setDisable(false);
					pane.generalFeedback(FeedbackMode.ERROR, "Unable to update the local database! :(");
				});
				return;
			}

			// We're done successfully it seems. Let the user know and re-enable the pane.
			Platform.runLater(() -> {
				pane.generalFeedback(FeedbackMode.SUCCESS, "Database reloaded.");
				pane.setDisable(false);
			});
			reloadingDB = false;
		}
	}
}
