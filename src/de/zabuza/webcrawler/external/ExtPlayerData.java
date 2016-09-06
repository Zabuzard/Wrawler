package de.zabuza.webcrawler.external;

import java.util.Calendar;

/**
 * Data container for external player data.
 * Stores different data like player registration.
 * 
 * @author Zabuza
 */
public final class ExtPlayerData {
	/**
	 * Name of the player.
	 */
	private final String player;
	/**
	 * Date of players registration.
	 */
	private final Calendar registrationDate;
	/**
	 * Date of players reactivation or null if not happened.
	 */
	private final Calendar reactivationDate;
	/**
	 * True if player was notified of inactivity, false if not.
	 */
	private final boolean inactivityNotification;
	
	/**
	 * Creates a new data container for external player data.
	 * @param thatPlayer Name of the player
	 * @param thatRegistrationDate Date of players registration
	 * @param thatReactivationDate Date of players reactivation or null if not happened
	 * @param thatInactivityNotification True if player was notified of inactivity, false if not
	 */
	public ExtPlayerData(String thatPlayer, Calendar thatRegistrationDate,
			Calendar thatReactivationDate, boolean thatInactivityNotification) {
		this.player = thatPlayer;
		this.registrationDate = thatRegistrationDate;
		this.reactivationDate = thatReactivationDate;
		this.inactivityNotification = thatInactivityNotification;
	}

	/**
	 * @return the player
	 */
	public String getPlayer() {
		return player;
	}

	/**
	 * @return the registrationDate
	 */
	public Calendar getRegistrationDate() {
		return registrationDate;
	}

	/**
	 * @return the reactivationDate
	 */
	public Calendar getReactivationDate() {
		return reactivationDate;
	}

	/**
	 * @return the inactivityNotification
	 */
	public boolean isInactivityNotification() {
		return inactivityNotification;
	}
}