package de.upb.t2t.control;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.MatchResult;

import de.upb.t2t.control.database.AbstractSQLiteBridge;
import de.upb.t2t.control.gui.transcriber.DelimiterButton;
import de.upb.t2t.control.gui.transcriber.MainTranscriptionPane;
import de.upb.t2t.control.gui.transcriber.TranscriptionSegment;
import de.upb.t2t.control.gui.transcriber.WordButton;
import de.upb.t2t.control.sqlite.SQLiteBridgeDesktop;
import de.upb.t2t.model.CurrencyCharacter;
import de.upb.t2t.model.DatabaseEntry;
import de.upb.t2t.model.PunctuationCharacter;
import de.upb.t2t.model.TranscriptionItem;
import de.upb.t2t.model.TranscriptionType;
import de.upb.t2t.model.Variety;
import de.upb.t2t.model.WordClass;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;

/**
 * This is one of the application's core classes. The only public method it offers is
 * {@link #transcribe(String} whose main purpose is to convert a given {@link String} into a series
 * of {@link TranscriptionSegment}s. These segments will hold the corresponding phonetic
 * transcription of said string and will be displayed inside the
 * {@link MainTranscriptionPane#outputField}. All other methods within this class simply provide
 * supportive functionality for the transcription process.
 *
 * @author Michel May (michel-may@gmx.de)
 * @see #transcribe(String)
 */
public class Transcriber {

	/* Static Variables */

	/* Static Methods */
	/**
	 * A convenience method that adds the given segment both to the given list, but also to the
	 * {@link MainTranscriptionPane#outputField}. For the latter, the JavaFx Application Thread is
	 * called.
	 *
	 * @param finishedSegment
	 *            the segment to add to both lists.
	 * @param finishedSegments
	 *            the working list to add the segment to.
	 */
	private static void addSegmentToListAndMainPane(TranscriptionSegment finishedSegment,
			List<TranscriptionSegment> finishedSegments) {
		finishedSegments.add(finishedSegment);
		Platform.runLater(() -> {
			MainController.getInstance().getTranscriptionPanel().getOutputField().getChildren().add(finishedSegment);
		});
	}

	/**
	 * A convenience function to retrieve the last element of a {@link List} of
	 * {@link TranscriptionSegment}s, just as {@link Stack#peek()} would. Additionally, any
	 * potential {@link IndexOutOfBoundsException} is caught and <code>null</code> returned instead.
	 *
	 * @param list
	 *            the list to retrieve the last element from.
	 * @return the last element of the list; <code>null</code> if the list was empty.
	 */
	private static TranscriptionSegment getLastSegment(List<TranscriptionSegment> list) {
		try {
			return list.get(list.size() - 1);
		} catch (IndexOutOfBoundsException e) {
			// Do nothing.
		}
		return null;
	}

	/**
	 * Evaluates whether the given <code>String</code> equals an expression that would require any
	 * following word, e.g. a currency character, to be in the singular. Its primary use is to
	 * prepare the transcription of any special character that needs to be replaced by the
	 * application prior to transcription. Examples for such expressions are "one", "a", "1", etc.
	 *
	 * @param string
	 *            the <code>String</code> to evaluate.
	 * @return <code>true</code> if the given <code>String</code> equals a singular expresion;
	 *         <code>false</code> otherwise.
	 * @see CurrencyCharacter
	 */
	private static boolean isSingularExpression(String string) {
		switch (string) {
		case "a":
		case "one":
		case "1":
		case "-1":
		case "1.00":
		case "-1.00":
		case "1.-":
		case "single":
			return true;
		}
		return false;
	}

	/**
	 * <p>
	 * A simple convenience function to check whether the given <code>String</code> ends with a
	 * minus character. If so then the minus would require to be transcribed, of course.
	 * </p>
	 * <p>
	 * <b>Impotant:</b> Please note that there are roughly 10 UTF-8 characters that look like a
	 * minus, but in fact are not. These may be various types of dashes or hyphens. Currently, this
	 * function will only return <code>true</code> for the HTML entities &#45 (hyphen minus) and
	 * &#8722 (minus sign).
	 * </p>
	 *
	 * @param string
	 *            the <code>String</code> to evaluate.
	 * @return <code>true</code> if the <code>String</code> ends with a minus character.
	 */
	private static boolean stringEndsWithMinusCharacter(String string) {
		if (string.isEmpty()) {
			return false;
		}

		switch (string.charAt(string.length() - 1)) {
		case '-':
		case '−':
			return true;
		default:
			return false;
		}
	}

	/**
	 * A convenience function that simply checks whether the given <code>char</code> equals a vowel
	 * / vocalic sound. This may be important when deciding which transcription to select for
	 * example for the lemma "the".
	 *
	 * @param c
	 *            the character to evaluate.
	 * @return <code>true</code> if the given character equals a vowel; <code>false</code>
	 *         otherwise.
	 */
	private static boolean isVocalicSound(char c) {

		switch (c) {
		case 'a': // diphtongs
		case 'ɑ':
		case 'ʌ':
		case 'æ':
		case 'ɜ':
		case 'e':
		case 'ə':
		case 'i':
		case 'ɪ':
		case 'ɔ':
		case 'ɒ':
		case 'u':
		case 'ʊ':
			return true;
		default:
			return false;
		}
	}

	/* Object Variables */
	/** The {@link Transcriber} instance's {@link Logger} object. */
	private Logger logger;

