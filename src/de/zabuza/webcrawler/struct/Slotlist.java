package de.zabuza.webcrawler.struct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.zabuza.webcrawler.enums.SlotStatus;
import de.zabuza.webcrawler.enums.SlotType;

/**
 * Class for an events slot-list. Contains information like player and their
 * slots.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 */
public final class Slotlist implements Serializable {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * List that contains all reserve player.
	 */
	private final Map<String, SlotStatus> reserve;
	/**
	 * List that contains all slotted player.
	 */
	private final List<SlotData> slots;

	/**
	 * Create a new empty slot-list with a starting capacity of size.
	 * 
	 * @param size
	 *            Starting capacity of slot-list
	 */
	public Slotlist(int size) {
		this.slots = new ArrayList<>(size);
		this.reserve = new HashMap<>();
	}

	/**
	 * Adds a new reserve player.
	 * 
	 * @param player
	 *            Player for the reserve
	 * @param status
	 *            Status of the slot
	 */
	public void addReserve(String player, SlotStatus status) {
		this.reserve.put(player, status);
	}

	/**
	 * Adds a new slot with a given player.
	 * 
	 * @param number
	 *            Number of the slot
	 * @param slot
	 *            Type of the slot
	 * @param customSlotName
	 *            Custom name of the slot
	 * @param player
	 *            Player for the slot
	 * @param status
	 *            Status of the slot
	 */
	public void addSlot(int number, SlotType slot, String customSlotName, String player, SlotStatus status) {
		this.slots.add(new SlotData(number, slot, customSlotName, player, status));
	}

	/**
	 * Adds a slot by an existing slotData object.
	 * 
	 * @param slotData
	 *            SlotData object to add
	 */
	public void addSlot(SlotData slotData) {
		if (slotData.getSlotType().equals(SlotType.RESERVE)) {
			addReserve(slotData.getPlayer(), slotData.getStatus());
		} else {
			this.slots.add(slotData);
		}
	}

	/**
	 * Gets a list of all reserve player with data.
	 * 
	 * @return List of all reserve player with data
	 */
	public Map<String, SlotStatus> getAllReserve() {
		return this.reserve;
	}

	/**
	 * Gets a list of all slotted player with data.
	 * 
	 * @return List of all slotted player with data
	 */
	public List<SlotData> getAllSlots() {
		return this.slots;
	}

	/**
	 * Gets the slot status of a reserve player.
	 * 
	 * @param player
	 *            Name of the reserve player
	 * @return Slot status of the reserve player
	 */
	public SlotStatus getReserveStatus(String player) {
		return this.reserve.get(player);
	}

	/**
	 * Gets the data container of a slot.
	 * 
	 * @param index
	 *            Index of the slot in the slot list
	 * @return Data container of the slot
	 */
	public SlotData getSlotData(int index) {
		return this.slots.get(index);
	}

	/**
	 * Gets the amount of all reserve player.
	 * 
	 * @return Amount of all reserve player
	 */
	public int reserveSize() {
		return this.reserve.size();
	}

	/**
	 * Gets the amount of all slotted player.
	 * 
	 * @return Amount of all slotted player
	 */
	public int slotSize() {
		return this.slots.size();
	}
}