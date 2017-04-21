package de.zabuza.webcrawler.struct;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.zabuza.webcrawler.database.EventDb;
import de.zabuza.webcrawler.database.MapTableDb;
import de.zabuza.webcrawler.database.SlotContainerDb;
import de.zabuza.webcrawler.database.SlotTableDb;
import de.zabuza.webcrawler.database.SlotTypeTableDb;
import de.zabuza.webcrawler.database.SlotlistTableDb;
import de.zabuza.webcrawler.database.UserTableDb;
import de.zabuza.webcrawler.enums.SlotStatus;
import de.zabuza.webcrawler.external.ExtEventData;
import de.zabuza.webcrawler.util.CrawlerUtil;

/**
 * List for event data.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 *
 */
public final class EventList implements Serializable, Iterable<EventData> {
	/**
	 * Path to the file that contains the export file.
	 */
	private static final String FILEPATH_EXPORT = "D:\\Samus Aran\\Eigene Dateien\\Intimist\\Gruppe W\\Clanleitung\\Gästemanagement\\Webcrawler\\exportDatabase.csv";
	/**
	 * Path to the file that contains the import file.
	 */
	private static final String FILEPATH_IMPORT = "D:\\Samus Aran\\Eigene Dateien\\Intimist\\Gruppe W\\Clanleitung\\Gästemanagement\\Webcrawler\\importDatabase.csv";
	/**
	 * Table header of events for input and output.
	 */
	private static final String IO_EVENT_TABLE_HEADER = "\"event_id\",\"user_id\",\"post_id\",\"thread_id\","
			+ "\"forum_id\",\"event_name\",\"player_number\",\"event_type\",\"event_date\","
			+ "\"event_time\",\"map\",\"slotlist\"," + "\"news_id\",\"calendar_id\",\"state\"";
	/**
	 * Table header of maps for input and output.
	 */
	private static final String IO_MAP_TABLE_HEADER = "\"map_id\",\"map_name\"";
	/**
	 * Table header of slots for input and output.
	 */
	private static final String IO_SLOT_TABLE_HEADER = "\"uniqueID\",\"slotlistID\",\"slotNumber\",\"slotid\","
			+ "\"customName\",\"assignedUserID\",\"customUser\",\"attendance\"";
	/**
	 * Table header of slotlists for input and output.
	 */
	private static final String IO_SLOTLIST_TABLE_HEADER = "\"uniqueID\",\"slotlistName\",\"owner\",\"comment\"";
	/**
	 * Table header of slot types for input and output.
	 */
	private static final String IO_SLOTTYPE_TABLE_HEADER = "\"slotid\",\"slotshort\",\"slotlong\"";

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Imports this object from a database usable format as a csv-file at the
	 * given path.
	 * 
	 * @return The imported event list
	 */
	public static EventList importDatabaseFormat() {
		List<String> content = null;
		try {
			content = CrawlerUtil.getFileContent(FILEPATH_IMPORT);
		} catch (final IOException e) {
			System.err.println("Unknown error while reading database import.");
			e.printStackTrace();
		}

		if (content == null) {
			throw new AssertionError();
		}

		// Collect all events
		int i = 1;
		String line = "";
		int amountOfEvents = 0;
		final Map<Integer, EventDb> idToEventDb = new HashMap<>();
		line = content.get(i);
		while (!line.equals(IO_MAP_TABLE_HEADER)) {
			final EventDb eventDb = new EventDb(line);
			idToEventDb.put(Integer.valueOf(eventDb.getSlotlistId()), eventDb);

			amountOfEvents++;
			i++;
			line = content.get(i);
		}

		// Create slotlists, add slots and link all to slotlist ids
		final Map<Integer, Slotlist> idToSlotlist = new HashMap<>();
		// Skip everything to start of slots
		do {
			line = content.get(i);
			i++;
		} while (!line.equals(IO_SLOT_TABLE_HEADER));

		// Parse slots
		line = content.get(i);
		while (!line.equals(IO_SLOTTYPE_TABLE_HEADER)) {
			final SlotContainerDb container = new SlotContainerDb(line);
			final SlotData slotData = container.toSlotData();

			final Integer slotlistId = Integer.valueOf(container.getSlotlistID());
			if (!idToSlotlist.containsKey(slotlistId)) {
				idToSlotlist.put(slotlistId, new Slotlist(idToEventDb.get(slotlistId).getPlayerNumber()));
			}
			final Slotlist slotlist = idToSlotlist.get(slotlistId);
			slotlist.addSlot(slotData);

			i++;
			line = content.get(i);
		}

		// Create events, link them with slotlists and add them to the list
		final EventList eventList = new EventList(amountOfEvents);
		for (final Entry<Integer, EventDb> entry : idToEventDb.entrySet()) {
			final EventData eventData = entry.getValue().toEventData(idToSlotlist.get(entry.getKey()));
			eventList.add(eventData);
		}

		return eventList;
	}