	/* Constructors */
	/**
	 * Constructor for the {@link Transcriber} class.
	 */
	public Transcriber() {
		logger = Logger.getLogger(getClass().getName());
	}

	/* Object Methods */
	/**
	 * <p>
	 * This probably is the most central method in the entire application. Its use is to parse a
	 * given <code>String</code> and transform it into a phonetic transcription. As feedback is
	 * given to the user about the progress of the current state of the function, it <b>should be
	 * called on a separate thread!</b> The procedure includes the following steps which are for the
	 * sake of readability not written in pseudo-code.
	 * </p>
	 * <ul>
	 * <li>Dealing with any potential non-word characters, such as currencies ('$', '€', ...) or
	 * numerals ('2015', '-1,234,567.89', ...), but also punctuation characters. The latter includes
	 * applying the notational delimiter elements, i.e. '/', '|' and '||'.</li>
	 * <li>Breaking up the <code>String</code> into segments that can be looked up in the database.
	 * </li>
	 * <li>Providing the corresponding transcription pieces.</li>
	 * <li>Assigning the appropriate word class of an item. However, since this cannot be done
	 * algorithmically, in case of a conflict (e.g. "might" as a modal auxiliary vs. "might" as a
	 * common noun), the function will appropriate the respective buttons accordingly so that the
	 * user can make the choice instead.</li>
	 * </ul>
	 *
	 * @param input
	 *            The input <code>String</code> to transcribe.
	 * @throws IOException
	 *             if the instantiation of any of the {@link TranscriptionSegment}s fails.
	 * @throws SQLException
	 *             if the currently used instance {@link AbstractSQLiteBridge} is of type
	 *             {@link SQLiteBridgeDesktop} and something went wrong while querying the database.
	 * @throws Exception
	 *             most likely if the currently used instance {@link AbstractSQLiteBridge} throws
	 *             one while querying the database.
	 * @see TranscriptionSegment
	 * @see MainTranscriptionPane
	 * @see AbstractSQLiteBridge
	 * @see CurrencyCharacter
	 * @see PunctuationCharacter
	 */
	public void transcribe(String input) throws IOException, SQLException, Exception {

		// In case the function has been called on the JavaFx Application Thread, log a warning.
		if (Platform.isFxApplicationThread()) {
			logger.warning("Called on the JavaFx Application Thread! The user will receive funny feedback!");
		}

		// Prepare the input and write a log message.
		input = input.trim();
		input = input.replaceAll("\\s{2,}", " ");
		logger.info("\n*********************************************************" + "\nTranscribing: " + input + " ..."
				+ "\n*********************************************************");

		// Prepare objects we will need for processing the sentence.
		AbstractSQLiteBridge sqlBridge = AbstractSQLiteBridge.getInstance();
		List<MatchResult> wordCharacterGroups, nonWordCharacters;
		int indexLeadingLastElement, indexTrailingFirstElement;
		String strLeading, strMiddle, strTrailing;
		PunctuationCharacter punctChar;
		CurrencyCharacter currencyChar;
		TranscriptionSegment tempSegment, finishedSegment;
		List<Button> transButtons;

		// Clear whatever old transcription may still be displayed.
		Platform.runLater(() -> {
			MainController.getInstance().getTranscriptionPanel().getOutputField().getChildren().clear();
			new Thread(() -> {
				System.gc();
			}).start();
		});

		// Prepare the list of finished segments and already add the initial delimiter.
		List<TranscriptionSegment> finishedSegments = new LinkedList<TranscriptionSegment>();

		addSegmentToListAndMainPane(newEnclosingDelimiterSegment(), finishedSegments);

		// We're good to go. Cleft the sentence into segments and process each of them.
		String[] segments = input.split("\\s");
		for (int i = 0; i < segments.length; i++) {

			logger.fine("Analysing segment " + i + ": \"" + segments[i] + "\" ...");
			MainController.getInstance().getTranscriptionPanel().progressFeedback(
					((double) i + 1) / ((double) segments.length),
					"Transcribing segment " + (i + 1) + " of " + segments.length + " ...");

			/*
			 * Before we can look up any translation for our sentence part, we need to distinguish
			 * from the actual lemma potential non-word characters such as punctuation or, for
			 * example, those indicating a currency ($, €, £, ...). Please note that as per
			 * definition of the regex patterns in Java, digits also count as word characters (\w).
			 * This will come in handy later on.
			 *
			 * Moreover, please be aware that the nonWordCharacters list will hold match results of
			 * length 1 (by means of its regex pattern). An input such as "'hello!'," would lead to
			 * the list containing the following items: "'", "!", "'", ",". Contrary to this
			 * behaviour, the wordCharacterGroups (as its name indicates) will be filled with result
			 * matches of various length. For instance, "well-known" would be split into "well" and
			 * "known".
			 */
			wordCharacterGroups = Statics.getAllRegexMatches(segments[i], "[\\w&&[^_]]+");
			nonWordCharacters = Statics.getAllRegexMatches(segments[i], "[\\W_]");
			finishedSegment = new TranscriptionSegment();
			tempSegment = null;

			// //////////////////////// //
			// NON-WORD CHARACTERS ONLY //
			// //////////////////////// //
			if (wordCharacterGroups.size() == 0) {

				logger.finer("No word character in this segment.");
				finishedSegment.getLemmaButton().setText(segments[i]);

				// Only a single character (e.g. "-")?
				if (nonWordCharacters.size() == 1) {

					logger.finer("Only a single character in the input.");

					String character = nonWordCharacters.get(0).group();
					tempSegment = getLastSegment(finishedSegments);

					// Are we dealing with a punctuation character?
					if (PunctuationCharacter.isPunctuationCharacter(character)) {

						logger.finer("Punctuation character found.");

						// Only add a delimiter if this is not the last segment and if there is no
						// preceding delimiter from the previous segment/sentence start.
						if (tempSegment != null && !tempSegment.hasTrailingDelimiter() && i < segments.length - 1) {

							logger.finer("Adding inner delimiter segment ...");
							finishedSegment = newInnerDelimiterSegment(character);

						} else {
							logger.finer("Adding segment without transcription ...");
						}
						addSegmentToListAndMainPane(finishedSegment, finishedSegments);
						continue;
					}

					// Are we dealing with a currency character?
					if (CurrencyCharacter.isCurrencyCharacter(character)) {

						logger.finer("Currency character found.");

						currencyChar = CurrencyCharacter.getCurrencyChar(character);

						// Determine whether we need to look up the singular or plural.
						if (tempSegment != null) {
							if (isSingularExpression(tempSegment.getLemmaButton().getText())) {
								logger.finer("Singular form required.");
								finishedSegment.getButtonBox().getChildren()
										.add(new WordButton(sqlBridge.queryDatabase(currencyChar.getSingularLemma())));
							} else {
								logger.finer("Plural form required.");
								finishedSegment.getButtonBox().getChildren()
										.add(new WordButton(sqlBridge.queryDatabase(currencyChar.getPluralLemma())));
							}

						} else {

							logger.finer("Unable to decide for singular/plural!");

							/*
							 * This is the first segment in the input. We have no means of
							 * distinguishing whether to look up the singular or plural. As a
							 * workaround, we'll do something unique and simply offer both.
							 */
							DatabaseEntry sg = sqlBridge.queryDatabase(currencyChar.getSingularLemma());
							DatabaseEntry pl = sqlBridge.queryDatabase(currencyChar.getPluralLemma());
							for (Entry<WordClass, Map<Variety, List<TranscriptionItem>>> wordClassEntry : pl
									.getMatchedItems().entrySet()) {
								for (Entry<Variety, List<TranscriptionItem>> varietyEntry : wordClassEntry.getValue()
										.entrySet()) {
									for (TranscriptionItem item : varietyEntry.getValue()) {
										sg.addTranscriptionItem(item);
									}
								}
							}
							finishedSegment.getButtonBox().getChildren().add(new WordButton(sg));
						}
						addSegmentToListAndMainPane(finishedSegment, finishedSegments);
						continue;
					}

					// None of the above? Nothing special we can do then.
				}

				// So there are multiple non-word characters (as in "..."). Not much we can do here.
				logger.finer("Bluntly adding new transcription segment with the bare string as the queried item");
				finishedSegment.getButtonBox().getChildren().add(new WordButton(sqlBridge.queryDatabase(segments[i])));
				addSegmentToListAndMainPane(finishedSegment, finishedSegments);
				continue;
			}

			// Reset the helper objects.
			indexLeadingLastElement = -1;
			indexTrailingFirstElement = -1;
			strLeading = "";
			strMiddle = "";
			strTrailing = "";
			punctChar = null;
			currencyChar = null;
			transButtons = new ArrayList<Button>();

			/*
			 * So far, we have made sure that there actually is a word that we can transcribe.
			 * However, we still need to distinguish the actual lemma from whatever other characters
			 * there might be. We will begin by dissecting the segment string into three parts, the
			 * first and the last of which containing any non-word characters such as punctuation or
			 * e.g. currency characters.
			 */
			logger.fine("Extrapolating lemma ...");
			if (!nonWordCharacters.isEmpty()) {

				// Leading characters first ...
				if (nonWordCharacters.get(0).start() == 0) {
					int j = 0;
					do {
						indexLeadingLastElement = j;
						strLeading += nonWordCharacters.get(j).group();
						j++;
					} while (j < nonWordCharacters.size()
							&& nonWordCharacters.get(j).end() <= wordCharacterGroups.get(0).start());
				}

				// ... then trailing ones ...
				if (nonWordCharacters.get(nonWordCharacters.size() - 1).end() == segments[i].length()) {
					int j = nonWordCharacters.size() - 1;
					do {
						indexTrailingFirstElement = j;
						strTrailing = nonWordCharacters.get(j).group() + strTrailing;
						j--;
					} while (j >= 0 && nonWordCharacters.get(j).start() >= wordCharacterGroups
							.get(wordCharacterGroups.size() - 1).end());
				}
			}

			// ... and lastly the lemma string itself.
			strMiddle = segments[i].substring(
					indexLeadingLastElement < 0 ? 0 : nonWordCharacters.get(indexLeadingLastElement).end(),
					indexTrailingFirstElement < 0 ? segments[i].length()
							: nonWordCharacters.get(indexTrailingFirstElement).start());

			/*
			 * At this point, we are finally able to begin with the actual work. The strMiddle holds
			 * the lemma, while the other 2 strings contain whatever other characters there may be.
			 * There are several use cases that need attending now. We must make sure that leading
			 * punctuation may sometimes lead to a delimiter element (e.g. if a direct speech
			 * begins). After that, the lemma is to be transcribed. However, it might be possible
			 * for a user to have for example given a numeral in digits which have to be transformed
			 * prior to querying the database. Lastly, any trailing punctuation must be
			 * appropriately dealt with.
			 */

			// /////////////////// //
			// LEADING PUNCTUATION //
			// /////////////////// //
			if (!strLeading.isEmpty()) {

				punctChar = findHighestPunctCharInList(nonWordCharacters.subList(0, indexLeadingLastElement + 1));

				if (punctChar != null) {

					// Quotation marks are a special case.
					if (punctChar.getCharacter() == '"' && strTrailing.contains("\"")) {

						// Do nothing. Just a single word being highlighted as in 'He said a short
						// "hello"'.
						logger.finer("Not adding a leading delimiter element as this segment is surrounded by \" \"!");

					} else if (punctChar.getDelimiterMode() > 0) {

						// Make sure there isn't already a delimiter element preceding this one.
						tempSegment = getLastSegment(finishedSegments);
						if (tempSegment != null && !tempSegment.hasTrailingDelimiter()) {
							logger.finer("Adding new delimiter element as there is non preceding this segment.");
							transButtons
									.add(new DelimiterButton(punctChar.getDelimiterMode() == 2 ? "||" : "|", false));
						}
					}
				}
			}

			// /////////////////// //
			// LEMMA TRANSCRIPTION //
			// /////////////////// //
			// Is there any digit in the input?
			if (strMiddle.matches(".*\\d+.*")) {

				logger.fine("Found digits inside the input.");

				// Remove any commas as in "12,345" and parse the string to a double.
				String string = strMiddle.replace(",", "");
				String decimalPart = "";
				long integerPart = -1;

				try {
					// Is this a decimal number?
					if (string.matches("\\A\\d+\\.\\d+\\z")) {
						String[] numParts = string.split("\\.");
						integerPart = Long.parseLong(numParts[0]);
						decimalPart = numParts[1];
					} else {
						integerPart = Long.parseLong(string);
					}
				} catch (NumberFormatException e) {
					logger.info("Unable to parse long value from input \"" + string + "\"!");
				}

				// Parsing failed.
				if (integerPart < 0) {
					finishedSegment.getLemmaButton().setText(strMiddle);
					transButtons.add(new WordButton(sqlBridge.queryDatabase(strMiddle)));

				} else {

					// We have successfully parsed the numeral and are now ready to transcribe it.
					logger.finer("Numeral in digit form identified.");

					// Could this numeral be representing a year?
					if (!stringEndsWithMinusCharacter(strLeading) && decimalPart.isEmpty() && integerPart < 2000
							&& integerPart > 100) {

						logger.finer("This numeral could represent a year int the interval of (100, 2000).");

						// We have to store the two ways of transcribing such a numeral so that the
						// user is able to switch between them when clicking the lemma button.
						List<WordButton> transYear = new ArrayList<WordButton>();
						List<WordButton> transCommon = new ArrayList<WordButton>();

						// We will assume that this actually is a year so let's transcribe it.
						transYear.addAll(lookUpNumeralTensAndOnes((int) integerPart / 100));
						transYear.add(new WordButton(sqlBridge.queryDatabase("hundred")));
						int tensAndOnes = (int) integerPart % 100;
						if (tensAndOnes != 0) {
							transYear.addAll(lookUpNumeralTensAndOnes((int) integerPart % 100));
						}
						transButtons.addAll(transYear);

						// In case it is just a "common" numeral, let's grab that transcription as
						// well.
						transCommon.addAll(lookUpNumeral(integerPart, ""));

						/*
						 * We'll have to give the user the option to choose between the year and a
						 * "default" numeral representation. Therefore, we will make the lemma
						 * button accessible, prompting the user for the way they wish the numeral
						 * to be transcribed.
						 */
						Button lemmaButton = finishedSegment.getLemmaButton();
						lemmaButton.setText(strMiddle);
						lemmaButton.setDisable(false);
						final TranscriptionSegment yearSegment = finishedSegment;

						lemmaButton.setOnAction((ActionEvent event) -> {

							// Show the choice dialogue first.
							Alert choiceAlert = new Alert(AlertType.CONFIRMATION);
							choiceAlert.setTitle("Year or Common Numeral?");
							choiceAlert.setHeaderText("Does this numeral represent a year?");
							choiceAlert.setContentText("If you choose \"Yes\" then \"" + lemmaButton.getText()
									+ "\" will be transcribed as a year numeral. If you choose \"No\" then it will be treated as a common numeral.\n\n"
									+ "Take a short look at the following example for clarification. If \"1800\" signifies a year, then we usually say \"eighteen hundred\". However, if it occurs in a different context, the same numeral becomes \"one thousand eight hundred\".");

							ButtonType typeYes = new ButtonType("Yes");
							ButtonType typeNo = new ButtonType("No");
							ButtonType typeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
							choiceAlert.getButtonTypes().setAll(typeYes, typeNo, typeCancel);

							Optional<ButtonType> result = choiceAlert.showAndWait();
							Platform.runLater(() -> {

								// Set the respective transcription.
								ObservableList<Node> children = yearSegment.getButtonBox().getChildren();
								if (!result.get().equals(typeCancel)) {
									children.clear();
									children.addAll(result.get().equals(typeYes) ? transYear : transCommon);
								}
							});
						});

					} else {

						// If there is one, migrate the minus character from the leading to the
						// middle string for nicer display in the main transcription panel.
						if (stringEndsWithMinusCharacter(strLeading)) {
							strMiddle = strLeading.charAt(strLeading.length() - 1) + strMiddle;
							strLeading = strLeading.substring(0, strLeading.length() - 1);

							// Transcribe the minus.
							transButtons.add(new WordButton(sqlBridge.queryDatabase("minus")));
						}

						// Now transcribe the number.
						transButtons.addAll(lookUpNumeral(integerPart, decimalPart));
					}
				}

			} else {
				// Just a normal word.
				transButtons.add(new WordButton(sqlBridge.queryDatabase(strMiddle)));
			}

			// //////////////////// //
			// TRAILING PUNCTUATION //
			// //////////////////// //
			if (!strTrailing.isEmpty()) {

				// Check for any currency char that might directly follow the middle string.
				try {
					currencyChar = CurrencyCharacter.getCurrencyChar(strTrailing.charAt(0));

					// Transcribe the currency character and migrate it to the middle string.
					if (isSingularExpression(strMiddle)) {
						transButtons.add(new WordButton(sqlBridge.queryDatabase(currencyChar.getSingularLemma())));
					} else {
						transButtons.add(new WordButton(sqlBridge.queryDatabase(currencyChar.getPluralLemma())));
					}
					strMiddle += currencyChar.getCharacter();
					strTrailing = strTrailing.substring(1);

				} catch (NoSuchElementException e) {
					// Obviously not a currency character.
				}

				punctChar = findHighestPunctCharInList(
						nonWordCharacters.subList(indexTrailingFirstElement, nonWordCharacters.size()));

				if (punctChar != null) {

					// Quotation marks are a special case.
					if (punctChar.getCharacter() == '"' && strLeading.contains("\"")) {

						// Do nothing. Just a single word being highlighted as in 'he said a short
						// "hello"'.
						logger.finer("Not adding a trailing delimiter element as this segment is surrounded by \" \"!");

					} else if (punctChar.getDelimiterMode() > 0 && i < segments.length - 1) {

						// Finally add the delimiter element.
						logger.finer("Adding trailing delimiter ...");
						tempSegment = getLastSegment(finishedSegments);
						transButtons.add(new DelimiterButton(punctChar.getDelimiterMode() == 2 ? "||" : "|", false));
					}
				}
			}

			finishedSegment.setLeadingString(strLeading);
			finishedSegment.getLemmaButton().setText(strMiddle);
			finishedSegment.setTrailingString(strTrailing);
			finishedSegment.getButtonBox().getChildren().addAll(transButtons);
			addSegmentToListAndMainPane(finishedSegment, finishedSegments);

		}

		// Add the closing delimiter.
		addSegmentToListAndMainPane(newEnclosingDelimiterSegment(), finishedSegments);

		// Do the post-processing outside the JavaFx Application Thread.
		doPostProcessing(finishedSegments);
	}

