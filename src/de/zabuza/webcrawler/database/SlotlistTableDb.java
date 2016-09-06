package de.zabuza.webcrawler.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.zabuza.webcrawler.struct.Slotlist;

/**
 * Class that links slotlists with their ids.
 * @author Zabuza
 *
 */
public final class SlotlistTableDb {
	/**
	 * Singleton instance of this class.
	 */
	private static SlotlistTableDb instance = null;
	/**
	 * Default capacity of maps.
	 */
	private static final int DEFAULT_CAPACITY = 300;
	/**
	 * Separator character for entries in the text representation.
	 */
	public static final String ENTRY_SEPARATOR = ";";
	/**
	 * Dictionary for id to slotlist access.
	 */
	private final Map<Integer, SlotlistContainerDb> idToSlotlist = new HashMap<Integer, SlotlistContainerDb>(DEFAULT_CAPACITY);
	/**
	 * Id for the first slotlist which increments each time a slotlist gets added.
	 */
	private int slotlistId = 1;
	
	/**
	 * Creates a new slotlist table.
	 */
	private SlotlistTableDb() {
		
	}
	/**
	 * Adds the given slotlist to the table and assigns a unique id.
	 * @param slotlist Slotlist to add
	 * @param name Name of the slotlist
	 * @param owner Owner of the slotlist
	 * @param comment Comment of the slotlist
	 * @return Assigned id
	 */
	public int add(Slotlist slotlist, String name, int owner, String comment) {
		SlotlistContainerDb container = new SlotlistContainerDb(slotlistId, name, owner, comment, slotlist);
		idToSlotlist.put(slotlistId, container);
		slotlistId++;
		return slotlistId - 1;
	}
	
	/**
	 * Returns the slotlist that is represented by the given id.
	 * @param id Id of the slotlist
	 * @return Slotlist that is represented by the given id
	 */
	public Slotlist getSlotlist(int id) {
		Slotlist slotlist = idToSlotlist.get(id).getSlotlist();
		if (slotlist == null) {
			System.err.println("Database table does not know slotlist with id: " + id);
		}
		return slotlist;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Entry<Integer, SlotlistContainerDb> entry : idToSlotlist.entrySet()) {
			builder.append(entry.getValue() + ENTRY_SEPARATOR);
		}
		builder.delete(builder.length() - ENTRY_SEPARATOR.length(), builder.length());
		return builder.toString();
	}
	
	/**
	 * Gets the singleton instance of this class.
	 * @return Singleton instance of this class
	 */
	public static SlotlistTableDb getInstance() {
		if (instance == null) {
			instance = new SlotlistTableDb();
		}
		return instance;
	}
}
