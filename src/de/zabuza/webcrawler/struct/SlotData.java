package de.zabuza.webcrawler.struct;

import java.io.Serializable;

import de.zabuza.webcrawler.enums.SlotStatus;
import de.zabuza.webcrawler.enums.SlotType;

/**
 * Data container for a slot.
 * Stores different data like type or key.
 * 
 * @author Zabuza
 */
public final class SlotData implements Serializable {
	
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Player of the slot.
	 */
	private final String player;
	/**
	 * Type of the slot.
	 */
	private final SlotType slotType;
	/**
	 * Custom name of the slot.
	 */
	private final String customSlotName;
	/**
	 * Number of the slot.
	 */
	private final int number;
	/**
	 * Status of the slot.
	 */
	private final SlotStatus status;
	
	/**
	 * Creates a new data container for a slot.
	 * @param thatNumber Number of the slot
	 * @param thatSlotType Type of the slot
	 * @param thatCustomSlotName Custom name of the slot
	 * @param thatPlayer Player of the slot
	 * @param thatStatus Status of the slot
	 */
	public SlotData(int thatNumber, SlotType thatSlotType,
			String thatCustomSlotName, String thatPlayer,
			SlotStatus thatStatus) {
		this.number = thatNumber;
		this.slotType = thatSlotType;
		this.customSlotName = thatCustomSlotName;
		this.player = thatPlayer;
		this.status = thatStatus;
	}
	/**
	 * @return the player
	 */
	public String getPlayer() {
		return player;
	}
	/**
	 * @return the slotType
	 */
	public SlotType getSlotType() {
		return slotType;
	}
	/**
	 * @return the number
	 */
	public int getNumber() {
		return number;
	}
	/**
	 * @return the status
	 */
	public SlotStatus getStatus() {
		return status;
	}
	/**
	 * @return the customSlotName
	 */
	public String getCustomSlotName() {
		return customSlotName;
	}
	@Override
	public String toString() {
		return number + " - " + slotType + " - " + player + " - " + status;
	}
}