	/**
	 * <p>
	 * This function should only be called after the finished {@link TranscriptionSegment}s have
	 * been added to the {@link MainTranscriptionPane}. It will then post process that list for user
	 * convenience by pre-selecting the most likely transcriptions, colourising potential conflicts
	 * and requesting focus on the first of these conflicts.
	 * </p>
	 * <p>
	 * Note that just like the {@link #transcribe(String)} function, this should not be called on
	 * the JavaFx Application Thread!
	 * </p>
	 *
	 * @param finishedSegments
	 *            the same list object of transcription segments that was added to the main
	 *            transcription pane.
	 */
	private void doPostProcessing(List<TranscriptionSegment> finishedSegments) {

		// In case the function has been called on the JavaFx Application Thread, log a warning.
		if (Platform.isFxApplicationThread()) {
			logger.warning("Called on the JavaFx Application Thread! The user will receive funny feedback!");
		}

		logger.fine("Doing post-processing ...");

		/*
		 * Now that the segments are added, we will run through all of them in order to do some
		 * "post-processing". This simply means that we highlight any word class conflicts and treat
		 * the special case of the determiner "the" (otherwise, people will have to adjust each and
		 * every of them).
		 */
		boolean firstConflict = true, anyButtonFocused = false;
		DatabaseEntry data;
		TranscriptionSegment seg;
		for (int i = 0; i < finishedSegments.size(); i++) {

			MainController.getInstance().getTranscriptionPanel().progressFeedback(
					((double) i + 1) / ((double) finishedSegments.size()),
					"Doing post-processing " + (i + 1) + " of " + finishedSegments.size() + " ...");

			seg = finishedSegments.get(i);

			for (Node node : seg.getButtonBox().getChildren()) {
				if (node instanceof WordButton) {

					// Even if this button might not be in conflicht, let's request focus anyway
					// so that we don't stay in the input field.
					if (!anyButtonFocused) {
						anyButtonFocused = true;
						Platform.runLater(() -> {
							node.requestFocus();
						});
					}

					// Prioritise conflict buttons over general ones when requesting focus.
					if (((WordButton) node).isInWordClassConflict()) {
						Platform.runLater(() -> {
							node.getStyleClass().add("conflict");
						});
						if (firstConflict) {
							firstConflict = false;
							Platform.runLater(() -> {
								node.requestFocus();
							});
						}
					}

					// Let's see if we're dealing with "the".
					data = ((WordButton) node).getData();
					if (data.getLemma().equals("the")) {

						logger.finer("\"the\" identified.");

						try {

							// Fetch the possible transcriptions and the first phonetic string
							// following this item.
							List<TranscriptionItem> transcriptions = data.getTranscriptionItems(
									WordClass.getWordClass("det"), Variety.getPreferredVariety());
							String nextPhoneticString = "";
							if (finishedSegments.get(i + 1).hasTranscriptionButtons()) {
								for (Node button : finishedSegments.get(i + 1).getButtonBox().getChildren()) {
									if (button instanceof WordButton && !((WordButton) button).getData().isEmpty()) {
										nextPhoneticString = ((WordButton) button).getData().getSelectedItem()
												.getPhoneticString();
									}
								}
							}

							logger.finer("Next phonetic string is: \"" + nextPhoneticString + "\".");

							// Can't we select the fitting transcription? Another conflict then ...
							if (nextPhoneticString.isEmpty() || transcriptions.isEmpty()) {
								Platform.runLater(() -> {
									if (!node.getStyleClass().contains("conflict")) {
										node.getStyleClass().add("conflict");
									}
								});
								if (firstConflict) {
									firstConflict = false;
									Platform.runLater(() -> {
										node.requestFocus();
									});
								}

							} else {

								// Depending on how the lemma in the following begins, select the
								// appropriate transcription.
								for (TranscriptionItem item : transcriptions) {

									// Only weak forms here.
									if (!item.getTransType().equals(TranscriptionType.WEAK)) {
										continue;
									}

									// Pre-select the schwa version as default.
									if (item.getPhoneticString().endsWith("ə")) {
										logger.finer("Pre-selecting schwa version ...");
										Platform.runLater(() -> {
											item.select();
										});
										break;
									}
								}

								/*
								 * So what shall it be, 'ə' or 'i'? It could still be that the next
								 * transcription starts with a vowel. Thus, we'd need to select the
								 * intermediate i version.
								 */
								if (isVocalicSound(nextPhoneticString.charAt(0))) {
									for (TranscriptionItem item : transcriptions) {

										// Only weak forms here.
										if (!item.getTransType().equals(TranscriptionType.WEAK)) {
											continue;
										}

										// This time, select the other transcription.
										if (item.getPhoneticString().endsWith("i")) {
											logger.finer("Overwriting schwa version by intermediate i version ...");
											Platform.runLater(() -> {
												item.select();
											});
											break;
										}
									}
								}
							}
						} catch (NoSuchElementException e) {
							logger.log(Level.WARNING,
									"Word class or preferred variety not found while adjusting \"the\" transcription segment!",
									e);
						}

					}
				}
			}
		}
	}

