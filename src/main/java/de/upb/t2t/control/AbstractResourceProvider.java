/**
 *
 */
package de.upb.t2t.control;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * <p>
 * A convenience class whose implementations only need to override the {@link #getInfix()} function.
 * Any sub-class file should be added into a folder containing resources the application may wish to
 * use. This approach may seem somewhat clumsy at first sight. However, since the project obeys to
 * the <a href="http://gluonhq.com/the-new-gluon-plugin-for-eclipse/">Gluon conventions</a>,
 * resource files need to be located for example in the <code>src/main/resources</code> folder.
 * Inconveniently, said folder structure changes once the application has been packed into a
 * <code>.jar</code> file. This class will automatically take care of both use cases.
 * </p>
 * <p>
 * <b>Important:</b> Please note that when running the application from inside eclipse (or
 * potentially any other IDE), the folder containing the <code>.class</code> files is where
 * instances of this class will point to. This should normally be the <code>bin</code> directory and
 * not its <code>src</code> counterpart. Therefore, changes made by the application will not be
 * adapted in the <code>src</code> files.
 * </p>
 *
 * @author Michel May (michel-may@gmx.de)
 *
 * @see #getResourceAsStream(String)
 * @see #getResourceAsURL(String)
 * @see #getResourceAsFile(String)
 */
public abstract class AbstractResourceProvider {

	/* Static Variables */

	/* Static Methods */
	/**
	 * A convenience function checking whether or not the application is being launched from within
	 * a <code>.jar</code> file.
	 *
	 * @param applicationRoot
	 *            the application's root file/directory.
	 * @return <code>true</code> if the paramter is a <code>.jar</code> file; <code>false</code>
	 *         otherwise.
	 */
	private static boolean executedFromJar(File applicationRoot) {
		return applicationRoot.getName().endsWith(".jar");
	}

	/* Object Variables */

	/* Constructors */

	/* Object Methods */

	/* Getters and Setters */
	/**
	 * Returns the path relative to the executed <code>.jar</code> to the current folder it should
	 * point to. This path is irrelevant if the application is running from inside an IDE.
	 *
	 * @return the path to the resource folde relative to the application's <code>.jar</code> file.
	 */
	protected abstract String getInfix();

	/**
	 * When an a Java programme is compiled into a <code>.jar</code> file, every resource it may
	 * require that has also been packed into said file will become read-only. Therefore, files that
	 * need to be modified at runtime should placed outside the <code>.jar</code> and accessed via
	 * this function.
	 *
	 * @param filename
	 *            the name of the file to obtain.
	 * @return the given file as an <code>InputStream</code>.
	 * @throws URISyntaxException
	 *             if {@link #getApplicationRoot()} throws one.
	 * @throws FileNotFoundException
	 *             if {@link FileInputStream#FileInputStream(String)} throws one.
	 */
	public InputStream getResourceAsStream(String filename) throws URISyntaxException, FileNotFoundException {

		// Are we running from a jar file?
		File applicationRoot = getApplicationRoot();
		if (executedFromJar(applicationRoot)) {
			return new FileInputStream(
					applicationRoot.getParentFile() + File.separator + getInfix() + File.separator + filename);
		}

		return getClass().getResourceAsStream(filename);
	}

	/**
	 * When an a Java programme is compiled into a <code>.jar</code> file, every resource it may
	 * require that has also been packed into said file will become read-only. Therefore, files that
	 * need to be modified at runtime should placed outside the <code>.jar</code> and accessed via
	 * this function.
	 *
	 * @param filename
	 *            the name of the file to obtain.
	 * @return the given file as a <code>URL</code>.
	 * @throws URISyntaxException
	 *             if {@link #getApplicationRoot()} or {@link URL#toURI()} throw one.
	 * @throws MalformedURLException
	 *             if {@link URI#toURL()} throws one.
	 */
	public URL getResourceAsURL(String filename) throws URISyntaxException, MalformedURLException {
		File applicationRoot = getApplicationRoot();
		if (executedFromJar(applicationRoot)) {
			return new File(applicationRoot.getParentFile() + File.separator + getInfix() + File.separator + filename)
					.toURI().toURL();
		}
		return getClass().getResource(filename);
	}

	/**
	 * When an a Java programme is compiled into a <code>.jar</code> file, every resource it may
	 * require that has also been packed into said file will become read-only. Therefore, files that
	 * need to be modified at runtime should placed outside the <code>.jar</code> and accessed via
	 * this function.
	 *
	 * @param filename
	 *            the name of the file to obtain.
	 * @return the given file as a <code>File</code>.
	 * @throws URISyntaxException
	 *             if {@link #getApplicationRoot()} or {@link URL#toURI()} throw one.
	 */
	public File getResourceAsFile(String filename) throws URISyntaxException {
		File applicationRoot = getApplicationRoot();
		if (executedFromJar(applicationRoot)) {
			return new File(applicationRoot.getParent() + File.separator + getInfix() + File.separator + filename);
		}
		return new File(getClass().getResource(filename).toURI());
	}

	/**
	 * A convenience getter to retrieve the application's root file/directory.
	 *
	 * @return the application's root file/directory.
	 * @throws URISyntaxException
	 */
	private File getApplicationRoot() throws URISyntaxException {
		return new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
	}
}
