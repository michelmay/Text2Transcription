package de.upb.t2t.model;

/**
 * A <code>TranscriptionType</code> indicates whether or not the concept of weak an strong forms
 * applies to a {@link TranscriptionItem}.
 * 
 * @author Michel May (michel-may@gmx.de)
 *
 */
public enum TranscriptionType implements Abbreviable {

	NONE(0, "None", "None"), WEAK(1, "Weak Form", "Weak"), STRONG(2, "Strong Form", "Strong");

	/**
	 * A convenience getter to retrieve the <code>TranscriptionType</code> corresponding to the
	 * given ID.
	 * 
	 * @param id
	 *            the ID to look up.
	 * @return the corresponding <code>TranscriptionType</code>.
	 */
	public static TranscriptionType getTranscriptionType(int id) {
		for (TranscriptionType type : TranscriptionType.values()) {
			if (type.getID() == id) {
				return type;
			}
		}
		throw new IllegalArgumentException("No transcription type stored for id: \"" + id + "\"!");
	}

	/** The transctiption type's ID. */
	private int id;
	/** The transctiption description, e.g. "Weak Form" */
	private String description;
	/** The transctiption abbreviation, e.g. "Weak" */
	private String abbreviation;

	/**
	 * Constructor for the {@link TranscriptionType} enum.
	 * 
	 * @param id
	 *            the transctiption type's ID
	 * @param description
	 *            the transctiption type's description.
	 * @param abbreviation
	 *            the transctiption type's abbreviation.
	 */
	private TranscriptionType(int id, String description, String abbreviation) {
		this.id = id;
		this.description = description;
		this.abbreviation = abbreviation;
	}

	/**
	 * A Getter for the transctiption type's {@link #id} attribute.
	 * 
	 * @return the transctiption type's <code>id</code> attribute.
	 */
	public int getID() {
		return id;
	}

	/**
	 * A Getter for the transctiption type's {@link #description} attribute.
	 * 
	 * @return the transctiption type's <code>description</code> attribute.
	 */
	public String getDescription() {
		return description;
	}

	@Override
	public String getAbbreviation() {
		return abbreviation;
	}

	@Override
	public String toString() {
		return "Transcription Type: " + description + " (" + abbreviation + ", " + id + ")";
	}
}
