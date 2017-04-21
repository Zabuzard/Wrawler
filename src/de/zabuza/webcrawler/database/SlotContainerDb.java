package de.zabuza.webcrawler.database;

import java.util.Map.Entry;

import de.zabuza.webcrawler.enums.SlotStatus;
import de.zabuza.webcrawler.enums.SlotType;
import de.zabuza.webcrawler.struct.SlotData;
import de.zabuza.webcrawler.util.CrawlerUtil;

/**
 * Container class for database slots.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 *
 */
public class SlotContainerDb {
	/**
	 * Enclosing character for values in the text representation.
	 */
	private static final String ENCLOSER = "\"";
	/**
	 * Number for a reserve slot.
	 */
	private static final int RESERVE_NUMBER = -2;
	/**
	 * Separator character for values in the text representation.
	 */
	private static final String SEPARATOR = ",";

	/**
	 * Gets the database id of an slot status by the status.
	 * 
	 * @param status
	 *            Status to get database id of
	 * @return Database id of the slot status
	 */
	private static int getIdBySlotStatus(final SlotStatus status) {
		switch (status) {
		case APPEARED:
			return 1;
		case SIGNED_OUT:
			return 2;
		case SIGNED_OUT_LATE:
			return 3;
		case PREPARED_LATE:
			return 4;
		case ABSENT:
			return 5;
		case UNKNOWN:
			return 6;
		default:
			System.err.println("Unknown slot status while creating database slot: " + status);
			return 6;
		}
	}

	/**
	 * Gets the slot status of a database id.
	 * 
	 * @param id
	 *            Database id to get slot status of
	 * @return Slot status to get
	 */
	private static SlotStatus getSlotStatusById(final int id) {
		switch (id) {
		case 1:
			return SlotStatus.APPEARED;
		case 2:
			return SlotStatus.SIGNED_OUT;
		case 3:
			return SlotStatus.SIGNED_OUT_LATE;
		case 4:
			return SlotStatus.PREPARED_LATE;
		case 5:
			return SlotStatus.ABSENT;
		case 6:
			return SlotStatus.UNKNOWN;
		default:
			System.err.println("Unknown slot status id while creating database slot: " + id);
			return SlotStatus.UNKNOWN;
		}
	}

	/**
	 * UserID of this slot.
	 */
	private final int assignedUserID;
	/**
	 * Attendance of slots user.
	 */
	private final int attendance;
	/**
	 * Custom name of the slot.
	 */
	private final String customName;
	/**
	 * Custom name of the user.
	 */
	private final String customUser;
	/**
	 * ID of slot that a reserve player has been assigned to.
	 */
	private final int reserveID;
	/**
	 * Id of the slot.
	 */
	private final int slotID;
	/**
	 * Id of the slotlist the slot belongs to.
	 */
	private final int slotlistID;

	/**
	 * Number of the slot.
	 */
	private final int slotNumber;
	/**
	 * Unique ID of the slot.
	 */
	private final int uniqueID;

	/**
	 * Creates a new slot container object for reserve player.
	 * 
	 * @param reservePlayer
	 *            Name of the player and his slot status
	 * @param thatUniqueID
	 *            Unique id of the slot
	 * @param thatSlotlistID
	 *            id of the slotlist the slot belongs to
	 */
	public SlotContainerDb(final Entry<String, SlotStatus> reservePlayer, final int thatUniqueID,
			final int thatSlotlistID) {
		this.uniqueID = thatUniqueID;
		this.slotlistID = thatSlotlistID;
		this.slotNumber = RESERVE_NUMBER;
		this.slotID = SlotTypeTableDb.getInstance().getId(SlotType.RESERVE).intValue();
		this.customName = "";
		final int userIdExternal = UserTableDb.getInstance().getId(reservePlayer.getKey()).intValue();
		if (userIdExternal > 0) {
			this.assignedUserID = userIdExternal;
			this.customUser = "";
		} else {
			this.assignedUserID = 0;
			this.customUser = reservePlayer.getKey();
		}
		this.attendance = getIdBySlotStatus(reservePlayer.getValue());
		this.reserveID = 0;
	}

