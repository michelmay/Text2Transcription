/**
 *
 */
package de.upb.t2t.gui.fxml;

import java.io.File;

import de.upb.t2t.control.AbstractResourceProvider;

/**
 * An implementation of the {@link AbstractResourceProvider} class, responsible for access to the
 * <code>.fxml</code> files.
 * 
 * @author Michel May (michel-may@gmx.de)
 */
public class FXMLProvider extends AbstractResourceProvider {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */

	/* Constructors */

	/* Object Methods */

	/* Getters & Setters */
	@Override
	public String getInfix() {
		return "gui" + File.separator + "fxml";
	}
}
