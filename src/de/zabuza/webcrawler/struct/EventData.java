package de.zabuza.webcrawler.struct;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map.Entry;

import de.zabuza.webcrawler.enums.EventType;
import de.zabuza.webcrawler.enums.SlotStatus;
import de.zabuza.webcrawler.util.CrawlerUtil;

/**
 * Data container for an event. Stores different data like name or type.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 */
public final class EventData implements Serializable {
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Creator of the event.
	 */
	private final String creator;
	/**
	 * Date the event took place.
	 */
	private final Calendar date;
	/**
	 * Map the event takes place at.
	 */
	private final String map;
	/**
	 * Name of the event.
	 */
	private final String name;
	/**
	 * Id of events forum thread opening post.
	 */
	private final int postId;
	/**
	 * Size of the event or amount of players.
	 */
	private final int size;
	/**
	 * Slot-list of the event.
	 */
	private final Slotlist slotlist;
	/**
	 * Id of events forum thread.
	 */
	private final int threadId;
	/**
	 * Time the event started.
	 */
	private final Calendar time;
	/**
	 * Type of the event.
	 */
	private final EventType type;

	/**
	 * Creates a new data container for an event.
	 * 
	 * @param thatName
	 *            Name of the event
	 * @param thatType
	 *            Type of the event
	 * @param thatSize
	 *            Size of the event
	 * @param thatCreator
	 *            Creator of the event
	 * @param thatMap
	 *            Map the event takes place at
	 * @param thatDate
	 *            Date the event took place
	 * @param thatTime
	 *            Time the event started
	 * @param thatThreadId
	 *            Id of events forum thread
	 * @param thatPostId
	 *            Id of the opening post
	 * @param thatSlotlist
	 *            Slot-list of the event
	 */
	public EventData(String thatName, EventType thatType, int thatSize, String thatCreator, String thatMap,
			Calendar thatDate, Calendar thatTime, int thatThreadId, int thatPostId, Slotlist thatSlotlist) {
		this.name = thatName;
		this.type = thatType;
		this.size = thatSize;
		this.creator = thatCreator;
		this.map = thatMap;
		this.date = thatDate;
		this.time = thatTime;
		this.threadId = thatThreadId;
		this.postId = thatPostId;
		this.slotlist = thatSlotlist;
	}

	/**
	 * @return the creator
	 */
	public String getCreator() {
		return this.creator;
	}

	/**
	 * @return the date
	 */
	public Calendar getDate() {
		return this.date;
	}

	/**
	 * @return the map
	 */
	public String getMap() {
		return this.map;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the postId
	 */
	public int getPostId() {
		return this.postId;
	}

	/**
	 * @return the size
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * @return the slot-list
	 */
	public Slotlist getSlotlist() {
		return this.slotlist;
	}

	/**
	 * @return the threadId
	 */
	public int getThreadId() {
		return this.threadId;
	}

	/**
	 * @return the time
	 */
	public Calendar getTime() {
		return this.time;
	}

	/**
	 * @return the type
	 */
	public EventType getType() {
		return this.type;
	}

	@Override
	public String toString() {
		String dateText = CrawlerUtil.convertDateToString(this.date);

		String header = "#" + this.threadId + "(" + this.postId + "): [" + dateText + "] " + this.type + this.size + " "
				+ this.name + " by " + this.creator + " at " + this.map + "\n";

		String slotlistText = "";
		for (int i = 0; i < this.slotlist.slotSize(); i++) {
			SlotData data = this.slotlist.getSlotData(i);
			slotlistText += "\t#" + data + "\n";
		}
		for (Entry<String, SlotStatus> entry : this.slotlist.getAllReserve().entrySet()) {
			slotlistText += "\t#" + entry.getKey() + " - " + entry.getValue() + "\n";
		}

		return header + slotlistText;
	}
}