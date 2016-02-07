/**
 *
 */
package config;

import de.upb.t2t.control.AbstractResourceProvider;

/**
 * An implementation of the {@link AbstractResourceProvider} class, responsible for access to the
 * config files.
 * 
 * @author Michel May (michel-may@gmx.de)
 */
public class ConfigProvider extends AbstractResourceProvider {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */

	/* Constructors */

	/* Object Methods */

	/* Getters & Setters */
	@Override
	protected String getInfix() {
		return "config";
	}

}
