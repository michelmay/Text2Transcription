package de.upb.t2t.gui.css;

import java.io.File;

import de.upb.t2t.control.AbstractResourceProvider;

/**
 * An implementation of the {@link AbstractResourceProvider} class, responsible for access to the
 * <code>.css</code> files.
 * 
 * @author Michel May (michel-may@gmx.de)
 */
public class CSSProvider extends AbstractResourceProvider {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */

	/* Constructors */

	/* Object Methods */

	/* Getters and Setters */
	@Override
	protected String getInfix() {
		return "gui" + File.separator + "css";
	}
}
