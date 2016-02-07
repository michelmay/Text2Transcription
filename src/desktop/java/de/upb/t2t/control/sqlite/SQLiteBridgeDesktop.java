package de.upb.t2t.control.sqlite;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import config.ConfigProvider;
import de.upb.t2t.control.database.AbstractSQLiteBridge;
import de.upb.t2t.model.CurrencyCharacter;
import de.upb.t2t.model.DatabaseEntry;
import de.upb.t2t.model.PunctuationCharacter;
import de.upb.t2t.model.TranscriptionItem;
import de.upb.t2t.model.TranscriptionType;
import de.upb.t2t.model.Variety;
import de.upb.t2t.model.WordClass;

/**
 * An implementation of the {@link AbstractSQLiteBridge} class on a desktop operating system.
 *
 * @author Michel May (michel-may@gmx.de
 *
 */
public class SQLiteBridgeDesktop extends AbstractSQLiteBridge {

	/* Static Variables */
	/**
	 * An SQL command <code>String</code> stored for convenience in order to be used in
	 * {@link PreparedStatement} objects throughout this class.
	 */
	private static final String QUERY_LEMMA = "SELECT * FROM lemmas WHERE `lemma` = ?";

	/* Static Methods */

	/* Object Variables */
	/** The bridge's logger object. */
	private Logger logger;
	/** The bridge's connection object. */
	private Connection conn;
	/** The bridge's lock object. */
	private ReentrantLock lock;

	/* Constructors */
	/**
	 * Constructor for the {@link SQLiteBridgeDesktop} class.
	 */
	public SQLiteBridgeDesktop() {
		logger = Logger.getLogger(getClass().getName());
		lock = new ReentrantLock(true);
		try {
			openDatabase();
		} catch (ClassNotFoundException | SQLException | URISyntaxException e) {
			logger.log(Level.SEVERE, "Unable to load / create database!", e);
		}
	}