	/**
	 * List of all event data.
	 */
	private final List<EventData> list;

	/**
	 * Creates a new event list.
	 */
	public EventList() {
		this.list = new ArrayList<>();
	}

	/**
	 * Creates a new event list with given initial capacity.
	 * 
	 * @param initialCapacity
	 *            Initial capacity of list
	 */
	public EventList(final int initialCapacity) {
		this.list = new ArrayList<>(initialCapacity);
	}

	/**
	 * Adds the event data element to the list.
	 * 
	 * @param data
	 *            Element to add
	 * @return If the element could be added
	 */
	public boolean add(final EventData data) {
		return this.list.add(data);
	}

	/**
	 * Exports this object into a database usable format and saves it as
	 * csv-file at the given path.
	 */
	public void exportDatabaseFormat() {
		// Initialize some tables beginning with earlier events
		final SlotlistTableDb slotlistTableDb = SlotlistTableDb.getInstance();
		final Map<EventData, Integer> slotlistIdToEventData = new HashMap<>(this.list.size());
		final SlotTableDb slotTableDb = new SlotTableDb();
		for (int i = this.list.size() - 1; i >= 0; i--) {
			final EventData event = this.list.get(i);
			final String name = event.getName() + " - Slotlist";
			final int owner = UserTableDb.getInstance().getId(event.getCreator()).intValue();
			final String comment = "auto-generated";
			final Slotlist slotlist = event.getSlotlist();
			final int id = slotlistTableDb.add(slotlist, name, owner, comment);
			slotlistIdToEventData.put(event, Integer.valueOf(id));
			slotTableDb.add(slotlist, id);
		}
		final List<String> result = new ArrayList<>();

		// Event table
		result.add(IO_EVENT_TABLE_HEADER);
		for (int i = this.list.size() - 1; i >= 0; i--) {
			final EventData event = this.list.get(i);
			result.add(new EventDb(event, slotlistIdToEventData.get(event).intValue()).toString());
		}
		// Map table
		result.add(IO_MAP_TABLE_HEADER);
		final MapTableDb mapTableDb = MapTableDb.getInstance();
		final String[] mapEntries = mapTableDb.toString().split(MapTableDb.ENTRY_SEPARATOR);
		for (final String mapEntry : mapEntries) {
			result.add(mapEntry);
		}
		// Slotlist table
		result.add(IO_SLOTLIST_TABLE_HEADER);
		final String[] slotlistEntries = slotlistTableDb.toString().split(SlotlistTableDb.ENTRY_SEPARATOR);
		for (final String slotlistEntry : slotlistEntries) {
			result.add(slotlistEntry);
		}
		// Slot table
		result.add(IO_SLOT_TABLE_HEADER);
		final String[] slotEntries = slotTableDb.toString().split(SlotlistTableDb.ENTRY_SEPARATOR);
		for (final String slotEntry : slotEntries) {
			result.add(slotEntry);
		}
		// SlotType table
		result.add(IO_SLOTTYPE_TABLE_HEADER);
		final SlotTypeTableDb slotTypeTableDb = SlotTypeTableDb.getInstance();
		final String[] slotTypeEntries = slotTypeTableDb.toString().split(SlotTypeTableDb.ENTRY_SEPARATOR);
		for (final String slotTypeEntry : slotTypeEntries) {
			result.add(slotTypeEntry);
		}

		// Save export
		try (final BufferedWriter wr = new BufferedWriter(new FileWriter(FILEPATH_EXPORT))) {
			for (int i = 0; i < result.size(); i++) {
				wr.write(result.get(i));
				if (i < result.size() - 1) {
					wr.write("\n");
				}
			}
		} catch (final IOException e) {
			System.err.println("Unknown error while saving database export.");
			e.printStackTrace();
		}
	}

	/**
	 * Exports this to an external event data object.
	 * 
	 * @return External event data object of this
	 */
	public Map<Calendar, ExtEventData> exportToExtEventDataMap() {
		final Map<Calendar, ExtEventData> map = new TreeMap<>();
		for (final EventData data : this.list) {
			final ExtEventData extEventData = new ExtEventData(data.getType(), data.getDate());
			final Slotlist slotlist = data.getSlotlist();
			for (final SlotData slotData : slotlist.getAllSlots()) {
				extEventData.addPlayer(slotData.getPlayer(), slotData.getStatus());
			}
			for (final Entry<String, SlotStatus> entry : slotlist.getAllReserve().entrySet()) {
				extEventData.addPlayer(entry.getKey(), entry.getValue());
			}
			map.put(data.getDate(), extEventData);
		}

		return map;
	}

	/**
	 * Amount of events in this list.
	 * 
	 * @return amount of events in this list
	 */
	public int getSize() {
		return this.list.size();
	}

	@Override
	public Iterator<EventData> iterator() {
		return this.list.iterator();
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		for (final EventData datum : this.list) {
			builder.append(datum + "\n");
		}
		return builder.toString();
	}
}