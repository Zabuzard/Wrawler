package de.zabuza.webcrawler.database;

import java.util.Calendar;

import de.zabuza.webcrawler.enums.EventType;
import de.zabuza.webcrawler.struct.EventData;
import de.zabuza.webcrawler.struct.Slotlist;
import de.zabuza.webcrawler.util.CrawlerUtil;

/**
 * Class that represents the data of an event in the database format.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 *
 */
public final class EventDb {
	/**
	 * Default forum id if forum is not known.
	 */
	private static final int DEFAULT_FORUM_ID = 4;
	/**
	 * Enclosing character for values in the text representation.
	 */
	private static final String ENCLOSER = "\"";
	/**
	 * Separator character for values in the text representation.
	 */
	private static final String SEPARATOR = ",";
	/**
	 * Starting id for events.
	 */
	private static int starting_eventId = 1;

	/**
	 * Gets the event type by its database id.
	 * 
	 * @param id
	 *            Database id to get event type of
	 * @return Event type to get
	 */
	public static EventType getEventTypeById(final int id) {
		switch (id) {
		case 1:
			return EventType.BLACKBOX;
		case 2:
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
		default:
			System.err.println("Unknown event type id while creating database event: " + id);
			return EventType.NO_TYPE;
		}
	}

	/**
	 * Gets the database id of an event type by the type.
	 * 
	 * @param type
	 *            Type to get database id of
	 * @return Database id of the event type
	 */
	public static int getIdByEventType(final EventType type) {
		switch (type) {
		case BLACKBOX:
			return 1;
		case COOP:
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
		case NO_TYPE:
			System.err.println("Unknown event type while creating database event: " + type);
			return 0;
		default:
			System.err.println("Unknown event type while creating database event: " + type);
			return 0;
		}
	}

	/**
	 * Converts the given date from a database readable format into a calendar
	 * object.
	 * 
	 * @param dbFormat
	 *            The date to convert in the database format
	 * @return Date as calendar object
	 */
	private static Calendar convertDateFromDbFormat(final String dbFormat) {
		final Calendar date = Calendar.getInstance();

		final int year = Integer.parseInt(dbFormat.substring(0, 4));
		final int month = Integer.parseInt(dbFormat.substring(5, 7)) - 1;
		final int day = Integer.parseInt(dbFormat.substring(8, 10));

		date.set(Calendar.YEAR, year);
		date.set(Calendar.MONTH, month);
		date.set(Calendar.DAY_OF_MONTH, day);
		return date;
	}

