package de.upb.t2t.control.database;

import java.util.logging.Logger;

import de.upb.t2t.control.sqlite.SQLiteBridgeDesktop;
import de.upb.t2t.model.CurrencyCharacter;
import de.upb.t2t.model.DatabaseEntry;
import de.upb.t2t.model.PunctuationCharacter;
import de.upb.t2t.model.TranscriptionItem;
import de.upb.t2t.model.TranscriptionType;
import de.upb.t2t.model.Variety;
import de.upb.t2t.model.WordClass;

/**
 * <p>
 * This is the central class for communication with the local SQL database. It is not (!)
 * responsible for communicating with the online database and thus potential damage done to the
 * local db file will be overwritten on the next update anyway. It offers functions to make queries
 * and modify <code>TranscriptionItem</code>s. The actual implementation of this class is dependent
 * on the underlying OS, as e.g. Android has its own library for SQLite. Note that the bridge
 * employs a singleton pattern and therefore its instance can be obtained by calling
 * {@link #getInstance()} from anywhere inside the application. The instance itself will be
 * initialised by the {@link SQLiteBridgeFactory}.
 * </p>
 * <p>
 * <b>Developer Note:</b> For developer convenience, there are a few static <code>String</code>s
 * stored inside this class. They are flagged as deprecated and should only be used for debugging.
 * With their help, the SQLite database may be re-initialised and some of the tables filled with
 * default values.
 * </p>
 *
 * @author Michel May (michel-may@gmx.de)
 * @see #queryDatabase(String)
 * @see #addTranscription(String, WordClass, Variety, TranscriptionType, String)
 * @see DatabaseEntry
 * @see TranscriptionItem
 * @see SQLiteBridgeFactory
 *
 */
public abstract class AbstractSQLiteBridge {

	/* Static Variables */
	/**
	 * The bridge's instance, depending on the OS.
	 *
	 * @see SQLiteBridgeFactory
	 */
	private static AbstractSQLiteBridge instance = SQLiteBridgeFactory.newSQLiteBridge();
	/** May be used to create the database's tables in case they have been erased. */
	@Deprecated
	protected static final String CMD_CREATE_TABLES = "CREATE TABLE lemmas (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lemma TEXT NOT NULL); "
			+ "CREATE TABLE transItems (transID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, id INTEGER NOT NULL, transcription TEXT NOT NULL, transType INTEGER, wordClassID INTEGER NOT NULL, varietyID INTEGER NOT NULL);"
			+ "CREATE TABLE wordClasses (wordClassID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, wordClass TEXT NOT NULL, abbreviation TEXT NOT NULL, contentWord INTEGER);"
			+ "CREATE TABLE varieties (varietyID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, variety TEXT NOT NULL, abbreviation TEXT NOT NULL);"
			+ "CREATE TABLE punctChars (punctChar TEXT PRIMARY KEY NOT NULL, delimiterMode INTEGER, comment TEXT);"
			+ "CREATE TABLE currencyChars (currencyChar TEXT PRIMARY KEY NOT NULL, singular TEXT NOT NULL, plural TEXT NOT NULL);";
	/** May be used to initialise the wordClasses table in case it has been erased. */
	@Deprecated
	protected static final String CMD_INIT_WORD_CLASSES = "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Adjective', 'adj', '1');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Adverb',				'rb',		'1');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Conjunction',			'conj',		'0');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Determiner',			'det',		'0');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Exclamation',			'excl',		'1');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Negator',				'neg',		'0');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Proper Noun',			'propN',	'1');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Common Noun',			'comN',		'1');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Pronoun',				'proN',		'0');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Numeral',				'num',		'0');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Preposition',			'prep',		'0');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('To-Infinitive Marker',	'to-inf',	'0');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Lexical Verb',			'lexV',		'1');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Modal Auxiliary',		'modAux',	'0');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Primary Auxiliary',	'primAux',	'0');"
			+ "INSERT INTO wordClasses (`wordClass`, `abbreviation`, `contentWord`) VALUES ('Catenative Verbs',		'catV',		'0');";
	/** May be used to initialise the varieties table in case it has been erased. */
	@Deprecated
	protected static final String CMD_INIT_VARIETIES = "INSERT INTO varieties (`variety`, `abbreviation`) VALUES ('British English', 'BrE');"
			+ "INSERT INTO varieties (`variety`, `abbreviation`) VALUES ('American English', 'AmE');"
			+ "INSERT INTO varieties (`variety`, `abbreviation`) VALUES ('Australian English', 'AuE');";
	/** May be used to initialise the punctChars table in case it has been erased. */
	@Deprecated
	protected static final String CMD_INIT_PUNCTUATION_CHARS = "INSERT INTO punctChars (`punctChar`, `delimiterMode`) VALUES (',', '1');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`) VALUES (':', '1');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`) VALUES ('\"', '1');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`, `comment`) VALUES ('-', '0', 'hyphen minus. This is what the user has on their keyboard. HTML Entity (decimal) &#45');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`, `comment`) VALUES ('‐', '1', 'hyphen. HTML Entity (decimal) &#8208');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`, `comment`) VALUES ('‑', '1', 'non-breaking hyphen. HTML Entity (decimal) &#8209');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`, `comment`) VALUES ('‒', '1', 'figure dash. HTML Entity (decimal) &#8210');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`, `comment`) VALUES ('–', '1', 'en dash. This is what programmes like LibreOffice will automatically extend the the hyphen minus character to in the flow of a text. HTML Entity (decimal) &#8211');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`, `comment`) VALUES ('—', '1', '(em) dash. HTML Entity (decimal) &#8212');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`, `comment`) VALUES ('―', '1', 'horizontal bar. HTML Entity (decimal) &#8213');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`, `comment`) VALUES ('−', '0', 'minus sign. HTML Entity (decimal) &#8722');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`) VALUES ('.', '2');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`) VALUES ('!', '2');"
			+ "INSERT INTO punctChars (`punctChar`, `delimiterMode`) VALUES ('?', '2');";
	/** May be used to initialise the currencyChars table in case it has been erased. */
	@Deprecated
	protected static final String CMD_INIT_CURRENCY_CHARS = "INSERT INTO currencyChars (`currencyChar`, `singular`, `plural`) VALUES ('$', 'dollar', 'dollars');"
			+ "INSERT INTO currencyChars (`currencyChar`, `singular`, `plural`) VALUES ('€', 'euro', 'euros');"
			+ "INSERT INTO currencyChars (`currencyChar`, `singular`, `plural`) VALUES ('£', 'pound', 'pounds');";

