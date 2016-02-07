/**
 *
 */
package de.upb.t2t.control.gui.transcriber;

import de.upb.t2t.model.Abbreviable;
import javafx.scene.control.ListCell;

/**
 * An implementation of the {@link ListCell} class that offers special functionality should it be
 * given items of type {@link Abbreviable}.
 *
 * @author Michel May (michel-may@gmx.de)
 *
 * @param <T>
 *            the class's generic type.
 */
public class AbbreviableListCell<T> extends ListCell<T> {

	/* Static Variables */

	/* Static Methods */

	/* Object Variables */

	/* Constructors */

	/* Object Methods */
	/**
	 * This will simply call the super-type {@link #updateItem(Object, boolean)} method.
	 * Additionally, should the given item be of type {@link Abbreviable},
	 * {@link ListCell#setText(String)} will additionally be called with
	 * {@link Abbreviable#getAbbreviation()} as parameter.
	 */
	@Override
	protected void updateItem(T item, boolean empty) {
		super.updateItem(item, empty);
		if (item == null) {
			setText("");
			return;
		}
		setText(item instanceof Abbreviable ? ((Abbreviable) item).getAbbreviation() : item.toString());
	}

	/* Getters and Setters */
}
