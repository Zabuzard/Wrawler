package de.zabuza.webcrawler.struct;

import java.io.Serializable;

import de.zabuza.webcrawler.enums.SlotStatus;
import de.zabuza.webcrawler.enums.SlotType;

/**
 * Data container for a slot. Stores different data like type or key.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 */
public final class SlotData implements Serializable {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Custom name of the slot.
	 */
	private final String customSlotName;
	/**
	 * Number of the slot.
	 */
	private final int number;
	/**
	 * Player of the slot.
	 */
	private final String player;
	/**
	 * Type of the slot.
	 */
	private final SlotType slotType;
	/**
	 * Status of the slot.
	 */
	private final SlotStatus status;

	/**
	 * Creates a new data container for a slot.
	 * 
	 * @param thatNumber
	 *            Number of the slot
	 * @param thatSlotType
	 *            Type of the slot
	 * @param thatCustomSlotName
	 *            Custom name of the slot
	 * @param thatPlayer
	 *            Player of the slot
	 * @param thatStatus
	 *            Status of the slot
	 */
	public SlotData(final int thatNumber, final SlotType thatSlotType, final String thatCustomSlotName, final String thatPlayer,
			final SlotStatus thatStatus) {
		this.number = thatNumber;
		this.slotType = thatSlotType;
		this.customSlotName = thatCustomSlotName;
		this.player = thatPlayer;
		this.status = thatStatus;
	}

	/**
	 * @return the customSlotName
	 */
	public String getCustomSlotName() {
		return this.customSlotName;
	}

	/**
	 * @return the number
	 */
	public int getNumber() {
		return this.number;
	}

	/**
	 * @return the player
	 */
	public String getPlayer() {
		return this.player;
	}

	/**
	 * @return the slotType
	 */
	public SlotType getSlotType() {
		return this.slotType;
	}

	/**
	 * @return the status
	 */
	public SlotStatus getStatus() {
		return this.status;
	}

	@Override
	public String toString() {
		return this.number + " - " + this.slotType + " - " + this.player + " - " + this.status;
	}
}