	/**
	 * Returns a new instance of a {@link TranscriptionSegment} that contains nothing
	 * transcriptionwise but the notational slash ("/") that signals the beginning and ending of a
	 * transcription.
	 *
	 * @return a new <code>TranscriptionSegment</code> containing the notational delimiting
	 *         character '/'.
	 * @throws IOException
	 *             if the {@link TranscriptionSegment} constructor throws one.
	 * @throws URISyntaxException
	 *             if {@link TranscriptionSegment#TranscriptionSegment()} throws one.
	 */
	private TranscriptionSegment newEnclosingDelimiterSegment() throws IOException, URISyntaxException {

		// Set the label to be empty and disable the button.
		TranscriptionSegment result = new TranscriptionSegment("");
		result.getLemmaButton().setVisible(false);
		result.getButtonBox().getChildren().add(new DelimiterButton("/", true));
		return result;
	}

	/**
	 * Returns a new instance of a {@link TranscriptionSegment} that contains nothing
	 * transcriptionwise but the notational single or double bars that indicate the original
	 * punctuation such as ".", "," or "?". The function automatically returns the appropriate
	 * element based on the given punctuation, including <code>null</code> if the given
	 * <code>String</code> does not require to be transcribed at all.
	 *
	 * @param punctuation
	 *            the punctuation character.
	 *
	 * @return the corresponding transcription segment.
	 *
	 * @throws IOException
	 *             if {@link TranscriptionSegment#TranscriptionSegment(String)} throws one.
	 * @throws URISyntaxException
	 *             if {@link TranscriptionSegment#TranscriptionSegment(String)} throws one.
	 */
	private TranscriptionSegment newInnerDelimiterSegment(String punctuation) throws IOException, URISyntaxException {

		// Get the corresponding punctuation character first.
		PunctuationCharacter punctChar = PunctuationCharacter.getPunctChar(punctuation);

		// Create the element and already set the label.
		TranscriptionSegment result = new TranscriptionSegment(punctuation);

		// Depending on what punctuation we have, set the button's text
		// accordingly, then disable it.
		DelimiterButton button = new DelimiterButton("", false);
		switch (punctChar.getDelimiterMode()) {
		case 1:
			button.setText("|");
			break;
		case 2:
			button.setText("||");
			break;
		}
		result.getButtonBox().getChildren().add(button);
		return result;
	}

