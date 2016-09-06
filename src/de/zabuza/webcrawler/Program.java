package de.zabuza.webcrawler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.zabuza.webcrawler.enums.SlotType;
import de.zabuza.webcrawler.external.ExtEventData;
import de.zabuza.webcrawler.external.ExtPlayerData;
import de.zabuza.webcrawler.struct.EventData;
import de.zabuza.webcrawler.struct.EventList;
import de.zabuza.webcrawler.util.CrawlerUtil;

@SuppressWarnings("unused")
public final class Program {
	/**
	 * Path to the file that contains external data.
	 */
	private static final String FILEPATH_EXT_DATA = "res/DatenInput.csv";
	/**
	 * Path to the file that contains the output of the program.
	 */
	private static final String FILEPATH_OUTPUT = "res/output.txt";
	/**
	 * Path to the location where the event list gets serialized.
	 */
	private static final String FILEPATH_SERIALIZATION = "res/eventList.ser";
	
	/**
	 * Utility class. No implementation.
	 */
	private Program() {

	}
	
	/**
	 * Starts the crawler.
	 * @param args
	 *            Not supported
	 * @throws IOException If an I/O-Exception occurs
	 */
	public static void main(String[] args) throws IOException {
		/*
		 * If true the program will use the web-crawler to create
		 * a updated list. If false the program will use the
		 * serialized version of the list (assuming it exists).
		 * 
		 * ********************** CAUTION **********************
		 * Crawling, of course, causes a heavy amount of web-traffic.
		 * If you use this frequently the server can interpret this as DDos!
		 * This could lead to legal consequences for the executor
		 * of this program!
		 * So use "true" only to create and serialize a list,
		 * after that only use "false" until you need to
		 * update the list again.
		 * ********************** CAUTION **********************
		 */
		boolean updateList = false;
		
		Map<Calendar, ExtEventData> extEventData = Filecrawler.processExternalEventData(FILEPATH_EXT_DATA);
		Map<String, ExtPlayerData> extPlayerData = Filecrawler.processExternalPlayerData(FILEPATH_EXT_DATA);
		
		EventList list = null;
		if (updateList) {
			System.out.println("Crawling events...");
			list = Webcrawler.crawlWeb(extEventData, true);
			System.out.println("Crawling done.");
			
			System.out.println("Serializing event list...");
			CrawlerUtil.serialize(list, FILEPATH_SERIALIZATION);
			System.out.println("Serialization done.");
		} else {
			list = CrawlerUtil.deserialize(FILEPATH_SERIALIZATION);
		}
		
		
		System.out.println("Printing results...");
		BufferedWriter wr = new BufferedWriter(new FileWriter(FILEPATH_OUTPUT));
		wr.write(list.toString());
		wr.close();
		System.out.println("Printing done.");
		
		
		Metrics metrics = new Metrics(list);
		
		//Counting slot types
		String playerRegex = "Rallen";
		Map<SlotType, Integer> slotTypeCount = metrics.countSlotTypesOf(playerRegex);
		int eventAmount = 0;
		for (Entry<SlotType, Integer> entry : slotTypeCount.entrySet()) {
			eventAmount += entry.getValue();
			System.out.println(entry.getKey() + ":" + entry.getValue());
		}
		if (!slotTypeCount.isEmpty()) {
			System.out.println("EventAmount: " + eventAmount + " (of " + metrics.getEventAmount() + ")");
		}
		//Counting lead slot types
		int leadAmount = metrics.countLeadSlotsOf(playerRegex);
		if (leadAmount != 0) {
			System.out.println("Amount of lead slots: " + leadAmount);
		}
		
		/*
		//List events of player
		String participatingPlayerRegex = "Zabuza";
		List<EventData> eventsWhereParticipated = metrics.getEventsWhereParticipated(participatingPlayerRegex);
		for (EventData datum : eventsWhereParticipated) {
			System.out.println(datum.getName());
		}
		*/
		
		/*
		//Get Lead ranking
		Map<String, Integer> leadRanking = metrics.getLeadRanking();
		for (Entry<String, Integer> entry : leadRanking.entrySet()) {
			System.out.println(entry.getKey() + ":" + entry.getValue());
		}
		*/
		
		/*
		//Get Events participated ranking
		Map<String, Integer> eventsParticipatedRanking = metrics.getEventsParticipatedRanking();
		for (Entry<String, Integer> entry : eventsParticipatedRanking.entrySet()) {
			System.out.println(entry.getKey() + ":" + entry.getValue());
		}
		*/
		
		/*
		//Average event size
		int beforeMonths = -6;
		Calendar since = Calendar.getInstance();
		since.add(Calendar.MONTH, beforeMonths);
		System.out.println(metrics.getAvgEventSize(since) + " : Average event size since '"
				+ beforeMonths + " months'");
		//Average absent players
		since = Calendar.getInstance();
		since.add(Calendar.MONTH, beforeMonths);
		System.out.println(metrics.getAvgAbsentPlayers(since) + " : Average absent players since '"
				+ beforeMonths + " months'");
		//Average absent or late signed out players
		since = Calendar.getInstance();
		since.add(Calendar.MONTH, beforeMonths);
		System.out.println(metrics.getAvgAbsentOrLateSignoutPlayers(since)
				+ " : Average absent or signed out late players since '" + beforeMonths + " months'");
		//Average absent, late signed out or signed out players
		since = Calendar.getInstance();
		since.add(Calendar.MONTH, beforeMonths);
		System.out.println(metrics.getAvgAbsentOrLateSignoutOrSigneoutPlayers(since)
				+ " : Average absent, signed out late or signed out players since '"
				+ beforeMonths + " months'");
		*/
		
		/*
		//Get event size ranking
		Map<EventData, Integer> eventSizeRanking = metrics.getEventSizeRanking();
		for (Entry<EventData, Integer> entry : eventSizeRanking.entrySet()) {
			String eventText = CrawlerUtil.convertDateToString(entry.getKey().getDate())
					+ " : " + entry.getKey().getType() + " : " + entry.getKey().getName();
			System.out.println(entry.getValue() + " : " + eventText);
		}
		*/
		
		/*
		//Get events of creator
		String creatorRegex = "(Pfleger)";
		List<EventData> eventsOfCreator = metrics.getEventsOf(creatorRegex);
		for (EventData datum : eventsOfCreator) {
			System.out.println(datum.getName());
		}
		System.out.println("Event amount: " + eventsOfCreator.size());
		*/
		
		/*
		System.out.println("Exporting to database format...");
		list.exportDatabaseFormat();
		System.out.println("Exporting done.");
		
		
		System.out.println("Importing from database format...");
		EventList importedList = EventList.importDatabaseFormat();
		System.out.println("Importing done.");
		
		
		System.out.println("Exporting to extEventData...");
		Map<Calendar, ExtEventData> importedExtEventData = importedList.exportToExtEventDataMap();
		Filecrawler.exportToExternalFile(importedExtEventData, extPlayerData);
		System.out.println("Exporting done.");
		*/
		
	}
}