	/* Static Methods */
	public static AbstractSQLiteBridge getInstance() {
		return instance;
	}

	/* Object Variables */

	/* Constructors */
	/**
	 * Constructor for the {@link AbstractSQLiteBridge} class. In order to not violate the singleton
	 * pattern, this constructor is marked as protected.
	 */
	protected AbstractSQLiteBridge() {
		super();
	}

	/* Object Methods */
	/**
	 * Establishes a connection to the local database file, potentially locking it depending on its
	 * implementation.
	 *
	 * @throws Exception
	 */
	public abstract void openDatabase() throws Exception;

	/**
	 * Closes the connection to the local database, potentially unlocking it and freeing memory
	 * depending on its implementation.
	 *
	 * @throws Exception
	 */
	public abstract void closeDatabase() throws Exception;

	/**
	 * Queries the SQLite file for the given lemma and converts any information into a
	 * <code>DatabaseEntry</code> object. In the event that no entry inside the database matches the
	 * query, the returned database entry will be empty, but not <code>null</code>.
	 *
	 * @param lemma
	 *            the lemma to look up
	 * @return the corresponding <code>DatabaseEntry</code> object.
	 * @throws Exception
	 */
	public abstract DatabaseEntry queryDatabase(String lemma) throws Exception;

	/**
	 * Queries the local database for the transcription item whose ID matches the given one.
	 *
	 * @param transID
	 *            the transcription item's ID.
	 * @return the corresponding {@link TranscriptionItem} object.
	 * @throws Exception
	 */
	public abstract TranscriptionItem queryTranscriptionItem(int transID) throws Exception;

	/**
	 * Adds a transcription item to the local database.
	 *
	 * @param lemma
	 *            the transcription item's corresponding lemma.
	 * @param wordClass
	 *            the transcription item's corresponding word class.
	 * @param variety
	 *            the transcription item's corresponding variety.
	 * @param transType
	 *            the transcription item's corresponding type.
	 * @param transcription
	 *            the phonetic transcription <code>String</code>.
	 * @return the {@link TranscriptionItem} object as retrieved from the database after adding the
	 *         given data.
	 * @throws Exception
	 */
	public abstract TranscriptionItem addTranscription(String lemma, WordClass wordClass, Variety variety,
			TranscriptionType transType, String transcription) throws Exception;

	/**
	 * Updates the <code>String</code> of a lemma with the given ID inside the local database.
	 *
	 * @param lemmaID
	 *            the lemma's ID.
	 * @param lemma
	 *            the <code>String</code> to update the entry with.
	 * @throws Exception
	 */
	public abstract void updateLemma(int lemmaID, String lemma) throws Exception;