	/**
	 * Converts the given date into a database readable format.
	 * 
	 * @param date
	 *            Date to convert
	 * @return Date in a database readable format
	 */
	private static String convertDateToDbFormat(final Calendar date) {
		final int year = date.get(Calendar.YEAR);
		final int month = date.get(Calendar.MONTH) + 1;
		final int day = date.get(Calendar.DAY_OF_MONTH);
		final String yearText = year + "";
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
	 * Id of the event in the calendar.
	 */
	private final String mCalendar_id;
	/**
	 * Date where the event is.
	 */
	private final String mEvent_date;
	/**
	 * Id of the event.
	 */
	private final String mEvent_id;
	/**
	 * Name of the event.
	 */
	private final String mEvent_name;
	/**
	 * Time where the event starts.
	 */
	private final String mEvent_time;
	/**
	 * Type of the event.
	 */
	private final String mEvent_type;
	/**
	 * Id of the forum the events is in.
	 */
	private final String mForum_id;
	/**
	 * Id of events map.
	 */
	private final String mMap;
	/**
	 * Id of the event in the news.
	 */
	private final String mNews_id;
	/**
	 * Number of players of this event.
	 */
	private final String mPlayer_number;
	/**
	 * Id of events post.
	 */
	private final String mPost_id;

	/**
	 * Id of events slotlist.
	 */
	private final String mSlotlist;

	/**
	 * State of the event.
	 */
	private final String mState;

	/**
	 * Id of events thread.
	 */
	private final String mThread_id;
	/**
	 * Id of events creator.
	 */
	private final String mUser_id;

	/**
	 * Creates a new EventDb object out of the given eventData object.
	 * 
	 * @param eventData
	 *            EventData object to create EventDb object out of
	 * @param slotlistId
	 *            Id of events slotlist
	 */
	public EventDb(final EventData eventData, final int slotlistId) {
		this.mEvent_id = starting_eventId + "";
		this.mUser_id = UserTableDb.getInstance().getId(eventData.getCreator()) + "";
		this.mPost_id = eventData.getPostId() + "";
		this.mThread_id = eventData.getThreadId() + "";
		this.mForum_id = DEFAULT_FORUM_ID + "";
		this.mEvent_name = eventData.getName();
		this.mPlayer_number = eventData.getSize() + "";
		this.mEvent_type = getIdByEventType(eventData.getType()) + "";
		this.mEvent_date = convertDateToDbFormat(eventData.getDate());
		this.mEvent_time = CrawlerUtil.convertTimeToString(eventData.getTime());
		this.mMap = MapTableDb.getInstance().getId(eventData.getMap()) + "";
		this.mSlotlist = slotlistId + "";
		this.mNews_id = "0";
		this.mCalendar_id = "0";
		this.mState = "7";

		starting_eventId++;
	}

	/**
	 * Creates a new EventDb object out of a database formatted text.
	 * 
	 * @param databaseFormatLine
	 *            Text in the database format
	 */
	public EventDb(final String databaseFormatLine) {
		final String[] values = CrawlerUtil.parseDatabaseFormatLine(databaseFormatLine);
		this.mEvent_id = values[0];
		this.mUser_id = values[1];
		this.mPost_id = values[2];
		this.mThread_id = values[3];
		this.mForum_id = values[4];
		this.mEvent_name = values[5];
		this.mPlayer_number = values[6];
		this.mEvent_type = values[7];
		this.mEvent_date = values[8];
		this.mEvent_time = values[9];
		this.mMap = values[10];
		this.mSlotlist = values[11];
		this.mNews_id = values[12];
		this.mCalendar_id = values[13];
		this.mState = values[14];
	}

	/**
	 * Gets the player number of this event.
	 * 
	 * @return Player number of this event
	 */
	public int getPlayerNumber() {
		return Integer.parseInt(this.mPlayer_number);
	}

	/**
	 * Gets the slotlist id of this event.
	 * 
	 * @return Slotlist id of this event
	 */
	public int getSlotlistId() {
		return Integer.parseInt(this.mSlotlist);
	}

	/**
	 * Returns a EventData representation of this object.
	 * 
	 * @param slotlist
	 *            Slotlist for this event data
	 * @return A EventData representation of this object.
	 */
	public EventData toEventData(final Slotlist slotlist) {
		final EventType type = getEventTypeById(Integer.parseInt(this.mEvent_type));
		final String creator = UserTableDb.getInstance().getUser(Integer.parseInt(this.mUser_id));
		final String thatMap = MapTableDb.getInstance().getMap(Integer.parseInt(this.mMap));
		final Calendar thatDate = convertDateFromDbFormat(this.mEvent_date);
		final Calendar thatTime = CrawlerUtil.convertStringToTime(this.mEvent_time);
		final EventData eventData = new EventData(this.mEvent_name, type, Integer.parseInt(this.mPlayer_number), creator,
				thatMap, thatDate, thatTime, Integer.parseInt(this.mThread_id), Integer.parseInt(this.mPost_id),
				slotlist);
		return eventData;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(ENCLOSER + this.mEvent_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mUser_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mPost_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mThread_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mForum_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mEvent_name + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mPlayer_number + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mEvent_type + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mEvent_date + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mEvent_time + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mMap + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mSlotlist + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mNews_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mCalendar_id + ENCLOSER + SEPARATOR);
		builder.append(ENCLOSER + this.mState + ENCLOSER);
		return builder.toString();
	}
}