package de.zabuza.webcrawler.external;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.zabuza.webcrawler.enums.EventType;
import de.zabuza.webcrawler.enums.SlotStatus;

/**
 * Data container for external event data.
 * Stores different data like slotted player or player status.
 * 
 * @author Zabuza
 */
public final class ExtEventData {
	/**
	 * Default expected size of an event.
	 */
	private final static int DEFAULT_SIZE = 30;
	/**
	 * Type of the event.
	 */
	private final EventType type;
	/**
	 * Date the event took place.
	 */
	private final Calendar date;
	/**
	 * Players and their status of this event.
	 */
	private final Map<String, SlotStatus> players;
	
	/**
	 * Creates a new data container for external event data.
	 * @param thatType Type of the event
	 * @param thatDate Date of the event
	 */
	public ExtEventData(EventType thatType, Calendar thatDate) {
		this.type = thatType;
		this.date = thatDate;
		players = new HashMap<String, SlotStatus>(DEFAULT_SIZE);
	}
	
	/**
	 * Adds a player and his status to this event.
	 * @param player Player to add
	 * @param status Status of the player
	 */
	public void addPlayer(String player, SlotStatus status) {
		players.put(player, status);
	}
	
	/**
	 * Returns the players status or null if that
	 * players has not participated on this event.
	 * 
	 * @param player Player to get status for
	 */
	public SlotStatus getPlayerStatus(String player) {
		return players.get(player);
	}

	/**
	 * @return the date
	 */
	public Calendar getDate() {
		return date;
	}

	/**
	 * @return the type
	 */
	public EventType getType() {
		return type;
	}
	
	/**
	 * Gets the list of players for this event.
	 * @return List of players
	 */
	public Set<String> getPlayers() {
		return players.keySet();
	}
}