	/**
	 * Updates an existing transcription item inside the local database.
	 *
	 * @param transID
	 *            the transcription item's corresponding ID.
	 * @param wordClass
	 *            the transcription item's new word class.
	 * @param variety
	 *            the transcription item's new variety.
	 * @param transType
	 *            the transcription item's new type.
	 * @param transcription
	 *            the transcription item's new phonetic <code>String</code>.
	 * @return the updated {@link TranscriptionItem} object as retrieved from the database after
	 *         adding the given data.
	 * @throws Exception
	 */
	public abstract TranscriptionItem updateTranscription(int transID, WordClass wordClass, Variety variety,
			TranscriptionType transType, String transcription) throws Exception;

	/**
	 * Deletes a lemma from the local database. Since deleting a lemma from the lemmas table without
	 * clearing its corresponding transcription items may potentially damage the structure of the
	 * database (or at least lead to dead entries), this function is protected.
	 *
	 * @param lemmaID
	 *            the lemma's ID
	 * @throws Exception
	 */
	protected abstract void deleteLemma(int lemmaID) throws Exception;

	/**
	 * Deletes a lemma from the local database. Since deleting a lemma from the lemmas table without
	 * clearing its corresponding transcription items may potentially damage the structure of the
	 * database (or at least lead to dead entries), this function is protected.
	 *
	 * @param lemma
	 *            the lemma
	 * @throws Exception
	 */
	protected abstract void deleteLemma(String lemma) throws Exception;

	/**
	 * Deletes an existing transcription item from the local database.
	 *
	 * @param transID
	 *            the transcription item's corresponding ID.
	 * @throws Exception
	 */
	public abstract void deleteTranscription(int transID) throws Exception;

	/**
	 * Deletes an existing transcription item from the local database.
	 *
	 * @param item
	 *            the item to delete.
	 * @throws Exception
	 */
	public abstract void deleteTranscription(TranscriptionItem item) throws Exception;

	/* Getters and Setters */
	/**
	 * Queries the database for information on the varieties it contains and converts said info into
	 * an array of {@link Variety} objects. This function should be called, when the
	 * <code>Variety</code> class is being loaded for the first time.
	 *
	 * @return an array of all <code>Variety</code> objects that could be found in the database.
	 * @throws Exception
	 */
	public abstract Variety[] getVarieties() throws Exception;

	/**
	 * Queries the local database for information on the word classes it contains and converts said
	 * info into an array of {@link WordClass} objects. This function should be called, when the
	 * <code>WordClass</code> class is being loaded for the first time.
	 *
	 * @return an array of all <code>WordClass</code> objects that could be found in the database.
	 * @throws Exception
	 */
	public abstract WordClass[] getWordClasses() throws Exception;

	/**
	 * Queries the local database for information on the punctuation characters it contains and
	 * converts said info into an array of {@link PunctuationCharacter} objects. This function
	 * should be called, when the <code>PunctuationCharacter</code> class is being loaded for the
	 * first time.
	 *
	 * @return an array of all <code>PunctuationCharacter</code> objects that could be found in the
	 *         database.
	 * @throws Exception
	 */
	public abstract PunctuationCharacter[] getPunctuationCharacters() throws Exception;

	/**
	 * Queries the local database for information on the punctuation characters it contains and
	 * converts said info into an array of {@link CurrencyCharacter} objects. This function should
	 * be called, when the <code>CurrencyCharacter</code> class is being loaded for the first time.
	 *
	 * @return an array of all <code>CurrencyCharacter</code> objects that could be found in the
	 *         database.
	 * @throws Exception
	 */
	public abstract CurrencyCharacter[] getCurrencyCharacters() throws Exception;

	/**
	 * A convenience function to obtain the given lemma's internal ID from the local database. As no
	 * other part of the application needs to know about that number and it is only of importance
	 * for implementations of the {@link AbstractSQLiteBridge}, this function is marked as
	 * protected.
	 *
	 * @param lemma
	 *            the lemma whose ID to fetch.
	 * @return the lemma's corresponding ID.
	 * @throws Exception
	 */
	protected abstract int getLemmaID(String lemma) throws Exception;

	/* Sub-Classes */
	/**
	 * A small class whose only purpose is to the initialise the {@link AbstractSQLiteBridge}'s
	 * instance appropriately, depending on the current OS.
	 *
	 * @author Michel May (michel-may@gmx.de)
	 * @see #newSQLiteBridge()
	 */
	private static class SQLiteBridgeFactory {

		/**
		 * Retrieves the current OS and returns the corresponding implementation of
		 * {@link AbstractSQLiteBridge}.
		 *
		 * @return the appropriate instance of {@link AbstractSQLiteBridge}.
		 */
		private static AbstractSQLiteBridge newSQLiteBridge() {
			String os = System.getProperty("os.name").toLowerCase();
			Logger.getGlobal().info("Operating system is: " + os);

			// Android?
			if (os.contains("android")) {
				return null;
			}

			// iOS?
			if (os.contains("ios")) {
				return null;
			}

			// Desktop obviously
			return new SQLiteBridgeDesktop();
		}
	}
}
