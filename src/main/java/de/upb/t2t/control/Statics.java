package de.upb.t2t.control;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides static functionalities, such as, for example, directory paths or commonly
 * used methods.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class Statics {

	/* Static Variables */
	/**
	 * A general regex pattern that matches any phonetic symbol. <b>Please check its definition
	 * before using it!</b>
	 */
	public static final String REGEX_BROAD_PHONETIC_SYMBOLS = "(ɑː)ʌæ(ɜː)eə(iː)iɪ(ɔː)ɒ(uː)uʊ" // Vowels
			+ "(eɪ)(aɪ)(ɔɪ)(əʊ)(aʊ)(ɪə)(eə)(ʊə)" // Diphthongs
			+ "bdfghjklmnŋprsʃt(tʃ)θðvwzʒ(dʒ)" // Consonants
			+ "​ˌ​​ˈ​"; // Stress Indicators

	/* Static Methods */
	/**
	 * Parses a boolean from the given <code>String</code>. That <code>String</code> may either be
	 * of the form <code>true</code>/<code>false</code> or <code>1</code>/ <code>0</code>. Like
	 * {@link Boolean#parseBoolean(String)}, it will will also return false if the
	 * <code>String</code> was badly formatted and not throw an exception.
	 *
	 * @param string
	 *            the <code>String</code> to parse
	 * @return the respective boolean value
	 */
	public static boolean parseBoolean(String string) {
		string = string.trim();
		return string.equals("1") || Boolean.parseBoolean(string);
	}

	/**
	 * A convenience function that returns a list of all match results to be found in the given
	 * input <code>String</code>.
	 *
	 * @param input
	 *            the <code>String</code> to search for the given regex pattern.
	 * @param regex
	 *            the regex pattern to search the given <code>String</code> with.
	 * @return a list of all match results to be found.
	 */
	public static List<MatchResult> getAllRegexMatches(String input, String regex) {
		List<MatchResult> result = new ArrayList<MatchResult>();
		Matcher matcher = Pattern.compile(regex).matcher(input);
		while (matcher.find()) {
			result.add(matcher.toMatchResult());
		}
		return result;
	}

	/* Object Variables */

	/* Constructors */

	/* Object Methods */

	/* Getters and Setters */
}