	/* Object Methods */
	/**
	 * The first thing this method does is setting the {@link #conn} variable to <code>null</code>
	 * to make sure no faulty remnant of an earlier connection, potentially causing conflicts, is
	 * left. The method then proceeds to initialise the {@link #conn} variable with the help of the
	 * JDBC {@link DriverManager}. Should this operation fail, the corresponding exception is thrown
	 * and the variable remains <code>null</code>
	 *
	 * @throws ClassNotFoundException
	 *             if the JDBC driver class could not be loaded.
	 * @throws SQLException
	 *             if the connection could not be established for some other reason (e.g. the
	 *             <code>.db</code> is missing or inaccessible).
	 * @throws URISyntaxException
	 *             if {@link ConfigProvider#getResourceAsFile(String)} throws one.
	 */
	@Override
	public void openDatabase() throws ClassNotFoundException, SQLException, URISyntaxException {
		lock.lock();
		try {
			conn = null;
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager
					.getConnection("jdbc:sqlite:" + new ConfigProvider().getResourceAsFile("T2T.db").getPath());
			conn.setAutoCommit(false);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Calls {@link Connection#close()} on the {@link #conn} variable and then sets it to
	 * <code>null</code>.
	 *
	 * @throws SQLException
	 *             if <code>conn.close()</code> throws one.
	 */
	@Override
	public void closeDatabase() throws SQLException {
		lock.lock();
		try {
			conn.close();
			conn = null;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public DatabaseEntry queryDatabase(String lemma) throws SQLException {

		lock.lock();

		// Don't (!) put the ResultSet into the try-with-resource statement as we must first prepare
		// the lemma string.
		try (Statement stmt = conn.createStatement(); PreparedStatement prepStmt = conn.prepareStatement(QUERY_LEMMA)) {

			lemma = lemma.toLowerCase();
			prepStmt.setString(1, lemma);
			ResultSet rs = prepStmt.executeQuery();
			DatabaseEntry entry = new DatabaseEntry(lemma);

			// Is there no database entry for the given lemma?
			if (!rs.next()) {
				logger.info("Could not find entry for lemma \"" + lemma + "\"");
				return entry;
			}

			// Prepare the required variables.
			int id = rs.getInt("id"), transID;
			String phoneticString;
			TranscriptionType transType;
			WordClass wordClass;
			Variety variety;

			// Query the database.
			rs = stmt.executeQuery("SELECT * FROM transItems WHERE `id` = '" + id + "';");

			// Parse all responses into a DatabaseEntry object.
			while (rs.next()) {
				transID = rs.getInt("transID");
				phoneticString = rs.getString("transcription");
				transType = TranscriptionType.getTranscriptionType(rs.getInt("transType"));
				wordClass = WordClass.getWordClass(rs.getInt("wordClassID"));
				variety = Variety.getVariety(rs.getInt("varietyID"));

				// Add a new TranscriptionItem instance to the list.
				entry.addTranscriptionItem(
						new TranscriptionItem(transID, lemma, phoneticString, transType, wordClass, variety));
			}
			rs.close();

			return entry;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public TranscriptionItem queryTranscriptionItem(int transID) throws SQLException {
		lock.lock();
		try (Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT * FROM transItems WHERE `transID` = '" + transID + "';");

			// No entry in the database for the given transID?
			if (!rs.next()) {
				rs.close();
				throw new SQLException("Unable to retrieve transcription item by id: " + transID);
			}

			// Parse the info into a new TranscriptionItem object.
			String transcription = rs.getString("transcription");
			TranscriptionType transType = TranscriptionType.getTranscriptionType(rs.getInt("transType"));
			WordClass wordClass = WordClass.getWordClass(rs.getInt("wordClassID"));
			Variety variety = Variety.getVariety(rs.getInt("varietyID"));
			int lemmaID = rs.getInt("id");
			rs = stmt.executeQuery("SELECT * FROM lemmas WHERE `id` = '" + lemmaID + "'");

			// No corresponding lemma stored for some reason?
			if (!rs.next()) {
				throw new SQLException("Unable to retrieve lemma for transcription item by id: " + lemmaID);
			}

			String lemma = rs.getString("lemma");
			rs.close();

			return new TranscriptionItem(transID, lemma, transcription, transType, wordClass, variety);

		} finally {
			lock.unlock();
		}
	}

	@Override
	public TranscriptionItem addTranscription(String lemma, WordClass wordClass, Variety variety,
			TranscriptionType transType, String transcription) throws IllegalArgumentException, SQLException {

		lock.lock();
		try (Statement stmt = conn.createStatement();) {

			// Prepare the lemma string.
			lemma = lemma.toLowerCase();

			// Create a new prepared statement.
			PreparedStatement prepStmt = conn.prepareStatement(QUERY_LEMMA);
			prepStmt.setString(1, lemma);

			ResultSet rs = prepStmt.executeQuery();
			int lemmaID;

			// No corresponding lemma in database yet?
			if (!rs.next()) {
				prepStmt.close();
				prepStmt = conn.prepareStatement("INSERT INTO lemmas (`lemma`) VALUES (?);");
				prepStmt.setString(1, lemma);
				prepStmt.executeUpdate();
				prepStmt.close();
				prepStmt = conn.prepareStatement(QUERY_LEMMA);
				prepStmt.setString(1, lemma);
				rs = prepStmt.executeQuery();
			}

			lemmaID = rs.getInt("id");

			// Check for an existent entry. However, we don't need to check for transType here (as
			// it would make no sense).
			String queryString = "SELECT * FROM transItems WHERE `id` = '" + lemmaID + "' AND `transcription` = '"
					+ transcription + "' AND `wordClassID` = '" + wordClass.getID() + "' AND `varietyID` = '"
					+ variety.getID() + "';";
			rs = stmt.executeQuery(queryString);

			// Already in database?
			if (rs.next()) {
				logger.warning("Entry already exists in database!\nlemma ID: " + lemmaID + ", word class: " + wordClass
						+ ", variety: " + variety + ", transcription: /" + transcription + "/");
				throw new IllegalArgumentException("Duplicate entries are not allowed!");
			}

			// Insert the item into the database.
			stmt.executeUpdate(
					"INSERT INTO transItems (`id`, `transcription`, `transType`, `wordClassID`, `varietyID`) VALUES ('"
							+ lemmaID + "', '" + transcription + "', '" + transType.getID() + "', '" + wordClass.getID()
							+ "', '" + variety.getID() + "'); ");

			// Retrieve the newly inserted item and return it, especially with
			// regard to its ID.
			rs = stmt.executeQuery(queryString);
			if (!rs.next()) {
				throw new SQLException("Exception while retrieving newly inserted transcription item! It seems the item"
						+ "was stored in the databse, but did not match its query for some reason.\nlemma ID: "
						+ lemmaID + ", word class: " + wordClass + ", variety: " + variety + ", transcription: /"
						+ transcription + "/");
			}

			// Parse all responses into a new TranscriptionItem object.
			int transID = rs.getInt("transID");
			transcription = rs.getString("transcription");
			transType = TranscriptionType.getTranscriptionType(rs.getInt("transType"));
			wordClass = WordClass.getWordClass(rs.getInt("wordClassID"));
			variety = Variety.getVariety(rs.getInt("varietyID"));
			TranscriptionItem result = new TranscriptionItem(transID, lemma, transcription, transType, wordClass,
					variety);

			rs.close();
			conn.commit();

			return result;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void updateLemma(int lemmaID, String lemma) throws SQLException {
		lock.lock();
		try (PreparedStatement preStmt = conn
				.prepareStatement("UPDATE lemma SET `lemma` = ? WHERE `id` = '" + lemmaID + "';")) {
			lemma = lemma.toLowerCase();
			preStmt.setString(1, lemma);
			conn.commit();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public TranscriptionItem updateTranscription(int transID, WordClass wordClass, Variety variety,
			TranscriptionType transType, String transcription) throws SQLException {
		lock.lock();
		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("UPDATE transItems SET `transcription` =  '" + transcription + "', `transType` = '"
					+ transType.getID() + "', `wordClassID` = '" + wordClass.getID() + "', `varietyID` = '"
					+ variety.getID() + "' WHERE `transID` = '" + transID + "';");
			conn.commit();
			return queryTranscriptionItem(transID);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void deleteLemma(int lemmaID) throws SQLException {

		lock.lock();
		try (Statement stmt = conn.createStatement()) {
			logger.info("Deleting lemma and all its corresponding entries: " + lemmaID + " ...");

			stmt.executeUpdate("DELETE FROM lemmas WHERE `id`='" + lemmaID + "';"
					+ "DELETE FROM transItems WHERE `id`='" + lemmaID + "';");
			conn.commit();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void deleteLemma(String lemma) throws Exception {
		lock.lock();
		try {
			deleteLemma(getLemmaID(lemma));
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void deleteTranscription(TranscriptionItem item) throws SQLException, Exception {
		lock.lock();
		try (Statement stmt = conn.createStatement()) {
			logger.info("Deleting transcription item: " + item + "...");

			int transID = item.getId();
			stmt.executeUpdate("DELETE FROM transItems WHERE `transID`='" + transID + "';");

			// Was this the last transcription item? In that case, remove the lemma as well.
			if (queryDatabase(item.getLemma()).isEmpty()) {
				deleteLemma(item.getLemma());
			}

			conn.commit();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void deleteTranscription(int transID) throws Exception {
		lock.lock();
		try {
			TranscriptionItem item = queryTranscriptionItem(transID);
			if (item != null) {
				deleteTranscription(item);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * A convenience method for debugging purposes to create the tables inside an (empty) local
	 * <code>.db</code> file.
	 *
	 * @throws SQLException
	 */
	@Deprecated
	@SuppressWarnings("unused")
	private void createTables() throws SQLException {
		lock.lock();
		try (Statement stmt = conn.createStatement()) {
			logger.info("Creating SQL tables ...");

			String sql = "CREATE TABLE lemma (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lemma TEXT NOT NULL); "
					+ "CREATE TABLE transItems (transID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, id INTEGER NOT NULL, transcription TEXT NOT NULL, transType INTEGER, wordClassID INTEGER NOT NULL, varietyID INTEGER NOT NULL);"
					+ "CREATE TABLE wordClasses (wordClassID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, wordClass TEXT NOT NULL, abbreviation TEXT NOT NULL, contentWord INTEGER);"
					+ "CREATE TABLE varieties (varietyID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, variety TEXT NOT NULL, abbreviation TEXT NOT NULL);";
			stmt.executeUpdate(sql);
			conn.commit();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * A convenience method for debugging purposes. It will fill the <code>wordClasses</code> table
	 * inside the local <code>.db</code> file with its initial values. Since it will not check for
	 * dublicates, the table must be empty prior to calling this method.
	 *
	 * @throws SQLException
	 */
	@Deprecated
	@SuppressWarnings("unused")
	private void initWordClasses() throws SQLException {
		lock.lock();
		try (Statement stmt = conn.createStatement()) {
			logger.info("Initialising word class table ...");

			if (0 < 1) {
				throw new SQLException("Do not use the following code, unless the database has been erased! "
						+ "In that case comment out this exception.");
			}

			stmt.executeUpdate(AbstractSQLiteBridge.CMD_INIT_WORD_CLASSES);
			conn.commit();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * A convenience method for debugging purposes. It will fill the <code>varieties</code> table
	 * inside the local <code>.db</code> file with its initial values. Since it will not check for
	 * dublicates, the table must be empty prior to calling this method.
	 *
	 * @throws SQLException
	 */
	@Deprecated
	@SuppressWarnings("unused")
	private void initVarieties() throws SQLException {
		lock.lock();
		try (Statement stmt = conn.createStatement()) {
			logger.info("Initialising varieties ... ");

			if (0 < 1) {
				throw new SQLException("Do not use the following code, unless the database has been erased! "
						+ "In that case comment out this exception.");
			}

			stmt.executeUpdate(AbstractSQLiteBridge.CMD_INIT_VARIETIES);
			conn.commit();

		} finally {
			lock.unlock();
		}
	}

	/**
	 * A convenience method for debugging purposes. It will fill the <code>punctChars</code> table
	 * inside the local <code>.db</code> file with its initial values. Since it will not check for
	 * dublicates, the table must be empty prior to calling this method.
	 *
	 * @throws SQLException
	 */
	@Deprecated
	@SuppressWarnings("unused")
	private void initPunctChars() throws SQLException {
		lock.lock();
		try (Statement stmt = conn.createStatement()) {
			logger.info("Initialising punctuation characters ... ");

			if (0 < 1) {
				throw new SQLException("Do not use the following code, unless the database has been erased! "
						+ "In that case comment out this exception.");
			}

			stmt.executeUpdate(AbstractSQLiteBridge.CMD_INIT_PUNCTUATION_CHARS);
			conn.commit();

		} finally {
			lock.unlock();
		}
	}

	/**
	 * A convenience method for debugging purposes. It will fill the <code>currencyChars</code>
	 * table inside the local <code>.db</code> file with its initial values. Since it will not check
	 * for dublicates, the table must be empty prior to calling this method.
	 *
	 * @throws SQLException
	 */
	@Deprecated
	@SuppressWarnings("unused")
	private void initCurrencyChars() throws SQLException {
		lock.lock();
		try (Statement stmt = conn.createStatement()) {
			logger.info("Initialising currency characters ... ");

			if (0 < 1) {
				throw new SQLException("Do not use the following code, unless the database has been erased! "
						+ "In that case comment out this exception.");
			}

			stmt.executeUpdate(AbstractSQLiteBridge.CMD_INIT_CURRENCY_CHARS);
			conn.commit();

		} finally {
			lock.unlock();
		}
	}

	/* Getters and Setters */
	@Override
	public Variety[] getVarieties() throws SQLException {

		lock.lock();
		try (Statement stmt = conn.createStatement()) {

			logger.info("Parsing varieties ...");

			ArrayList<Variety> tempList = new ArrayList<Variety>();

			ResultSet rs = stmt.executeQuery("SELECT * FROM varieties");
			while (rs.next()) {
				tempList.add(
						new Variety(rs.getInt("varietyID"), rs.getString("variety"), rs.getString("abbreviation")));
			}
			stmt.close();
			rs.close();

			String logString = "";
			Variety[] result = new Variety[tempList.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = tempList.get(i);
				logString += "\n\t" + result[i];
			}

			logger.info("The following varieties have been parsed:" + logString);

			return result;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public WordClass[] getWordClasses() throws SQLException {

		lock.lock();
		try (Statement stmt = conn.createStatement()) {
			logger.info("Parsing word classes ...");

			ArrayList<WordClass> tempList = new ArrayList<WordClass>();

			ResultSet rs = stmt.executeQuery("SELECT * FROM wordClasses");
			while (rs.next()) {
				tempList.add(new WordClass(rs.getInt("wordClassID"), rs.getString("wordClass"),
						rs.getString("abbreviation"), rs.getBoolean("contentWord")));
			}
			stmt.close();
			rs.close();

			String logString = "";
			WordClass[] result = new WordClass[tempList.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = tempList.get(i);
				logString += "\n\t" + result[i];
			}

			logger.info("The following word classes have been parsed:" + logString);

			return result;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public PunctuationCharacter[] getPunctuationCharacters() throws SQLException {
		lock.lock();
		try (Statement stmt = conn.createStatement()) {
			logger.info("Parsing punctuation characters ...");

			ArrayList<PunctuationCharacter> tempList = new ArrayList<PunctuationCharacter>();

			ResultSet rs = stmt.executeQuery("SELECT * FROM punctChars");
			while (rs.next()) {
				tempList.add(new PunctuationCharacter(rs.getString("punctChar"), rs.getInt("delimiterMode")));
			}
			stmt.close();
			rs.close();

			String logString = "";
			PunctuationCharacter[] result = new PunctuationCharacter[tempList.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = tempList.get(i);
				logString += "\n\t" + result[i];
			}

			logger.info("The following punctuation characters have been parsed:" + logString);

			return result;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public CurrencyCharacter[] getCurrencyCharacters() throws SQLException {
		lock.lock();
		try (Statement stmt = conn.createStatement()) {
			logger.info("Parsing currency characters ...");

			ArrayList<CurrencyCharacter> tempList = new ArrayList<CurrencyCharacter>();

			ResultSet rs = stmt.executeQuery("SELECT * FROM currencyChars");
			while (rs.next()) {
				tempList.add(new CurrencyCharacter(rs.getString("currencyChar"), rs.getString("singular"),
						rs.getString("plural")));
			}
			stmt.close();
			rs.close();

			String logString = "";
			CurrencyCharacter[] result = new CurrencyCharacter[tempList.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = tempList.get(i);
				logString += "\n\t" + result[i];
			}

			logger.info("The following currency characters have been parsed:" + logString);

			return result;
		} finally {
			lock.unlock();
		}
	}

	@Override
	protected int getLemmaID(String lemma) throws SQLException {

		lock.lock();

		logger.info("Retrieving id for lemma: \"" + lemma + "\" ...");

		try (PreparedStatement prepStmt = conn.prepareStatement(QUERY_LEMMA)) {

			// Prepare the lemma string.
			lemma = lemma.toLowerCase();

			prepStmt.setString(1, lemma);

			ResultSet rs = prepStmt.executeQuery();
			if (!rs.next()) {
				logger.info("No such lemma stored in database!");
				return -1;
			}
			return (rs.getInt("id"));
		} finally {
			lock.unlock();
		}
	}
}
