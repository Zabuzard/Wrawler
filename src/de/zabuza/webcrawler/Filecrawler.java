package de.zabuza.webcrawler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.zabuza.webcrawler.database.UserTableDb;
import de.zabuza.webcrawler.enums.EventType;
import de.zabuza.webcrawler.enums.SlotStatus;
import de.zabuza.webcrawler.external.ExtEventData;
import de.zabuza.webcrawler.external.ExtPlayerData;
import de.zabuza.webcrawler.util.CrawlerUtil;
import de.zabuza.webcrawler.util.InsensitiveComparator;

/**
 * Utility class for file crawling.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 *
 */
public final class Filecrawler {
	/**
	 * Path to the file that contains the output of the program.
	 */
	private static final String FILEPATH_OUTPUT = "D:\\Samus Aran\\Eigene Dateien\\Intimist\\Gruppe W\\Clanleitung\\Gästemanagement\\Webcrawler\\exportDatenInput.csv";
	/**
	 * Begin of external data header.
	 */
	private static final String HEADER_BEGIN = "Name;Reaktivierung;Registrierungsdatum;"
			+ "angeschrieben wegen Inaktivität";
	/**
	 * Notification symbol for inactivity in the external data.
	 */
	private static final String INACTIVITY_NOTIFICATION = "x";
	/**
	 * Offset from start to event data begin in external data.
	 */
	private static final int OFFSET_EVENT = 4;
	/**
	 * Offset from start to player data begin in external data.
	 */
	private static final int OFFSET_PLAYERS = 2;
	/**
	 * Symbol for an absent slot status in the external data.
	 */
	private static final String SLOT_STATUS_ABSENT = "n";
	/**
	 * Symbol for an appeared slot status in the external data.
	 */
	private static final String SLOT_STATUS_APPEARED = "a";
	/**
	 * Symbol for an late prepared slot status in the external data.
	 */
	private static final String SLOT_STATUS_PREPARED_TOO_LATE = "t";
	/**
	 * Symbol for an signed out slot status in the external data.
	 */
	private static final String SLOT_STATUS_SIGNED_OUT = "x";
	/**
	 * Symbol for an signed out too late slot status in the external data.
	 */
	private static final String SLOT_STATUS_SIGNED_OUT_TOO_LATE = "s";
	/**
	 * Value the external file split its data with.
	 */
	private static final String SPLIT_VALUE = ";";

