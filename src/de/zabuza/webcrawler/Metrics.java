package de.zabuza.webcrawler;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.zabuza.webcrawler.enums.SlotStatus;
import de.zabuza.webcrawler.enums.SlotType;
import de.zabuza.webcrawler.struct.EventData;
import de.zabuza.webcrawler.struct.EventList;
import de.zabuza.webcrawler.struct.SlotData;
import de.zabuza.webcrawler.struct.Slotlist;
import de.zabuza.webcrawler.util.MapUtil;

/**
 * Provides metrics for event lists.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 *
 */
public final class Metrics {

	/**
	 * Decimal format for decimal outputs of this class.
	 */
	private final DecimalFormat decFormat;
	/**
	 * Event list.
	 */
	private final EventList list;

	/**
	 * Creates a new metrics object with a given event list.
	 * 
	 * @param thatList
	 *            Event list for this metrics object
	 */
	public Metrics(final EventList thatList) {
		this.list = thatList;

		final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
		symbols.setDecimalSeparator('.');
		this.decFormat = new DecimalFormat("#.##");
		this.decFormat.setDecimalFormatSymbols(symbols);
	}

	/**
	 * Counts the amount of lead slot types a player, given as regex pattern,
	 * has.
	 * 
	 * @param playerRegex
	 *            Regex pattern that represents the player that lead slot types
	 *            should get counted, matching is case insensitive.
	 * @return Amount of lead slot types
	 */
	public int countLeadSlotsOf(final String playerRegex) {
		final Map<SlotType, Integer> slotMap = countSlotTypesOf(playerRegex);
		int leadAmount = 0;

		for (final Entry<SlotType, Integer> entry : slotMap.entrySet()) {
			final SlotType type = entry.getKey();
			if (type == SlotType.CO || type == SlotType.XO || type == SlotType.PL || type == SlotType.PSG
					|| type == SlotType.SL || type == SlotType.TL || type == SlotType.FTL || type == SlotType.TPL
					|| type == SlotType.TPS || type == SlotType.TC || type == SlotType.WCO) {
				leadAmount += entry.getValue().intValue();
			}
		}
		return leadAmount;
	}

	/**
	 * Counts the amount of slot types a player, given as regex pattern, has and
	 * returns a sorted map of it.
	 * 
	 * @param playerRegex
	 *            Regex pattern that represents the player that slot types
	 *            should get counted, matching is case insensitive.
	 * @return Sorted map of slot types with amount
	 */
	public Map<SlotType, Integer> countSlotTypesOf(final String playerRegex) {
		final Map<SlotType, Integer> slotMap = new HashMap<>();
		final Pattern pattern = Pattern.compile(playerRegex, Pattern.CASE_INSENSITIVE);
		Matcher matcher;

		for (final EventData datum : this.list) {
			final Slotlist slots = datum.getSlotlist();
			for (final SlotData slotData : slots.getAllSlots()) {
				matcher = pattern.matcher(slotData.getPlayer());
				if (matcher.find()) {
					final SlotType type = slotData.getSlotType();
					if (!slotMap.containsKey(type)) {
						slotMap.put(type, Integer.valueOf(0));
					}
					slotMap.put(type, Integer.valueOf(slotMap.get(type).intValue() + 1));
				}
			}
			for (final Entry<String, SlotStatus> slotData : slots.getAllReserve().entrySet()) {
				matcher = pattern.matcher(slotData.getKey());
				if (matcher.find()) {
					final SlotType type = SlotType.RESERVE;
					if (!slotMap.containsKey(type)) {
						slotMap.put(type, Integer.valueOf(0));
					}
					slotMap.put(type, Integer.valueOf(slotMap.get(type).intValue() + 1));
				}
			}
		}
		final Map<SlotType, Integer> sortedMap = MapUtil.sortByValue(slotMap);

		return sortedMap;
	}

