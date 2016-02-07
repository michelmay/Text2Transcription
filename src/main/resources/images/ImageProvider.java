package images;

import de.upb.t2t.control.AbstractResourceProvider;

/**
 * An implementation of the {@link AbstractResourceProvider} class, responsible for access to the
 * image files.
 * 
 * @author Michel May (michel-may@gmx.de)
 */
public class ImageProvider extends AbstractResourceProvider {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */

	/* Constructors */

	/* Object Methods */

	/* Getters & Setters */
	@Override
	protected String getInfix() {
		return "images";
	}
}
