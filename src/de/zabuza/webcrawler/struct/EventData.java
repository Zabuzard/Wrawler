package de.zabuza.webcrawler.struct;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map.Entry;

import de.zabuza.webcrawler.enums.EventType;
import de.zabuza.webcrawler.enums.SlotStatus;
import de.zabuza.webcrawler.util.CrawlerUtil;

/**
 * Data container for an event.
 * Stores different data like name or type.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 */
public final class EventData implements Serializable {
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Name of the event.
	 */
	private final String name;
	/**
	 * Type of the event.
	 */
	private final EventType type;
	/**
	 * Size of the event or amount of players.
	 */
	private final int size;
	/**
	 * Creator of the event.
	 */
	private final String creator;
	/**
	 * Map the event takes place at.
	 */
	private final String map;
	/**
	 * Date the event took place.
	 */
	private final Calendar date;
	/**
	 * Time the event started.
	 */
	private final Calendar time;
	/**
	 * Id of events forum thread.
	 */
	private final int threadId;
	/**
	 * Id of events forum thread opening post.
	 */
	private final int postId;
	/**
	 * Slot-list of the event.
	 */
	private final Slotlist slotlist;
	
	/**
	 * Creates a new data container for an event.
	 * @param thatName Name of the event
	 * @param thatType Type of the event
	 * @param thatSize Size of the event
	 * @param thatCreator Creator of the event
	 * @param thatMap Map the event takes place at
	 * @param thatDate Date the event took place
	 * @param thatTime Time the event started
	 * @param thatThreadId Id of events forum thread
	 * @param thatPostId Id of the opening post
	 * @param thatSlotlist Slot-list of the event
	 */
	public EventData(String thatName, EventType thatType,
			int thatSize, String thatCreator, String thatMap,
			Calendar thatDate, Calendar thatTime, int thatThreadId,
			int thatPostId, Slotlist thatSlotlist) {
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
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the creator
	 */
	public String getCreator() {
		return creator;
	}
	/**
	 * @return the date
	 */
	public Calendar getDate() {
		return date;
	}
	/**
	 * @return the time
	 */
	public Calendar getTime() {
		return time;
	}
	/**
	 * @return the type
	 */
	public EventType getType() {
		return type;
	}
	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}
	/**
	 * @return the threadId
	 */
	public int getThreadId() {
		return threadId;
	}
	/**
	 * @return the postId
	 */
	public int getPostId() {
		return postId;
	}
	/**
	 * @return the map
	 */
	public String getMap() {
		return map;
	}
	/**
	 * @return the slot-list
	 */
	public Slotlist getSlotlist() {
		return slotlist;
	}
	@Override
	public String toString() {
		String dateText = CrawlerUtil.convertDateToString(date);
		
		String header = "#" + threadId + "(" + postId +
				"): [" + dateText + "] " + type + size + " "
				+ name + " by " + creator + " at " + map + "\n";
		
		String slotlistText = "";
		for (int i = 0; i < slotlist.slotSize(); i++) {
			SlotData data = slotlist.getSlotData(i);
			slotlistText += "\t#" + data + "\n";
		}
		for (Entry<String, SlotStatus> entry : slotlist.getAllReserve().entrySet()) {
			slotlistText += "\t#" + entry.getKey() + " - " + entry.getValue() + "\n";
		}
		
		return header + slotlistText;
	}
}