	/**
	 * Computes the average amount of players that are absent, signed out late
	 * or signed out per event based on all passed events.
	 * 
	 * @param since
	 *            Date since the average should be computed or null if from
	 *            beginning
	 * @return Average amount of players that are absent, signed out late or
	 *         signed out per event
	 */
	public double getAvgAbsentOrLateSignoutOrSigneoutPlayers(final Calendar since) {
		return getAvgPlayerStatus(since, 2);
	}

	/**
	 * Computes the average amount of players that are absent or signed out late
	 * per event based on all passed events.
	 * 
	 * @param since
	 *            Date since the average should be computed or null if from
	 *            beginning
	 * @return Average amount of players that are absent or signed out late per
	 *         event
	 */
	public double getAvgAbsentOrLateSignoutPlayers(final Calendar since) {
		return getAvgPlayerStatus(since, 1);
	}

	/**
	 * Computes the average amount of players that are absent per event based on
	 * all passed events.
	 * 
	 * @param since
	 *            Date since the average should be computed or null if from
	 *            beginning
	 * @return Average amount of players that are absent per event
	 */
	public double getAvgAbsentPlayers(final Calendar since) {
		return getAvgPlayerStatus(since, 0);
	}

	/**
	 * Computes the average size of an event based on all passed events.
	 * 
	 * @param since
	 *            Date since the average should be computed or null if from
	 *            beginning
	 * @return Average size of an event
	 */
	public double getAvgEventSize(final Calendar since) {
		int totalSize = 0;
		int eventAmount = 0;
		for (final EventData event : this.list) {
			if (since != null && event.getDate().before(since)) {
				continue;
			}
			totalSize += event.getSize();
			eventAmount++;
		}
		final double avg = ((double) totalSize) / eventAmount;
		return Double.valueOf(this.decFormat.format(avg)).doubleValue();
	}

	/**
	 * Amount of events in this list.
	 * 
	 * @return amount of events in this list
	 */
	public int getEventAmount() {
		return this.list.getSize();
	}

	/**
	 * Gets a, by size, sorted map that represents all events.
	 * 
	 * @return Sorted map that represents all events
	 */
	public Map<EventData, Integer> getEventSizeRanking() {
		final Map<EventData, Integer> eventSizeRanking = new HashMap<>();

		for (final EventData datum : this.list) {
			eventSizeRanking.put(datum, Integer.valueOf(datum.getSize()));
		}
		return MapUtil.sortByValue(eventSizeRanking);
	}

	/**
	 * Gets a list of all events that the given creator designed.
	 * 
	 * @param creatorRegex
	 *            Creator of the events to get
	 * @return List of all events that the given creator designed
	 */
	public List<EventData> getEventsOf(final String creatorRegex) {
		final List<EventData> events = new LinkedList<>();
		final Pattern pattern = Pattern.compile(creatorRegex, Pattern.CASE_INSENSITIVE);
		Matcher matcher;

		for (final EventData datum : this.list) {
			matcher = pattern.matcher(datum.getCreator());
			if (matcher.find()) {
				events.add(datum);
			}
		}
		return events;
	}

	/**
	 * Gets a sorted map that represents players and the amount of events they
	 * participated in.
	 * 
	 * @return Sorted map that represents players and the amount of events they
	 *         participated in
	 */
	public Map<String, Integer> getEventsParticipatedRanking() {
		final Map<String, Integer> eventsParticipatedRanking = new HashMap<>();
		final Set<String> players = new HashSet<>();

		for (final EventData datum : this.list) {
			final Slotlist slots = datum.getSlotlist();
			for (final SlotData slotData : slots.getAllSlots()) {
				final String player = slotData.getPlayer();
				if (players.add(player)) {
					eventsParticipatedRanking.put(player,
							Integer.valueOf(getEventsWhereParticipated("(" + player + ")").size()));
				}
			}
		}
		return MapUtil.sortByValue(eventsParticipatedRanking);
	}

