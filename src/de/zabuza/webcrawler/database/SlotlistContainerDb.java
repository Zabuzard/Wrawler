package de.zabuza.webcrawler.database;

import de.zabuza.webcrawler.struct.Slotlist;

/**
 * Container class for database slotlists.
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 *
 */
public final class SlotlistContainerDb {
	/**
	 * Separator character for values in the text representation.
	 */
	private static final String SEPARATOR = ",";
	/**
	 * Enclosing character for values in the text representation.
	 */
	private static final String ENCLOSER = "\"";
	/**
	 * Unique ID of the slotlist.
	 */
	private final int uniqueID;
	/**
	 * Name of the slotlist.
	 */
	private final String slotlistName;
	/**
	 * Owner of the slotlist.
	 */
	private final int owner;
	/**
	 * Comment of the slotlist.
	 */
	private final String comment;
	/**
	 * Slotlist object itself.
	 */
	private final Slotlist slotlist;
	
	/**
	 * Creates a new slotlist container.
	 * @param thatUniqueID Unique ID of this slotlist
	 * @param thatSlotlistName Name of the slotlist
	 * @param thatOwner Owner of the slotlist
	 * @param thatComment Comment of the slotlist
	 * @param thatSlotlist Slotlist object itself
	 */
	public SlotlistContainerDb(int thatUniqueID, String thatSlotlistName,
			int thatOwner, String thatComment, Slotlist thatSlotlist) {
		this.uniqueID = thatUniqueID;
		this.slotlistName = thatSlotlistName;
		this.owner = thatOwner;
		this.comment = thatComment;
		this.slotlist = thatSlotlist;
	}

	/**
	 * @return the uniqueID
	 */
	public int getUniqueID() {
		return uniqueID;
	}

	/**
	 * @return the slotlistName
	 */
	public String getSlotlistName() {
		return slotlistName;
	}

	/**
	 * @return the owner
	 */
	public int getOwner() {
		return owner;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @return the slotlist
	 */
	public Slotlist getSlotlist() {
		return slotlist;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(ENCLOSER + uniqueID + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + slotlistName + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + owner + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + comment + ENCLOSER);
		return builder.toString();
	}
}