	/**
	 * A convenience function that will run through the given list and, should there be anyone
	 * inside, return the punctuation character with the highest delimier mode. This is useful when
	 * deciding which notational bar delimiter to add ('|' vs. '||').
	 *
	 * @param nonWordCharacters
	 *            list of non-word character match results from a regex search.
	 * @return the {@link PunctuationCharacter} with the highest delimiter mode inside the given
	 *         list; <code>null</code> if there isn't any
	 */
	private PunctuationCharacter findHighestPunctCharInList(List<MatchResult> nonWordCharacters) {
		List<PunctuationCharacter> matches = new ArrayList<PunctuationCharacter>(nonWordCharacters.size());
		for (MatchResult mr : nonWordCharacters) {
			try {
				matches.add(PunctuationCharacter.getPunctChar(mr.group()));
			} catch (IllegalArgumentException e) {
				logger.log(Level.WARNING,
						"Unexpected IllegalArgumentException while retrieving PunctuationCharacter object: \""
								+ mr.group() + "\"!",
						e);
			} catch (NoSuchElementException e) {
				// Do nothing.
			}
		}
		matches.sort((PunctuationCharacter char1, PunctuationCharacter char2) -> {
			// Sort in descending order so that we can grab the first element.
			if (char1.getDelimiterMode() < char2.getDelimiterMode()) {
				return 1;
			}
			if (char1.getDelimiterMode() == char2.getDelimiterMode()) {
				return 0;
			}
			return -1;
		});
		return matches.size() == 0 ? null : matches.get(0);
	}

