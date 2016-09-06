package de.zabuza.webcrawler.database;

import java.util.Map.Entry;

import de.zabuza.webcrawler.enums.SlotStatus;
import de.zabuza.webcrawler.enums.SlotType;
import de.zabuza.webcrawler.struct.SlotData;
import de.zabuza.webcrawler.util.CrawlerUtil;

/**
 * Container class for database slots.
 * @author Zabuza
 *
 */
public class SlotContainerDb {
	/**
	 * Separator character for values in the text representation.
	 */
	private static final String SEPARATOR = ",";
	/**
	 * Enclosing character for values in the text representation.
	 */
	private static final String ENCLOSER = "\"";
	/**
	 * Number for a reserve slot.
	 */
	private static final int RESERVE_NUMBER = -2;
	/**
	 * Unique ID of the slot.
	 */
	private final int uniqueID;
	/**
	 * Id of the slotlist the slot belongs to.
	 */
	private final int slotlistID;
	/**
	 * Number of the slot.
	 */
	private final int slotNumber;
	/**
	 * Id of the slot.
	 */
	private final int slotID;
	/**
	 * Custom name of the slot.
	 */
	private final String customName;
	/**
	 * UserID of this slot.
	 */
	private final int assignedUserID;
	/**
	 * Custom name of the user.
	 */
	private final String customUser;
	/**
	 * Attendance of slots user.
	 */
	private final int attendance;
	/**
	 * ID of slot that a reserve player has been assigned to.
	 */
	private final int reserveID;
	
	/**
	 * Creates a new slot container object.
	 * @param slotData Data of the slot
	 * @param thatUniqueID Unique id of the slot
	 * @param thatSlotlistID id of the slotlist the slot belongs to
	 */
	public SlotContainerDb(SlotData slotData, int thatUniqueID, int thatSlotlistID) {
		uniqueID = thatUniqueID;
		slotlistID = thatSlotlistID;
		slotNumber = slotData.getNumber();
		slotID = SlotTypeTableDb.getInstance().getId(slotData.getSlotType());
		customName = slotData.getCustomSlotName();
		int userIdExternal = UserTableDb.getInstance().getId(slotData.getPlayer());
		if (userIdExternal > 0) {
			assignedUserID = userIdExternal;
			customUser = "";
		} else {
			assignedUserID = 0;
			customUser = slotData.getPlayer();
		}
		attendance = getIdBySlotStatus(slotData.getStatus());
		reserveID = 0;
	}
	/**
	 * Creates a new slot container object for reserve player.
	 * @param reservePlayer Name of the player and his slot status
	 * @param thatUniqueID Unique id of the slot
	 * @param thatSlotlistID id of the slotlist the slot belongs to
	 */
	public SlotContainerDb(Entry<String, SlotStatus> reservePlayer, int thatUniqueID, int thatSlotlistID) {
		uniqueID = thatUniqueID;
		slotlistID = thatSlotlistID;
		slotNumber = RESERVE_NUMBER;
		slotID = SlotTypeTableDb.getInstance().getId(SlotType.RESERVE);
		customName = "";
		int userIdExternal = UserTableDb.getInstance().getId(reservePlayer.getKey());
		if (userIdExternal > 0) {
			assignedUserID = userIdExternal;
			customUser = "";
		} else {
			assignedUserID = 0;
			customUser = reservePlayer.getKey();
		}
		attendance = getIdBySlotStatus(reservePlayer.getValue());
		reserveID = 0;
	}
	
	/**
	 * Creates a new slot container using the database format.
	 * @param databaseFormat Slot container in the database format
	 */
	public SlotContainerDb(String databaseFormat) {
		String[] values = CrawlerUtil.parseDatabaseFormatLine(databaseFormat);
		uniqueID = Integer.parseInt(values[0]);
		slotlistID = Integer.parseInt(values[1]);
		slotNumber = Integer.parseInt(values[2]);
		slotID = Integer.parseInt(values[3]);
		customName = values[4];
		assignedUserID = Integer.parseInt(values[5]);
		customUser = values[6];
		attendance = Integer.parseInt(values[7]);
		reserveID = Integer.parseInt(values[8]);;
	}
	
	/**
	 * Returns a SlotData representation of this object.
	 * @return A SlotData representation of this object.
	 */
	public SlotData toSlotData() {
		String thatUserName = "";
		if (assignedUserID == 0) {
			thatUserName = customUser;
		} else {
			thatUserName = UserTableDb.getInstance().getUser(assignedUserID);
		}
		SlotData slotData = new SlotData(slotNumber, SlotTypeTableDb.getInstance().getSlotType(slotID),
				customName, thatUserName, getSlotStatusById(attendance));
		return slotData;
	}

	/**
	 * @return the uniqueID
	 */
	public int getUniqueID() {
		return uniqueID;
	}

	/**
	 * @return the slotlistID
	 */
	public int getSlotlistID() {
		return slotlistID;
	}

	/**
	 * @return the slotNumber
	 */
	public int getSlotNumber() {
		return slotNumber;
	}

	/**
	 * @return the slotID
	 */
	public int getSlotID() {
		return slotID;
	}

	/**
	 * @return the customName
	 */
	public String getCustomName() {
		return customName;
	}

	/**
	 * @return the assignedUserID
	 */
	public int getAssignedUserID() {
		return assignedUserID;
	}

	/**
	 * @return the customUser
	 */
	public String getCustomUser() {
		return customUser;
	}

	/**
	 * @return the attendance
	 */
	public int getAttendance() {
		return attendance;
	}
	
	/**
	 * @return the reserveID
	 */
	public int getReserveID() {
		return reserveID;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(ENCLOSER + uniqueID + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + slotlistID + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + slotNumber + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + slotID + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + customName + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + assignedUserID + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + customUser + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + attendance + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + reserveID + ENCLOSER);
		return builder.toString();
	}
	/**
	 * Gets the database id of an slot status by the status.
	 * @param status Status to get database id of
	 * @return Database id of the slot status
	 */
	private static int getIdBySlotStatus(SlotStatus status) {
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
	 * @param id Database id to get slot status of
	 * @return Slot status to get
	 */
	private static SlotStatus getSlotStatusById(int id) {
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
}
