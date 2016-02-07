/**
 *
 */
package de.upb.t2t.control;

/**
 * A simple interface that offers listening functionality in the event that a generic item has been
 * selected.
 *
 * @author Michel May (michel-may@gmx.de)
 * @see #onItemSelected(Object)
 */
public interface SelectionListener<T> {

	/**
	 * Called whenever an item has been selected.
	 *
	 * @param item the item that has been selected.
	 */
	public void onItemSelected(T item);
}
