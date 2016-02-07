/**
 *
 */
package de.upb.t2t.control.gui;

import de.upb.t2t.model.FeedbackMode;

/**
 * This interface assures its implementing classes to provide basic functionality in terms of
 * offering the user feedback about the status of an operation.
 *
 * @author Michel May(michel-may@gmx.de)
 *
 */
public interface UserFeedbackProvider {

	/**
	 * The most basic way of giving the user feedback by displaying the given <code>String</code>.
	 * To facilitate programming, information about an operation's success, failure, error state,
	 * etc. may directly be read from the first paramter. This also allows for visual
	 * representations, such as warning signs or similar iconic codings.
	 *
	 * @param mode
	 *            indicates the nature of the feedback.
	 * @param feedback
	 *            a message that the user may receive.
	 */
	public void generalFeedback(FeedbackMode mode, String feedback);

	/**
	 * Presents the user with feedback about a pending process. The first parameter indicates the
	 * estimated percentage of completion.
	 *
	 * @param progress
	 *            the estimated percentage of completion.
	 * @param feedback
	 *            a message that the user may receive.
	 */
	public void progressFeedback(double progress, String feedback);

}