	/**
	 * Creates a external csv file using the external data maps.
	 * 
	 * @param extEventMap
	 *            Map that contains external event data
	 * @param extPlayerMap
	 *            Map that contains external player data
	 */
	public static void exportToExternalFile(Map<Calendar, ExtEventData> extEventMap,
			Map<String, ExtPlayerData> extPlayerMap) {
		// Setup slot status data for every player and his events
		Map<String, Map<Calendar, SlotStatus>> playerEventData = new TreeMap<>(new InsensitiveComparator());
		for (Entry<Calendar, ExtEventData> entry : extEventMap.entrySet()) {
			for (String player : entry.getValue().getPlayers()) {
				// Skip player if he has no valid id
				if (UserTableDb.getInstance().getId(player).intValue() <= 0) {
					continue;
				}

				if (!playerEventData.containsKey(player)) {
					playerEventData.put(player, new TreeMap<Calendar, SlotStatus>());
				}
				playerEventData.get(player).put(entry.getKey(), entry.getValue().getPlayerStatus(player));
			}
		}

		// Build header and event lines
		List<String> result = new ArrayList<>(playerEventData.size() + OFFSET_PLAYERS);
		result.add(HEADER_BEGIN);
		StringBuilder secondLineBegin = new StringBuilder();
		for (int i = 0; i < OFFSET_EVENT - 1; i++) {
			secondLineBegin.append(SPLIT_VALUE);
		}
		result.add(secondLineBegin.toString());

		StringBuilder eventTypes = new StringBuilder();
		StringBuilder eventDates = new StringBuilder();
		for (Entry<Calendar, ExtEventData> entry : extEventMap.entrySet()) {
			eventTypes.append(SPLIT_VALUE).append(entry.getValue().getType());
			eventDates.append(SPLIT_VALUE).append(CrawlerUtil.convertDateToString(entry.getKey()));
		}
		eventTypes.append(SPLIT_VALUE);
		eventDates.append(SPLIT_VALUE);
		result.set(0, result.get(0) + eventTypes.toString());
		result.set(1, result.get(1) + eventDates.toString());

		// Build player lines
		List<Calendar> eventDateList = new ArrayList<>(extEventMap.keySet());
		Collections.sort(eventDateList);
		for (Entry<String, Map<Calendar, SlotStatus>> entry : playerEventData.entrySet()) {
			// Append pre event data values
			String player = entry.getKey();
			StringBuilder playerLine = new StringBuilder();
			// TODO Which player is W-Member? They need a '#'
			playerLine.append(player);
			if (!extPlayerMap.containsKey(player)) {
				// TODO How to catch new players & players that don't have
				// played yet?
				// System.err.println("Skipping for external data unknown
				// player: " + player);
				continue;
				// playerLine.append(SPLIT_VALUE).append(SPLIT_VALUE)
				// .append(SPLIT_VALUE);
			}

			ExtPlayerData extPlayerData = extPlayerMap.get(player);
			playerLine.append(SPLIT_VALUE);
			if (extPlayerData.getReactivationDate() != null) {
				playerLine.append(CrawlerUtil.convertDateToString(extPlayerData.getReactivationDate()));
			}
			playerLine.append(SPLIT_VALUE);
			if (extPlayerData.getRegistrationDate() != null) {
				playerLine.append(CrawlerUtil.convertDateToString(extPlayerData.getRegistrationDate()));
			}
			playerLine.append(SPLIT_VALUE);
			if (extPlayerData.isInactivityNotification()) {
				playerLine.append(INACTIVITY_NOTIFICATION);
			}
			// Append event data values
			int dateIndex = 0;
			for (Entry<Calendar, SlotStatus> eventEntry : entry.getValue().entrySet()) {
				Calendar dateToPrint = eventDateList.get(dateIndex);
				Calendar dateOfEntry = eventEntry.getKey();
				// Print events player has not participated on
				while (!dateToPrint.equals(dateOfEntry)) {
					playerLine.append(SPLIT_VALUE);
					dateIndex++;
					dateToPrint = eventDateList.get(dateIndex);
				}
				// Print event that entry stands for
				playerLine.append(SPLIT_VALUE);
				switch (eventEntry.getValue()) {
				case APPEARED:
					playerLine.append(SLOT_STATUS_APPEARED);
					break;
				case SIGNED_OUT:
					playerLine.append(SLOT_STATUS_SIGNED_OUT);
					break;
				case SIGNED_OUT_LATE:
					playerLine.append(SLOT_STATUS_SIGNED_OUT_TOO_LATE);
					break;
				case PREPARED_LATE:
					playerLine.append(SLOT_STATUS_PREPARED_TOO_LATE);
					break;
				case ABSENT:
					playerLine.append(SLOT_STATUS_ABSENT);
					break;
				case UNKNOWN:
					break;
				default:
					break;
				}

				dateIndex++;
			}
			// Print remaining events
			while (dateIndex < eventDateList.size()) {
				playerLine.append(SPLIT_VALUE);
				dateIndex++;
			}
			playerLine.append(SPLIT_VALUE);
			result.add(playerLine.toString());
		}

		// Save export
		try (final BufferedWriter wr = new BufferedWriter(new FileWriter(FILEPATH_OUTPUT))) {
			for (int i = 0; i < result.size(); i++) {
				wr.write(result.get(i));
				if (i < result.size() - 1) {
					wr.write("\n");
				}
			}
		} catch (IOException e) {
			System.err.println("Unknown error while saving external file export.");
			e.printStackTrace();
		}
	}

