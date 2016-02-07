package de.upb.t2t.main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import config.ConfigProvider;

/**
 * A helper class for developers whose sole purpose is to reset the application's default properties
 * file should it have been tempred with. All settings are hard-coded inside the
 * {@link #writeDefaultProperties(String, String)}.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public class DefaultPropertiesSetter {

	/**
	 * This method will write the hard-coded application properties defaults into a file in the
	 * config folder. The name of said file can be the one given by the parameter.
	 *
	 * @param filename
	 *            the file name of the properties file.
	 * @param comments
	 *            the comments to set in the file's headline.
	 */
	private static void writeDefaultProperties(String filename, String comments) {
		Properties defaults = new Properties();

		// The GUI's initial size.
		defaults.setProperty("gui.main.width", "250.0");
		defaults.setProperty("gui.main.height", "250.0");
		defaults.setProperty("gui.addWord.width", "180.0");
		defaults.setProperty("gui.addWord.height", "180.0");
		defaults.setProperty("gui.addWord.x", "15.0");
		defaults.setProperty("gui.addWord.y", "15.0");

		// The default variety.
		defaults.setProperty("variety.preferred", "BrE");

		// The update server's address.
		defaults.setProperty("database.server.url", "");
		defaults.setProperty("database.offline_mode", "0");

		// Write the changes to the files.
		try (FileOutputStream out = new FileOutputStream(new ConfigProvider().getResourceAsFile(filename))) {
			defaults.store(out, comments);
		} catch (IOException | URISyntaxException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * The class's main method.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		writeDefaultProperties("default", "Do not modify!");
		writeDefaultProperties("application", "");
		System.out.println("Done.");
	}
}