	/**
	 * Creates a new slot container object.
	 * 
	 * @param slotData
	 *            Data of the slot
	 * @param thatUniqueID
	 *            Unique id of the slot
	 * @param thatSlotlistID
	 *            id of the slotlist the slot belongs to
	 */
	public SlotContainerDb(final SlotData slotData, final int thatUniqueID, final int thatSlotlistID) {
		this.uniqueID = thatUniqueID;
		this.slotlistID = thatSlotlistID;
		this.slotNumber = slotData.getNumber();
		this.slotID = SlotTypeTableDb.getInstance().getId(slotData.getSlotType()).intValue();
		this.customName = slotData.getCustomSlotName();
		final int userIdExternal = UserTableDb.getInstance().getId(slotData.getPlayer()).intValue();
		if (userIdExternal > 0) {
			this.assignedUserID = userIdExternal;
			this.customUser = "";
		} else {
			this.assignedUserID = 0;
			this.customUser = slotData.getPlayer();
		}
		this.attendance = getIdBySlotStatus(slotData.getStatus());
		this.reserveID = 0;
	}

	/**
	 * Creates a new slot container using the database format.
	 * 
	 * @param databaseFormat
	 *            Slot container in the database format
	 */
	public SlotContainerDb(final String databaseFormat) {
		final String[] values = CrawlerUtil.parseDatabaseFormatLine(databaseFormat);
		this.uniqueID = Integer.parseInt(values[0]);
		this.slotlistID = Integer.parseInt(values[1]);
		this.slotNumber = Integer.parseInt(values[2]);
		this.slotID = Integer.parseInt(values[3]);
		this.customName = values[4];
		this.assignedUserID = Integer.parseInt(values[5]);
		this.customUser = values[6];
		this.attendance = Integer.parseInt(values[7]);
		this.reserveID = Integer.parseInt(values[8]);
	}

	/**
	 * @return the assignedUserID
	 */
	public int getAssignedUserID() {
		return this.assignedUserID;
	}

	/**
	 * @return the attendance
	 */
	public int getAttendance() {
		return this.attendance;
	}

	/**
	 * @return the customName
	 */
	public String getCustomName() {
		return this.customName;
	}

	/**
	 * @return the customUser
	 */
	public String getCustomUser() {
		return this.customUser;
	}

	/**
	 * @return the reserveID
	 */
	public int getReserveID() {
		return this.reserveID;
	}

	/**
	 * @return the slotID
	 */
	public int getSlotID() {
		return this.slotID;
	}

	/**
	 * @return the slotlistID
	 */
	public int getSlotlistID() {
		return this.slotlistID;
	}

	/**
	 * @return the slotNumber
	 */
	public int getSlotNumber() {
		return this.slotNumber;
	}

	/**
	 * @return the uniqueID
	 */
	public int getUniqueID() {
		return this.uniqueID;
	}

	/**
	 * Returns a SlotData representation of this object.
	 * 
	 * @return A SlotData representation of this object.
	 */
	public SlotData toSlotData() {
		String thatUserName = "";
		if (this.assignedUserID == 0) {
			thatUserName = this.customUser;
		} else {
			thatUserName = UserTableDb.getInstance().getUser(this.assignedUserID);
		}
		final SlotData slotData = new SlotData(this.slotNumber, SlotTypeTableDb.getInstance().getSlotType(this.slotID),
				this.customName, thatUserName, getSlotStatusById(this.attendance));
		return slotData;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(ENCLOSER + this.uniqueID + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.slotlistID + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.slotNumber + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.slotID + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.customName + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.assignedUserID + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.customUser + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.attendance + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.reserveID + ENCLOSER);
		return builder.toString();
	}
}