	/**
	 * Gets a list of all events where the given player participated in.
	 * 
	 * @param playerRegex
	 *            Participating player of the events to get
	 * @return List of all events where the given player participated in
	 */
	public List<EventData> getEventsWhereParticipated(final String playerRegex) {
		final List<EventData> events = new LinkedList<>();
		final Pattern pattern = Pattern.compile(playerRegex, Pattern.CASE_INSENSITIVE);
		Matcher matcher;

		for (final EventData datum : this.list) {
			final Slotlist slots = datum.getSlotlist();
			for (final SlotData slotData : slots.getAllSlots()) {
				matcher = pattern.matcher(slotData.getPlayer());
				if (matcher.find() && (slotData.getStatus() == SlotStatus.APPEARED
						|| slotData.getStatus() == SlotStatus.PREPARED_LATE
						|| slotData.getStatus() == SlotStatus.UNKNOWN)) {
					events.add(datum);
					break;
				}
			}
			for (final Entry<String, SlotStatus> slotData : slots.getAllReserve().entrySet()) {
				matcher = pattern.matcher(slotData.getKey());
				if (matcher.find() && (slotData.getValue() == SlotStatus.APPEARED
						|| slotData.getValue() == SlotStatus.PREPARED_LATE
						|| slotData.getValue() == SlotStatus.UNKNOWN)) {
					events.add(datum);
					break;
				}
			}
		}

		return events;
	}

	/**
	 * Gets a sorted map that represents players and the amount of lead slots
	 * they assigned to.
	 * 
	 * @return Sorted map that represents players and the amount of lead slots
	 *         they assigned to
	 */
	public Map<String, Integer> getLeadRanking() {
		final Map<String, Integer> leadRanking = new HashMap<>();
		final Set<String> players = new HashSet<>();

		for (final EventData datum : this.list) {
			final Slotlist slots = datum.getSlotlist();
			for (final SlotData slotData : slots.getAllSlots()) {
				final String player = slotData.getPlayer();
				if (players.add(player)) {
					leadRanking.put(player, Integer.valueOf(countLeadSlotsOf(player)));
				}
			}
		}
		return MapUtil.sortByValue(leadRanking);
	}

	/**
	 * Computes the average amount of players that are absent, signed out late
	 * or signed out (dependent on mode) per event based on all passed events.
	 * 
	 * @param since
	 *            Date since the average should be computed or null if from
	 *            beginning
	 * @param mode
	 *            0 for only absent players, 1 for absent and signed out late, 2
	 *            for absent, signed out late and signed out players
	 * @return Average amount of players that are absent, signed out late or
	 *         signed out (dependent on mode)
	 */
	private double getAvgPlayerStatus(final Calendar since, final int mode) {
		int amountOfPlayers = 0;
		int eventAmount = 0;
		for (final EventData event : this.list) {
			if (since != null && event.getDate().before(since)) {
				continue;
			}
			eventAmount++;
			final Slotlist slotlist = event.getSlotlist();
			for (final SlotData slotData : slotlist.getAllSlots()) {
				if (mode >= 0) {
					if (slotData.getStatus() == SlotStatus.ABSENT) {
						amountOfPlayers++;
					}
				}
				if (mode >= 1) {
					if (slotData.getStatus() == SlotStatus.SIGNED_OUT_LATE) {
						amountOfPlayers++;
					}
				}
				if (mode >= 2) {
					if (slotData.getStatus() == SlotStatus.SIGNED_OUT) {
						amountOfPlayers++;
					}
				}
			}
			for (final Entry<String, SlotStatus> entry : slotlist.getAllReserve().entrySet()) {
				if (mode >= 0) {
					if (entry.getValue() == SlotStatus.ABSENT) {
						amountOfPlayers++;
					}
				}
				if (mode >= 1) {
					if (entry.getValue() == SlotStatus.SIGNED_OUT_LATE) {
						amountOfPlayers++;
					}
				}
				if (mode >= 2) {
					if (entry.getValue() == SlotStatus.SIGNED_OUT) {
						amountOfPlayers++;
					}
				}
			}
		}
		final double avg = ((double) amountOfPlayers) / eventAmount;
		return Double.valueOf(this.decFormat.format(avg)).doubleValue();
	}
}