	/**
	 * <p>
	 * This function translates (almost) any given number into its corresponding transcription. For
	 * easier processing, the integer part of said number is given as a <code>long</code> (thereby
	 * predetermining the maximum range of possible number transcription), while the decimal part is
	 * a (well-formatted) <code>String</code>. The '.' character will automatically be added to the
	 * transcription should the decimal string not be empty. As an example, an input such as
	 * <center><b>"12345.678"</b> would lead to the follwing transcription: <b>/twelv θaʊznd θriː
	 * hʌndred fɔːti faɪv dɒt sɪks sevn eɪt/</b></center>
	 * </p>
	 * <p>
	 * Note that it will not take into account the special case of year numbers between 2000 and
	 * 100. These need special treatment.
	 * </p>
	 *
	 * @param integerPart
	 *            the integer part of the number to transcribe, e.g. "<b>12345</b>.351".
	 * @param decimalPart
	 *            the decimal part of the number to transcribe, e.g. "12345.<b>351</b>".
	 * @return a list of {@link WordButton}s that constitute the transcription.
	 * @throws NumberFormatException
	 *             most likely if the decimalPart <code>String</code> was malformed, but possibly
	 *             also if anything unexpected happened while transcribing the integer part.
	 * @throws SQLException
	 *             if the currently used instance {@link AbstractSQLiteBridge} is of type
	 *             {@link SQLiteBridgeDesktop} and something went wrong while querying the database.
	 * @throws Exception
	 *             most likely if the currently used instance {@link AbstractSQLiteBridge} throws
	 *             one while querying the database.
	 */
	private List<WordButton> lookUpNumeral(long integerPart, String decimalPart)
			throws NumberFormatException, SQLException, Exception {

		logger.finer("Looking up numeral: " + integerPart + "." + (decimalPart.isEmpty() ? "0" : decimalPart) + " ...");

		// Prepare the required objects.
		AbstractSQLiteBridge sqlBridge = AbstractSQLiteBridge.getInstance();
		DatabaseEntry hundred = sqlBridge.queryDatabase("hundred");
		String numString = integerPart + "";
		String area = "";
		List<WordButton> result = new ArrayList<WordButton>();
		char[] digits = numString.toCharArray();
		int tempNum;
		boolean transcribeArea = false; // Helps preventing the "thousand", "million", ... from
										// being transcribed in case of e.g. "1,000,222".

		// Deal with input such as "0" or "0.355".
		if (integerPart == 0) {

			logger.fine("The integer part was simply 0.");
			result.add(new WordButton(sqlBridge.queryDatabase("zero")));

		} else {

			// Transcribe those digits first that come before the dot.
			for (int i = 0; i < digits.length; i++) {

				// Reset all necessary variables and see in which area we are.
				tempNum = -1;
				area = getLookupStringDigitArea(digits.length - i);

				// Add transcription buttons depending on where we are in the char array.
				switch ((digits.length - i) % 3) {

				// This is a hundreds digit.
				case 0:

					logger.finest("This is a hundreds digit!");

					tempNum = Integer.parseInt(digits[i] + "");
					if (tempNum != 0) {
						transcribeArea = true;
						result.addAll(lookUpNumeralTensAndOnes(tempNum));
						result.add(new WordButton(hundred));
					}
					break;

				// This is a tens digit.
				case 2:

					logger.finest("This is a tens digit!");

					// Puzzle together the tens and ones position, then fall through.
					tempNum = Integer.parseInt(digits[i] + "" + digits[++i]);

				case 1:

					/*
					 * This case should only be true if the integer part's first digit was at a ones
					 * position. However, we will reach this code anyway, as case 2 falls through to
					 * here. Thus, we'll have to make sure that the value of tempNum does not get
					 * overwritten in case it should already have been set.
					 */

					// Not coming from a tens digit?
					if (tempNum < 0) {
						logger.finest("This is a ones digit!");
						tempNum = Integer.parseInt(digits[i] + "");
					}

					// Do not transcribe a 0 as in "100".
					if (tempNum != 0) {
						transcribeArea = true;
						result.addAll(lookUpNumeralTensAndOnes(tempNum));
					}

					// Append the area
					if (!area.isEmpty() && transcribeArea) {
						transcribeArea = false;
						result.add(new WordButton(sqlBridge.queryDatabase(area)));
					}
					break;
				}
			}
		}

		// We are done with the integer part. Now for the decimal one.
		if (!decimalPart.isEmpty()) {

			logger.finer("Appending decimal part ...");

			// Transcribe the notational dot first.
			result.add(new WordButton(sqlBridge.queryDatabase("dot")));

			// The rest is pretty straight forward. Transcribe each digit successively.
			for (int i = 0; i < decimalPart.length(); i++) {
				result.add(new WordButton(sqlBridge.queryDatabase(
						getLookupStringNumeralBelowTwenty(Integer.parseInt(decimalPart.charAt(i) + "")))));
			}
		}

		return result;
	}