	/**
	 * Process external event data provided in file at given path.
	 * 
	 * @param path
	 *            Path to file with external data
	 * @return Map that contains all external event data
	 * @throws IOException
	 *             If an I/O-Exception occurs
	 */
	public static Map<Calendar, ExtEventData> processExternalEventData(String path) throws IOException {
		List<String> content = CrawlerUtil.getFileContent(path);
		// Create list with split content
		List<String[]> contentSplit = new ArrayList<>(content.size());
		for (int i = 0; i < content.size(); i++) {
			contentSplit.add(content.get(i).split(SPLIT_VALUE));
		}

		// Process header
		String[] typeHeader = contentSplit.get(0);
		String[] dateHeader = contentSplit.get(1);
		if (typeHeader.length != dateHeader.length) {
			System.err.println("External file error: Headers do not have the same length.");
		}

		Map<Calendar, ExtEventData> events = new TreeMap<>();
		// Process all events
		for (int i = OFFSET_EVENT; i < typeHeader.length; i++) {
			// Parse type
			String typeText = typeHeader[i].trim().toUpperCase();
			EventType type = EventType.NO_TYPE;
			if (typeText.equals("CO") || typeText.equals("COOP")) {
				type = EventType.COOP;
			} else if (typeText.equals("SPVP") || typeText.equals("PVP") || typeText.equals("TVT")) {
				type = EventType.TVT;
			} else if (typeText.equals("CO+") || typeText.equals("COOP+")) {
				type = EventType.COOP_PLUS;
			} else if (typeText.equals("BB")) {
				type = EventType.BLACKBOX;
			} else if (typeText.equals("ORG") || typeText.equals("ORGA")) {
				type = EventType.ORGA;
			} else if (typeText.equals("MIL") || typeText.equals("MILSIM")) {
				type = EventType.MILSIM;
			} else if (typeText.equals("COMP") || typeText.equals("COMPETITION")) {
				type = EventType.COMPETITION;
			} else {
				System.err.println("External file error: Unknown event type '" + typeText + "' in column " + i);
			}

			// Parse date
			String dateText = dateHeader[i].trim();
			Calendar date = CrawlerUtil.convertStringToDate(dateText);

			ExtEventData event = new ExtEventData(type, date);

			// Process player status
			for (int j = OFFSET_PLAYERS; j < content.size(); j++) {
				String[] playerLine = contentSplit.get(j);
				// Skip player if thats an empty line
				if (playerLine.length == 0) {
					continue;
				}
				String player = playerLine[0].trim();
				if (player.endsWith("#")) {
					player = player.substring(0, player.length() - 1);
				}

				// Skip player if he has no entry in this and following events
				if (playerLine.length < i + 1) {
					continue;
				}

				String slotStatusText = playerLine[i].trim();
				SlotStatus status = SlotStatus.UNKNOWN;
				if (!slotStatusText.equals("")) {
					if (slotStatusText.equals(SLOT_STATUS_APPEARED)) {
						status = SlotStatus.APPEARED;
					} else if (slotStatusText.equals(SLOT_STATUS_SIGNED_OUT)) {
						status = SlotStatus.SIGNED_OUT;
					} else if (slotStatusText.equals(SLOT_STATUS_SIGNED_OUT_TOO_LATE)) {
						status = SlotStatus.SIGNED_OUT_LATE;
					} else if (slotStatusText.equals(SLOT_STATUS_ABSENT)) {
						status = SlotStatus.ABSENT;
					} else if (slotStatusText.equals(SLOT_STATUS_PREPARED_TOO_LATE)) {
						status = SlotStatus.PREPARED_LATE;
					} else {
						System.err.println("External file error: Unknown slot status '" + slotStatusText
								+ "' in column:" + i + ",row:" + j);
					}
					event.addPlayer(player, status);
				}
			}

			events.put(date, event);
		}

		return events;
	}

	/**
	 * Process external player data provided in file at given path.
	 * 
	 * @param path
	 *            Path to file with external data
	 * @return Map that contains all external player data sorted by player
	 * @throws IOException
	 *             If an I/O-Exception occurs
	 */
	public static Map<String, ExtPlayerData> processExternalPlayerData(String path) throws IOException {
		List<String> content = CrawlerUtil.getFileContent(path);
		// Create list with split content
		List<String[]> contentSplit = new ArrayList<>(content.size());
		for (int i = 0; i < content.size(); i++) {
			contentSplit.add(content.get(i).split(SPLIT_VALUE));
		}
		Map<String, ExtPlayerData> playersData = new TreeMap<>(new InsensitiveComparator());

		// Process player data
		for (int i = OFFSET_PLAYERS; i < content.size(); i++) {
			String[] playerLine = contentSplit.get(i);
			// Skip player if thats an empty line
			if (playerLine.length == 0) {
				continue;
			}

			// Get player name
			String player = playerLine[0].trim();
			if (player.endsWith("#")) {
				player = player.substring(0, player.length() - 1);
			}

			// Get player reactivation date
			String reactivationDateText = playerLine[1].trim();
			Calendar reactivationDate = null;
			if (!reactivationDateText.equals("")) {
				reactivationDate = CrawlerUtil.convertStringToDate(reactivationDateText);
			}

			// Get player registration date
			String registrationDateText = playerLine[2].trim();
			Calendar registrationDate = null;
			if (!registrationDateText.equals("")) {
				registrationDate = CrawlerUtil.convertStringToDate(registrationDateText);
			} else {
				System.err.println("External file error: Unknown registration date for player '" + player + "'");
			}

			// Get player notification of inactivity status

			// Take care of index because field can be missing (then it and all
			// following lines where empty)
			String inactivityNotificationText = "";
			if (playerLine.length >= 4) {
				inactivityNotificationText = playerLine[3].trim();
			}

			boolean inactivityNotification = !inactivityNotificationText.equals("");

			ExtPlayerData playerDate = new ExtPlayerData(player, registrationDate, reactivationDate,
					inactivityNotification);

			playersData.put(player, playerDate);
		}

		return playersData;
	}

	/**
	 * Utility class. No implementation.
	 */
	private Filecrawler() {

	}
}
