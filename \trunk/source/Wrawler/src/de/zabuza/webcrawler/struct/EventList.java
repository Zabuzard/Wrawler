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
 * @author Zabuza
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
			+ "\"event_time\",\"map\",\"slotlist\","
			+ "\"news_id\",\"calendar_id\",\"state\"";
	/**
	 * Table header of maps for input and output.
	 */
	private static final String IO_MAP_TABLE_HEADER = "\"map_id\",\"map_name\"";
	/**
	 * Table header of slotlists for input and output.
	 */
	private static final String IO_SLOTLIST_TABLE_HEADER = "\"uniqueID\",\"slotlistName\",\"owner\",\"comment\"";
	/**
	 * Table header of slots for input and output.
	 */
	private static final String IO_SLOT_TABLE_HEADER = "\"uniqueID\",\"slotlistID\",\"slotNumber\",\"slotid\","
			+ "\"customName\",\"assignedUserID\",\"customUser\",\"attendance\"";
	/**
	 * Table header of slot types for input and output.
	 */
	private static final String IO_SLOTTYPE_TABLE_HEADER = "\"slotid\",\"slotshort\",\"slotlong\"";
	
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * List of all event data.
	 */
	private final List<EventData> list;
	
	/**
	 * Creates a new event list.
	 */
	public EventList() {
		list = new ArrayList<EventData>();
	}
	
	/**
	 * Creates a new event list with given initial capacity.
	 * @param initialCapacity Initial capacity of list
	 */
	public EventList(int initialCapacity) {
		list = new ArrayList<EventData>(initialCapacity);
	}
	
	/**
	 * Adds the event data element to the list.
	 * @param data Element to add
	 * @return If the element could be added
	 */
	public boolean add(EventData data) {
		return list.add(data);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (EventData datum : list) {
			builder.append(datum + "\n");
		}
		return builder.toString();
	}
	
	/**
	 * Amount of events in this list.
	 * @return amount of events in this list
	 */
	public int getSize() {
		return list.size();
	}

	@Override
	public Iterator<EventData> iterator() {
		return list.iterator();
	}
	/**
	 * Exports this object into a database usable format
	 * and saves it as csv-file at the given path.
	 */
	public void exportDatabaseFormat() {
		//Initialize some tables beginning with earlier events
		SlotlistTableDb slotlistTableDb = SlotlistTableDb.getInstance();
		Map<EventData, Integer> slotlistIdToEventData = new HashMap<EventData, Integer>(list.size());
		SlotTableDb slotTableDb = new SlotTableDb();
		for (int i = list.size() - 1; i >= 0; i--) {
			EventData event = list.get(i);
			String name = event.getName() + " - Slotlist";
			int owner = UserTableDb.getInstance().getId(event.getCreator());
			String comment = "auto-generated";
			Slotlist slotlist = event.getSlotlist();
			int id = slotlistTableDb.add(slotlist, name, owner, comment);
			slotlistIdToEventData.put(event, id);
			slotTableDb.add(slotlist, id);
		}
		List<String> result = new ArrayList<String>();
		
		//Event table
		result.add(IO_EVENT_TABLE_HEADER);
		for (int i = list.size() - 1; i >= 0; i--) {
			EventData event = list.get(i);
			result.add(new EventDb(event, slotlistIdToEventData.get(event)).toString());
		}
		//Map table
		result.add(IO_MAP_TABLE_HEADER);
		MapTableDb mapTableDb = MapTableDb.getInstance();
		String[] mapEntries = mapTableDb.toString().split(MapTableDb.ENTRY_SEPARATOR);
		for (String mapEntry : mapEntries) {
			result.add(mapEntry);
		}
		//Slotlist table
		result.add(IO_SLOTLIST_TABLE_HEADER);
		String[] slotlistEntries = slotlistTableDb.toString().split(SlotlistTableDb.ENTRY_SEPARATOR);
		for (String slotlistEntry : slotlistEntries) {
			result.add(slotlistEntry);
		}
		//Slot table
		result.add(IO_SLOT_TABLE_HEADER);
		String[] slotEntries = slotTableDb.toString().split(SlotlistTableDb.ENTRY_SEPARATOR);
		for (String slotEntry : slotEntries) {
			result.add(slotEntry);
		}
		//SlotType table
		result.add(IO_SLOTTYPE_TABLE_HEADER);
		SlotTypeTableDb slotTypeTableDb = SlotTypeTableDb.getInstance();
		String[] slotTypeEntries = slotTypeTableDb.toString().split(SlotTypeTableDb.ENTRY_SEPARATOR);
		for (String slotTypeEntry : slotTypeEntries) {
			result.add(slotTypeEntry);
		}
		
		//Save export
		BufferedWriter wr = null;
		try {
			wr = new BufferedWriter(new FileWriter(FILEPATH_EXPORT));
			for (int i = 0; i < result.size(); i++) {
				wr.write(result.get(i));
				if (i < result.size() - 1) {
					wr.write("\n");
				}
			}
		} catch (IOException e) {
			System.err.println("Unknown error while saving database export.");
			e.printStackTrace();
		} finally {
			try {
				wr.close();
			} catch (IOException e) {
				System.err.println("Unknown error while closing database export stream.");
				e.printStackTrace();
			}
		}
	}
	/**
	 * Imports this object from a database usable format
	 * as a csv-file at the given path.
	 */
	public static EventList importDatabaseFormat() {
		List<String> content = null;
		try {
			content = CrawlerUtil.getFileContent(FILEPATH_IMPORT);
		} catch (IOException e) {
			System.err.println("Unknown error while reading database import.");
			e.printStackTrace();
		}
		
		//Collect all events
		int i = 1;
		String line = "";
		int amountOfEvents = 0;
		Map<Integer, EventDb> idToEventDb = new HashMap<Integer, EventDb>();
		line = content.get(i);
		while (!line.equals(IO_MAP_TABLE_HEADER)) {
			EventDb eventDb = new EventDb(line);
			idToEventDb.put(eventDb.getSlotlistId(), eventDb);
			
			amountOfEvents++;
			i++;
			line = content.get(i);
		}
		
		//Create slotlists, add slots and link all to slotlist ids
		Map<Integer, Slotlist> idToSlotlist = new HashMap<Integer, Slotlist>();
		//Skip everything to start of slots
		do {
			line = content.get(i);
			i++;
		} while (!line.equals(IO_SLOT_TABLE_HEADER));
		
		//Parse slots
		line = content.get(i);
		while (!line.equals(IO_SLOTTYPE_TABLE_HEADER)) {
			SlotContainerDb container = new SlotContainerDb(line);
			SlotData slotData = container.toSlotData();
			
			int slotlistId = container.getSlotlistID();
			if (!idToSlotlist.containsKey(slotlistId)) {
				idToSlotlist.put(slotlistId,
						new Slotlist(idToEventDb.get(slotlistId).getPlayerNumber()));
			}
			Slotlist slotlist = idToSlotlist.get(slotlistId);
			slotlist.addSlot(slotData);
			
			i++;
			line = content.get(i);
		}
		
		//Create events, link them with slotlists and add them to the list
		EventList eventList = new EventList(amountOfEvents);
		for (Entry<Integer, EventDb> entry : idToEventDb.entrySet()) {
			EventData eventData = entry.getValue()
					.toEventData(idToSlotlist.get(entry.getKey()));
			eventList.add(eventData);
		}
		
		return eventList;
	}
	/**
	 * Exports this to an external event data object.
	 * @return External event data object of this
	 */
	public Map<Calendar, ExtEventData> exportToExtEventDataMap() {
		Map<Calendar, ExtEventData> map = new TreeMap<Calendar, ExtEventData>();
		for (EventData data : list) {
			ExtEventData extEventData = new ExtEventData(data.getType(), data.getDate());
			Slotlist slotlist = data.getSlotlist();
			for (SlotData slotData : slotlist.getAllSlots()) {
				extEventData.addPlayer(slotData.getPlayer(), slotData.getStatus());
			}
			for (Entry<String, SlotStatus> entry : slotlist.getAllReserve().entrySet()) {
				extEventData.addPlayer(entry.getKey(), entry.getValue());
			}
			map.put(data.getDate(), extEventData);
		}
		
		return map;
	}
}