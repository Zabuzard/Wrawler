package de.zabuza.webcrawler.database;

import java.util.Calendar;

import de.zabuza.webcrawler.enums.EventType;
import de.zabuza.webcrawler.struct.EventData;
import de.zabuza.webcrawler.struct.Slotlist;
import de.zabuza.webcrawler.util.CrawlerUtil;

/**
 * Class that represents the data of an event in the database format.
 * @author Zabuza
 *
 */
public final class EventDb {
	/**
	 * Separator character for values in the text representation.
	 */
	private static final String SEPARATOR = ",";
	/**
	 * Enclosing character for values in the text representation.
	 */
	private static final String ENCLOSER = "\"";
	/**
	 * Default forum id if forum is not known.
	 */
	private static final int DEFAULT_FORUM_ID = 4;
	/**
	 * Starting id for events.
	 */
	private static int starting_eventId = 1;
	
	/**
	 * Id of the event.
	 */
	private final String event_id ;
	/**
	 * Id of events creator.
	 */
	private final String user_id;
	/**
	 * Id of events post.
	 */
	private final String post_id;
	/**
	 * Id of events thread.
	 */
	private final String thread_id;
	/**
	 * Id of the forum the events is in.
	 */
	private final String forum_id;
	/**
	 * Name of the event.
	 */
	private final String event_name;
	/**
	 * Number of players of this event.
	 */
	private final String player_number;
	/**
	 * Type of the event.
	 */
	private final String event_type;
	/**
	 * Date where the event is.
	 */
	private final String event_date;
	/**
	 * Time where the event starts.
	 */
	private final String event_time;
	/**
	 * Id of events map.
	 */
	private final String map;
	/**
	 * Id of events slotlist.
	 */
	private final String slotlist;
	/**
	 * Id of the event in the news.
	 */
	private final String news_id;
	/**
	 * Id of the event in the calendar.
	 */
	private final String calendar_id;
	/**
	 * State of the event.
	 */
	private final String state;
	
	/**
	 * Creates a new EventDb object out of the given eventData object.
	 * @param eventData EventData object to create EventDb object out of
	 * @param slotlistId Id of events slotlist
	 */
	public EventDb(EventData eventData, int slotlistId) {
		event_id = starting_eventId + "";
		user_id = UserTableDb.getInstance().getId(eventData.getCreator()) + "";
		post_id = eventData.getPostId() + "";
		thread_id = eventData.getThreadId() + "";
		forum_id = DEFAULT_FORUM_ID + "";
		event_name = eventData.getName();
		player_number = eventData.getSize() + "";
		event_type = getIdByEventType(eventData.getType()) + "";
		event_date = convertDateToDbFormat(eventData.getDate());
		event_time = CrawlerUtil.convertTimeToString(eventData.getTime());
		map = MapTableDb.getInstance().getId(eventData.getMap()) + "";
		slotlist = slotlistId + "";
		news_id = "0";
		calendar_id = "0";
		state = "7";
		
		starting_eventId++;
	}
	
