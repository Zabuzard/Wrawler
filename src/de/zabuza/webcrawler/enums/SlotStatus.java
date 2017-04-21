package de.zabuza.webcrawler.enums;

/**
 * Enumeration of all slot status.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 */
public enum SlotStatus {
	/**
	 * Signed in and was absent at event.
	 */
	ABSENT,
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
	 * Status is unknown.
	 */
	UNKNOWN
}