	/**
	 * A convenience method to lookup the transcription parts of an <code>int</code> number on the
	 * interval of [0, 99].
	 *
	 * @param num
	 *            the number to transcribe.
	 * @return a list of {@link WordButton}s that constitute the transcription.
	 * @throws IllegalArgumentException
	 *             if the number was smaller than 0 or greater than 99.
	 * @throws SQLException
	 *             if the currently used instance {@link AbstractSQLiteBridge} is of type
	 *             {@link SQLiteBridgeDesktop} and something went wrong while querying the database.
	 * @throws Exception
	 *             most likely if the currently used instance {@link AbstractSQLiteBridge} throws
	 *             one while querying the database.
	 */
	private List<WordButton> lookUpNumeralTensAndOnes(int num)
			throws IllegalArgumentException, SQLException, Exception {

		logger.finer("Looking up numeral's tens and ones: " + num + "...");

		if (num < 0 || num >= 100) {
			throw new IllegalArgumentException("Number value must be 0 at least and 99 at max!");
		}

		// Prepare the required objects.
		List<WordButton> result = new ArrayList<WordButton>();
		AbstractSQLiteBridge sqlBridge = AbstractSQLiteBridge.getInstance();

		// Are we dealing with a number < 20? Easy then.
		if (num < 20) {
			result.add(new WordButton(sqlBridge.queryDatabase(getLookupStringNumeralBelowTwenty(num))));
			return result;
		}

		// Look up the tens and the ones seperately to save items in the database. Tens first.
		switch (num / 10) {
		case 2:
			result.add(new WordButton(sqlBridge.queryDatabase("twenty")));
			break;
		case 3:
			result.add(new WordButton(sqlBridge.queryDatabase("thirty")));
			break;
		case 4:
			result.add(new WordButton(sqlBridge.queryDatabase("forty")));
			break;
		case 5:
			result.add(new WordButton(sqlBridge.queryDatabase("fifty")));
			break;
		case 6:
			result.add(new WordButton(sqlBridge.queryDatabase("sixty")));
			break;
		case 7:
			result.add(new WordButton(sqlBridge.queryDatabase("seventy")));
			break;
		case 8:
			result.add(new WordButton(sqlBridge.queryDatabase("eighty")));
			break;
		case 9:
			result.add(new WordButton(sqlBridge.queryDatabase("ninety")));
			break;
		}

		// Simply add the ones digit.
		num %= 10;
		if (num != 0) {
			result.add(new WordButton(sqlBridge.queryDatabase(getLookupStringNumeralBelowTwenty(num % 10))));
		}

		return result;
	}

