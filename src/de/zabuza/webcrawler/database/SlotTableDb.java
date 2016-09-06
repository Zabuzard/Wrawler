package de.zabuza.webcrawler.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.zabuza.webcrawler.enums.SlotStatus;
import de.zabuza.webcrawler.struct.SlotData;
import de.zabuza.webcrawler.struct.Slotlist;

/**
 * Class that links slot container with their slotlists.
 * @author Zabuza
 *
 */
public final class SlotTableDb {
	/**
	 * Default capacity of maps.
	 */
	private static final int DEFAULT_CAPACITY = 10_000;
	/**
	 * Separator character for entries in the text representation.
	 */
	public static final String ENTRY_SEPARATOR = ";";
	/**
	 * Dictionary for id to slot access.
	 */
	private final Map<Integer, SlotContainerDb> idToSlot =
			new HashMap<Integer, SlotContainerDb>(DEFAULT_CAPACITY);
	/**
	 * Id for the first slot which increments each time a slot gets added.
	 */
	private int slotId = 1;
	
	/**
	 * Creates a new slot table.
	 */
	public SlotTableDb() {
		
	}
	
	/**
	 * Adds the given slotlists slots to the table and assigns unique ids for each slot.
	 * @param slotlist Slotlist to add slots off
	 * @param slotlistId Id of the slotlist the slots belong to
	 */
	public void add(Slotlist slotlist, int slotlistId) {
		for (SlotData slotData : slotlist.getAllSlots()) {
			SlotContainerDb container = new SlotContainerDb(slotData, slotId, slotlistId);
			idToSlot.put(slotId, container);
			slotId++;
		}
		for (Entry<String, SlotStatus> entry : slotlist.getAllReserve().entrySet()) {
			SlotContainerDb container = new SlotContainerDb(entry, slotId, slotlistId);
			idToSlot.put(slotId, container);
			slotId++;
		}
	}
	
	/**
	 * Returns the slot container that is represented by the given id.
	 * @param id Id of the slot
	 * @return Slot container that is represented by the given id
	 */
	public SlotContainerDb getSlot(int id) {
		SlotContainerDb slotContainer = idToSlot.get(id);
		if (slotContainer == null) {
			System.err.println("Database table does not know slot container with id: " + id);
		}
		return slotContainer;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Entry<Integer, SlotContainerDb> entry : idToSlot.entrySet()) {
			builder.append(entry.getValue() + ENTRY_SEPARATOR);
		}
		builder.delete(builder.length() - ENTRY_SEPARATOR.length(), builder.length());
		return builder.toString();
	}
}