	/**
	 * Creates a new EventDb object out of a database formatted text.
	 * @param databaseFormatLine Text in the database format
	 */
	public EventDb(String databaseFormatLine) {
		String[] values = CrawlerUtil.parseDatabaseFormatLine(databaseFormatLine);
		event_id = values[0];
		user_id = values[1];
		post_id = values[2];
		thread_id = values[3];
		forum_id = values[4];
		event_name = values[5];
		player_number = values[6];
		event_type = values[7];
		event_date = values[8];
		event_time = values[9];
		map = values[10];
		slotlist = values[11];
		news_id = values[12];
		calendar_id = values[13];
		state = values[14];
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(ENCLOSER + event_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + user_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + post_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + thread_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + forum_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + event_name + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + player_number + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + event_type + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + event_date + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + event_time + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + map + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + slotlist + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + news_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + calendar_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + state + ENCLOSER);
		return builder.toString();
	}
	/**
	 * Returns a EventData representation of this object.
	 * @param slotlist Slotlist for this event data
	 * @return A EventData representation of this object.
	 */
	public EventData toEventData(Slotlist slotlist) {
		EventType type = getEventTypeById(Integer.parseInt(event_type));
		String creator = UserTableDb.getInstance().getUser(Integer.parseInt(user_id));
		String thatMap = MapTableDb.getInstance().getMap(Integer.parseInt(map));
		Calendar thatDate = convertDateFromDbFormat(event_date);
		Calendar thatTime = CrawlerUtil.convertStringToTime(event_time);
		EventData eventData = new EventData(event_name, type, Integer.parseInt(player_number),
				creator, thatMap, thatDate, thatTime, Integer.parseInt(thread_id),
				Integer.parseInt(post_id), slotlist);
		return eventData;
	}
	/**
	 * Gets the slotlist id of this event.
	 * @return Slotlist id of this event
	 */
	public int getSlotlistId() {
		return Integer.parseInt(slotlist);
	}
	/**
	 * Gets the player number of this event.
	 * @return Player number of this event
	 */
	public int getPlayerNumber() {
		return Integer.parseInt(player_number);
	}
	/**
	 * Converts the given date into a database readable format.
	 * @param date Date to convert
	 * @return Date in a database readable format
	 */
	private static String convertDateToDbFormat(Calendar date) {
		int year = date.get(Calendar.YEAR);
		int month = date.get(Calendar.MONTH) + 1;
		int day  = date.get(Calendar.DAY_OF_MONTH);
		String yearText = year + "";
		String monthText = month + "";
		String dayText = day + "";
		if (month < 10) {
			monthText = "0" + month;
		}
		if (day < 10) {
			dayText = "0" + day;
		}
		return yearText + "-" + monthText + "-" + dayText;
	}
	/**
	 * Converts the given date from a database readable format into a calendar object.
	 * @param date Date to convert
	 * @return Date as calendar object
	 */
	private static Calendar convertDateFromDbFormat(String dbFormat) {
		Calendar date = Calendar.getInstance();
		
		int year = Integer.parseInt(dbFormat.substring(0, 4));
		int month = Integer.parseInt(dbFormat.substring(5, 7)) - 1;
		int day = Integer.parseInt(dbFormat.substring(8, 10));
		
		date.set(Calendar.YEAR, year);
		date.set(Calendar.MONTH, month);
		date.set(Calendar.DAY_OF_MONTH, day);
		return date;
	}
	/**
	 * Gets the database id of an event type by the type.
	 * @param type Type to get database id of
	 * @return Database id of the event type
	 */
	public static int getIdByEventType(EventType type) {
		switch (type) {
			case BLACKBOX :
				return 1;
			case COOP : 
				return 2;
			case COOP_PLUS:
				return 3;
			case COMPETITION:
				return 4;
			case MILSIM:
				return 5;
			case ORGA:
				return 6;
			case TVT:
				return 7;
			case ZEUS:
				return 8;
			default :
				System.err.println("Unknown event type while creating database event: " + type);
				return 0;
		}
	}
	/**
	 * Gets the event type by its database id.
	 * @param id Database id to get event type of
	 * @return Event type to get
	 */
	public static EventType getEventTypeById(int id) {
		switch (id) {
		case 1 :
			return EventType.BLACKBOX;
		case 2 : 
			return EventType.COOP;
		case 3:
			return EventType.COOP_PLUS;
		case 4:
			return EventType.COMPETITION;
		case 5:
			return EventType.MILSIM;
		case 6:
			return EventType.ORGA;
		case 7:
			return EventType.TVT;
		case 8:
			return EventType.ZEUS;
		default :
			System.err.println("Unknown event type id while creating database event: " + id);
			return EventType.NO_TYPE;
	}
	}
}