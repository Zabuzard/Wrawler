package de.zabuza.webcrawler.enums;

/**
 * Enumeration of all event types.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 */
public enum EventType {
	/**
	 * No valid type for an event.
	 */
	NO_TYPE,
	/**
	 * Cooperative event.
	 */
	COOP,
	/**
	 * Cooperative+ event.
	 */
	COOP_PLUS,
	/**
	 * Team versus Team event.
	 */
	TVT,
	/**
	 * Black-box event.
	 */
	BLACKBOX,
	/**
	 * Military simulation event.
	 */
	MILSIM,
	/**
	 * Competition event.
	 */
	COMPETITION,
	/**
	 * Organization event.
	 */
	ORGA,
	/**
	 * Zeus event.
	 */
	ZEUS
}