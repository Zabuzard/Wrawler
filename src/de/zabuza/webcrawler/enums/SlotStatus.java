package de.zabuza.webcrawler.enums;

/**
 * Enumeration of all slot status.
 *  
 * @author Zabuza
 */
public enum SlotStatus {
	/**
	 * Status is unknown.
	 */
	UNKNOWN,
	/**
	 * Signed in and appeared at event.
	 */
	APPEARED,
	/**
	 * Signed in, appeared at the event but was not prepared in time.
	 */
	PREPARED_LATE,
	/**
	 * Signed in and later signed out from event.
	 */
	SIGNED_OUT,
	/**
	 * Signed in and signed out too late from event.
	 */
	SIGNED_OUT_LATE,
	/**
	 * Signed in and was absent at event.
	 */
	ABSENT
}