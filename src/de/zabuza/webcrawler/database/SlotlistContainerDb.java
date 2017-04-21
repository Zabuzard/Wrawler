package de.zabuza.webcrawler.database;

import de.zabuza.webcrawler.struct.Slotlist;

/**
 * Container class for database slotlists.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 *
 */
public final class SlotlistContainerDb {
	/**
	 * Enclosing character for values in the text representation.
	 */
	private static final String ENCLOSER = "\"";
	/**
	 * Separator character for values in the text representation.
	 */
	private static final String SEPARATOR = ",";
	/**
	 * Comment of the slotlist.
	 */
	private final String comment;
	/**
	 * Owner of the slotlist.
	 */
	private final int owner;
	/**
	 * Slotlist object itself.
	 */
	private final Slotlist slotlist;
	/**
	 * Name of the slotlist.
	 */
	private final String slotlistName;
	/**
	 * Unique ID of the slotlist.
	 */
	private final int uniqueID;

	/**
	 * Creates a new slotlist container.
	 * 
	 * @param thatUniqueID
	 *            Unique ID of this slotlist
	 * @param thatSlotlistName
	 *            Name of the slotlist
	 * @param thatOwner
	 *            Owner of the slotlist
	 * @param thatComment
	 *            Comment of the slotlist
	 * @param thatSlotlist
	 *            Slotlist object itself
	 */
	public SlotlistContainerDb(final int thatUniqueID, final String thatSlotlistName, final int thatOwner, final String thatComment,
			final Slotlist thatSlotlist) {
		this.uniqueID = thatUniqueID;
		this.slotlistName = thatSlotlistName;
		this.owner = thatOwner;
		this.comment = thatComment;
		this.slotlist = thatSlotlist;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return this.comment;
	}

	/**
	 * @return the owner
	 */
	public int getOwner() {
		return this.owner;
	}

	/**
	 * @return the slotlist
	 */
	public Slotlist getSlotlist() {
		return this.slotlist;
	}

	/**
	 * @return the slotlistName
	 */
	public String getSlotlistName() {
		return this.slotlistName;
	}

	/**
	 * @return the uniqueID
	 */
	public int getUniqueID() {
		return this.uniqueID;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(ENCLOSER + this.uniqueID + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.slotlistName + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.owner + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.comment + ENCLOSER);
		return builder.toString();
	}
}
