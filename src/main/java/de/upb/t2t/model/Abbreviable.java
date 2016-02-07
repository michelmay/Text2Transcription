/**
 *
 */
package de.upb.t2t.model;

/**
 * An interface that assures its implementing classes to have an abbreviated description of
 * themselves. Examples for this would be the {@link WordClass} "British Englisch" with its
 * abbreviation "BrE".
 *
 * @author Michel May (michel-may@gmx.de)
 *
 */
public interface Abbreviable {

	/**
	 * @return an abbreviated <code>String</code> representation of this object.
	 */
	public String getAbbreviation();

}