	/* Getters and Setters */
	/**
	 * A convenience method to fetch the <code>String</code> of a one-digit number for later
	 * database lookup.
	 *
	 * @param num
	 *            the number whose lookup <code>String</code> to retrieve.
	 * @return the number's corresponding lookup <code>String</code>.
	 */
	private String getLookupStringNumeralBelowTwenty(int num) {

		logger.finest("Getting lookup string for numeral below 20: " + num + "...");

		switch (num) {
		case 1:
			return "one";
		case 2:
			return "two";
		case 3:
			return "three";
		case 4:
			return "four";
		case 5:
			return "five";
		case 6:
			return "six";
		case 7:
			return "seven";
		case 8:
			return "eight";
		case 9:
			return "nine";
		case 10:
			return "ten";
		case 11:
			return "eleven";
		case 12:
			return "twelve";
		case 13:
			return "thirteen";
		case 14:
			return "fourteen";
		case 15:
			return "fifteen";
		case 16:
			return "sixteen";
		case 17:
			return "seventeen";
		case 18:
			return "eighteen";
		case 19:
			return "nineteen";
		default:
			return "zero";
		}
	}

	/**
	 * A convenience method to fetch the <code>String</code> of the current digit area for later
	 * database lookup. "Digit area" means the distance from the "dot" inside a decimal number and
	 * the corresponding name of that area. For example, a digit count of 4 to 6 would mean that the
	 * respective number is in its thousands (as in "<b>12</b>,345"), 7 to 9 would be in the
	 * millions, etc.
	 *
	 * @param digitCountFromZero
	 *            the number of digits from zero.
	 * @return the corresponding digit area.
	 */
	private String getLookupStringDigitArea(int digitCountFromZero) {

		logger.finest("Computing digit area ...");

		if (digitCountFromZero > 18) {
			return "quintillion";
		}

		if (digitCountFromZero > 15) {
			return "quadrillion";
		}

		if (digitCountFromZero > 12) {
			return "trillion";
		}

		if (digitCountFromZero > 9) {
			return "billion";
		}

		if (digitCountFromZero > 6) {
			return "million";
		}

		if (digitCountFromZero > 3) {
			return "thousand";
		}

		// Higher values would be out of the range of the long primitive.
		return "";
	}
}
