package de.zabuza.webcrawler.external;

import java.util.Calendar;

/**
 * Data container for external player data. Stores different data like player
 * registration.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 */
public final class ExtPlayerData {
	/**
	 * True if player was notified of inactivity, false if not.
	 */
	private final boolean inactivityNotification;
	/**
	 * Name of the player.
	 */
	private final String player;
	/**
	 * Date of players reactivation or null if not happened.
	 */
	private final Calendar reactivationDate;
	/**
	 * Date of players registration.
	 */
	private final Calendar registrationDate;

	/**
	 * Creates a new data container for external player data.
	 * 
	 * @param thatPlayer
	 *            Name of the player
	 * @param thatRegistrationDate
	 *            Date of players registration
	 * @param thatReactivationDate
	 *            Date of players reactivation or null if not happened
	 * @param thatInactivityNotification
	 *            True if player was notified of inactivity, false if not
	 */
	public ExtPlayerData(String thatPlayer, Calendar thatRegistrationDate, Calendar thatReactivationDate,
			boolean thatInactivityNotification) {
		this.player = thatPlayer;
		this.registrationDate = thatRegistrationDate;
		this.reactivationDate = thatReactivationDate;
		this.inactivityNotification = thatInactivityNotification;
	}

	/**
	 * @return the player
	 */
	public String getPlayer() {
		return this.player;
	}

	/**
	 * @return the reactivationDate
	 */
	public Calendar getReactivationDate() {
		return this.reactivationDate;
	}

	/**
	 * @return the registrationDate
	 */
	public Calendar getRegistrationDate() {
		return this.registrationDate;
	}

	/**
	 * @return the inactivityNotification
	 */
	public boolean isInactivityNotification() {
		return this.inactivityNotification;
	}
}