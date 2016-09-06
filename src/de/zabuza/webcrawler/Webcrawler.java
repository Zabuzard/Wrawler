package de.zabuza.webcrawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.zabuza.webcrawler.enums.EventType;
import de.zabuza.webcrawler.enums.SlotStatus;
import de.zabuza.webcrawler.enums.SlotType;
import de.zabuza.webcrawler.external.ExtEventData;
import de.zabuza.webcrawler.struct.EventData;
import de.zabuza.webcrawler.struct.EventList;
import de.zabuza.webcrawler.struct.Slotlist;
import de.zabuza.webcrawler.util.CrawlerUtil;

/**
 * Utility class.
 * Provides a web crawler that searches some event
 * information of the GruppeW forum.
 * 
 * @author Zabuza
 */
public final class Webcrawler {
	/**
	 * Path to the server.
	 */
	private static final String SERVERPATH = "http://www.gruppe-w.de/forum/";
	/**
	 * Path to the event sub-forum.
	 */
	private static final String EVENTS_PATH = SERVERPATH + "viewforum.php?forum_id=4";
	/**
	 * Get variable for next event pages.
	 */
	private static final String EVENTS_PATH_SUFFIX = "&rowstart=";
	
	/**
	 * Mask where a new event starts in the forum.
	 */
	private static final String EVENTS_MASK_START = "<!--pre_forum-->";
	/**
	 * Mask where a new event ends in the forum.
	 */
	private static final String EVENTS_MASK_END = "<!--sub_forum_table-->";
	/**
	 * Mask which threads get rejected as event.
	 */
	private static final String EVENTS_REJECT_STICKY = "Thema gepinnt";
	/**
	 * Mask for a line that gets accepted as event.
	 */
	private static final String EVENTS_ACCEPT_THREAD = "<a href='viewthread.php?thread_id=";
	/**
	 * Tag that surrounds an events url.
	 */
	private static final String EVENTS_URL_TAG = "'";
	
	/**
	 * Mask where events title begins.
	 */
	private static final String THREAD_MASK_TITLE = "class='forum_thread_title'>";
	/**
	 * Mask that is directly before a title.
	 */
	private static final String THREAD_TITLE_START = "<strong>";
	/**
	 * Mask that is directly after a title.
	 */
	private static final String THREAD_TITLE_END = "</strong>";
	
	/**
	 * Mask where events creator begins.
	 */
	private static final String THREAD_CREATOR_START = "<!--forum_thread_user_name-->";
	/**
	 * Mask where events creator ends.
	 */
	private static final String THREAD_CREATOR_END = "</td>";
	/**
	 * Mask where events postId begins.
	 */
	private static final String THREAD_POSTID_START = "id='post_";
	/**
	 * Mask where events postId ends.
	 */
	private static final String THREAD_POSTID_END = "'>#1</a>";
	
	/**
	 * Mask where events content ends.
	 */
	private static final String THREAD_CONTENT_END = "<!--sub_forum_post_message-->";
	
	/**
	 * Constant for an unknown map.
	 */
	private static final String MAP_UNKNOWN = "Unknown";
	/**
	 * Constant for a map that gets rejected.
	 */
	private static final String MAP_REJECT = "JA";
	/**
	 * Constant for an unknown creator.
	 */
	private static final String CREATOR_UNKNOWN = "UNKNOWN";
	/**
	 * Constant for a creator that gets rejected.
	 */
	private static final String CREATOR_REJECT = "Anonymer Benutzer";
	/**
	 * Amount of lines that gets rejected after an event was found.
	 */
	private static final int EVENTS_REJECT_LINE_SIZE = 7;
	/**
	 * Amount of threads per page.
	 */
	private static final int EVENTS_THREAD_AMOUNT = 20;
	/**
	 * Line offset between threads title and creation date.
	 */
	private static final int THREAD_TITLE_OFFSET_DATE = 9;
	/**
	 * Line offset between threads title and the creator information.
	 */
	private static final int THREAD_TITLE_OFFSET_CREATOR = 5;
	/**
	 * Line offset between threads creator and post id information.
	 */
	private static final int THREAD_POSTID_OFFSET_CREATOR = 3;
	/**
	 * Prequel of the current year.
	 */
	private static final int DATE_YEAR_PRE = 20;
	/**
	 * First year of GruppeW.
	 */
	private static final int DATE_FIRST_YEAR = 2012;
	/**
	 * Constant for a non-valid event size.
	 */
	private static final int NO_SIZE = -1;
	/**
	 * Constant for a non-valid event thread id.
	 */
	private static final int NO_ID = -1;
	
	/**
	 * Utility class. No implementation.
	 */
	private Webcrawler() {
		
	}
	
	/**
	 * Crawls the web and catches information about all events of GruppeW.
	 * @param extEventData Event data from external files
	 * @param logging If logging information should be printed on the console
	 * @throws IOException If an I/O-Exception occurs
	 */
	public static EventList crawlWeb(Map<Calendar, ExtEventData> extEventData,
			boolean logging) throws IOException {
		List<String> events = getEventUrls(EVENTS_PATH);
		
		EventList data = new EventList(events.size());
		for (int i = 0; i < events.size(); i++) {
			EventData datum = crawlEvent(events.get(i), extEventData);
			if (datum != null) {
				data.add(datum);
			}
			if (logging && (i + 1) % 10 == 0) {
				System.out.println((i + 1) + " of " + events.size() + " events");
			}
		}
		
		return data;
	}
	
	/**
	 * Crawls an event given by its path and returns an information container.
	 * @param path Path to the event thread as url
	 * @param extEventData Event data from external files
	 * @return Information container that stores all event data
	 * @throws IOException If an I/O-Exception occurs
	 */
	private static EventData crawlEvent(String path,
			Map<Calendar, ExtEventData> extEventData) throws IOException {
		List<String> content = CrawlerUtil.getWebContent(path);

		//Get event title
		int i = -1;
		String line = "";
		do {
			i++;
			line = content.get(i);
		} while (!line.contains(THREAD_MASK_TITLE));
		int titleBegin = line.indexOf(THREAD_TITLE_START) + THREAD_TITLE_START.length();
		int titleEnd = line.indexOf(THREAD_TITLE_END);
		String title = line.substring(titleBegin, titleEnd);
		
		//Replace problematic chars
		title = title.replaceAll("Ã¤", "ä")
				.replaceAll("Ã¼", "ü")
				.replaceAll("&#39;", "'")
				.replaceAll("Ã¶", "ö")
				.replaceAll("&quot;", "\"")
				.replaceAll("ÃŸ", "ß")
				.replaceAll("Ãœ", "Ü");
		
		//Threads that are no events
		if (title.trim().contains("Vorankündigung - Time is running V3")
				|| title.trim().contains("[Alter Thread] WR2")
				|| title.trim().contains("WICHTIG: Planung Samstag, Winter Rush2")
				|| title.trim().contains("[Co30+] Die letzte Bastion Russlands - verschoben")
				|| title.trim().contains("Vorankündigung 26.04.13 - Operation Seelandung")
				|| title.trim().contains("20.07 Massentest Desert Storm")
				|| title.trim().contains("[16.06.2013 | 20:00] - Show of Force- MCC")) {
			return null;
		}
		
		//Get event date
		Calendar date = getEventDate(title, content, i);
		
		//Get event creator
		i += THREAD_TITLE_OFFSET_CREATOR;
		line = content.get(i);
		int creatorBegin = line.indexOf(THREAD_CREATOR_START) + THREAD_CREATOR_START.length();
		int creatorEnd = line.indexOf(THREAD_CREATOR_END);
		String creator = line.substring(creatorBegin, creatorEnd);
		if (creator.contains(CREATOR_REJECT)) {
			creator = CREATOR_UNKNOWN;
		}
		
		//Get events opening post id
		line = content.get(i + THREAD_POSTID_OFFSET_CREATOR);
		int postIdBegin = line.indexOf(THREAD_POSTID_START) + THREAD_POSTID_START.length();
		int postIdEnd = line.indexOf(THREAD_POSTID_END);
		int postId = Integer.parseInt(line.substring(postIdBegin, postIdEnd));
		
		//Get event type
		EventType type = getEventType(title);
		//Get event size
		int size = getEventSize(title);
		//Get thread id
		int threadId = getThreadId(path);
		//Get event date
		Calendar time = getEventTime(content, i, title);
		//Get thread map
		String map = getThreadMap(content, i);
		//Get event name
		String name = getThreadName(title);
		
		Slotlist slotlist = createSlotlist(size, content, i, title,
				date, type, extEventData);
		
		return new EventData(name, type, size, creator, map, date, time, threadId, postId, slotlist);
	}
	/**
	 * Creates a slot-list of the event by
	 * extracting it from the event thread web content.
	 * @param size Starting capacity of the slot-list
	 * @param content Content of the event threads web site
	 * @param curContentIndex Current index in the content which
	 * should be placed near starting of the true content
	 * @param title Title of the event
	 * @param date Date of the event
	 * @param type Type of the event
	 * @param extEventData Event data from external files
	 * @return Slot-list of the event or null or an empty list if failure occurred
	 */
	private static Slotlist createSlotlist(int size, List<String> content,
			int curContentIndex, String title, Calendar date, EventType type,
			Map<Calendar, ExtEventData> extEventData) {
		Slotlist slotlist = null;
		int i = curContentIndex;
		
		String line = "";
		Pattern pattern;
		Matcher matcher;
		boolean listStartFound = false;
		boolean slotFound = false;
		
		ExtEventData extEventDate = extEventData.get(date);
		Set<String> extEventPlayers = null;
		if (extEventDate != null) {
			extEventPlayers = extEventDate.getPlayers();
		}
		
		final String slot_pattern = "[A-Za-züäöÜÄÖ\\s\\+ß\\-\\(\\)/\\.0-9\\?,\\*]+";
		final String player_pattern = "[A-Za-züäöÜÄÖ\\sß\\-_0-9\\?\\.:]+";
		
		do {
			i++;
			slotFound = false;
			line = content.get(i);
			
			//Replace problematic chars
			line = line.replaceAll("Ã¤", "ä")
					.replaceAll("Ã¼", "ü")
					.replaceAll("&#39;", "'")
					.replaceAll("Ã¶", "ö")
					.replaceAll("&quot;", "\"")
					.replaceAll("ÃŸ", "ß")
					.replaceAll("Ãœ", "Ü")
					.replaceAll("â€“", "-")
					.replaceAll("Ã„", "Ä")
					.replaceAll("Ã§", "c")
					.replaceAll("Ã¢", "a");
			
			//If list start was found search for slots
			if (listStartFound) {
				if (slotlist == null) {
					slotlist = new Slotlist(size);
				}
				
				String keyText = "";
				String slotText = "";
				String player = "";
				
				pattern = Pattern.compile("#([\\d]+)[\\s]{1,3}-[\\s]{1,3}(<span style='color:#[a-fA-F0-9]{6}'>)?(" + slot_pattern
						+ ")(</span>)?[\\s]?(</strong></span>|</span></strong>)?[\\s]{0,3}-"
						+ "[\\s]{0,3}<strong><span style='color:#[a-fA-F0-9]{6}'>(<strong>)?[\\[]?W[\\]]?[\\s]?</span>"
						+ "[\\s]?(</strong>[\\s]?<strong>)?(" + player_pattern + ")</strong>[\\s]*( - nicht bestätigt)?<br[\\s]?/>",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					slotFound = true;
					keyText = matcher.group(1);
					slotText = matcher.group(3);
					player = matcher.group(8);
				}
				if (!slotFound) {
					pattern = Pattern.compile("#([\\d]+)[\\s]{1,3}-[\\s]{1,3}(<span style='color:#[a-fA-F0-9]{6}'>)?(" + slot_pattern
							+ ")(</span>)?[\\s]?(</strong></span>|</span></strong>)?[\\s]{0,3}-"
							+ "[\\s]{0,3}<strong>[\\s]?(" + player_pattern + ")</strong>[\\s]*( - nicht bestätigt)?<br[\\s]?/>",
							Pattern.CASE_INSENSITIVE);
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						slotFound = true;
						keyText = matcher.group(1);
						slotText = matcher.group(3);
						player = matcher.group(6);
					}
				}
				if (!slotFound) {
					pattern = Pattern.compile("#([\\d]+)[\\s]{1,3}(<span style='color:#[a-fA-F0-9]{6}'>)?(" + slot_pattern
							+ ")(</span>)?[\\s]?(</strong></span>|</span></strong>)?[\\s]{0,3}-"
							+ "[\\s]{0,3}<strong><span style='color:#[a-fA-F0-9]{6}'>(<strong>)?[\\[]?W[\\]]?[\\s]?</span>"
							+ "[\\s]?(</strong>[\\s]?<strong>)?(" + player_pattern + ")</strong>[\\s]*( - nicht bestätigt)?<br[\\s]?/>",
							Pattern.CASE_INSENSITIVE);
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						slotFound = true;
						keyText = matcher.group(1);
						slotText = matcher.group(3);
						player = matcher.group(8);
					}
				}
				if (!slotFound) {
					pattern = Pattern.compile("#([\\d]+)[\\s]{1,3}(<span style='color:#[a-fA-F0-9]{6}'>)?(" + slot_pattern
							+ ")(</span>)?[\\s]?(</strong></span>|</span></strong>)?[\\s]{0,3}-"
							+ "[\\s]{0,3}<strong>[\\s]?(" + player_pattern + ")</strong>[\\s]*( - nicht bestätigt)?<br[\\s]?/>",
							Pattern.CASE_INSENSITIVE);
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						slotFound = true;
						keyText = matcher.group(1);
						slotText = matcher.group(3);
						player = matcher.group(6);
					}
				}
				if (!slotFound) {
					pattern = Pattern.compile("#([\\d]+)[\\s]{1,3}(<span style='color:#[a-fA-F0-9]{6}'>)?(" + slot_pattern
							+ ")(</span>)?[\\s]?(</strong></span>|</span></strong>)?[\\s]{0,3}[\\-]?"
							+ "[\\s]{0,3}<strong>[\\s]?<span style='color:#[a-fA-F0-9]{6}'>(<strong>)?[\\[]?W[\\]]?[\\s]?</span>"
							+ "[\\s]?(</strong>[\\s]?<strong>)?(" + player_pattern + ")</strong>[\\s]*( - nicht bestätigt)?<br[\\s]?/>",
							Pattern.CASE_INSENSITIVE);
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						slotFound = true;
						keyText = matcher.group(1);
						slotText = matcher.group(3);
						player = matcher.group(8);
					}
				}
				if (!slotFound) {
					pattern = Pattern.compile("#([\\d]+)[\\s]{1,3}(<span style='color:#[a-fA-F0-9]{6}'>)?(" + slot_pattern
							+ ")(</span>)?[\\s]?(</strong></span>|</span></strong>)?[\\s]{0,3}[\\-]?"
							+ "[\\s]{0,3}<strong>[\\s]?(" + player_pattern + ")</strong>[\\s]*( - nicht bestätigt)?<br[\\s]?/>",
							Pattern.CASE_INSENSITIVE);
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						slotFound = true;
						keyText = matcher.group(1);
						slotText = matcher.group(3);
						player = matcher.group(6);
					}
				}
				if (!slotFound) {
					pattern = Pattern.compile("#([\\d]+)[\\s]{1,3}-[\\s]{1,3}(<span style='color:#[a-fA-F0-9]{6}'>)?(" + slot_pattern
							+ ")(</span>)?[\\s]?(</strong></span>|</span></strong>)?[\\s]{0,3}[\\-]?"
							+ "[\\s]{0,3}<strong>[\\s]?(" + player_pattern + ")(</strong>)?[\\s]*( - nicht bestätigt)?<br[\\s]?/>",
							Pattern.CASE_INSENSITIVE);
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						slotFound = true;
						keyText = matcher.group(1);
						slotText = matcher.group(3);
						player = matcher.group(6);
					}
				}
				if (!slotFound) {
					pattern = Pattern.compile("#([\\d]+)[\\s]{1,3}(<span style='color:#[a-fA-F0-9]{6}'>)?(" + slot_pattern
							+ ")(</span>)?[\\s]?(</strong></span>|</span></strong>)?[\\s]{0,3}[\\-]?"
							+ "[\\s]{0,3}<strong>[\\s]?[\\[]?<span style='color:#[a-fA-F0-9]{6}'>(<strong>)?[\\[]?W[\\]]?</span>[\\]]?"
							+ "[\\s]?(</strong>[\\s]?<strong>)?(" + player_pattern + ")(</strong>)?[\\s]*( - nicht bestätigt)?<br[\\s]?/>",
							Pattern.CASE_INSENSITIVE);
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						slotFound = true;
						keyText = matcher.group(1);
						slotText = matcher.group(3);
						player = matcher.group(8);
					}
				}
				if (!slotFound) {
					pattern = Pattern.compile("#([\\d]+)[\\s]{1,3}(<span style='color:#[a-fA-F0-9]{6}'>)?(" + slot_pattern
							+ ")(</span>)?[\\s]?(</strong></span>|</span></strong>)?[\\s]{0,3}[\\-]?"
							+ "[\\s]{0,3}<strong>[\\s]?(" + player_pattern + ")(</strong>)?[\\s]*( - nicht bestätigt)?<br[\\s]?/>",
							Pattern.CASE_INSENSITIVE);
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						slotFound = true;
						keyText = matcher.group(1);
						slotText = matcher.group(3);
						player = matcher.group(6);
					}
				}
				if (!slotFound) {
					pattern = Pattern.compile("#([\\d]+)[\\s]{0,3}-[\\s]{1,3}(<span style='color:#[a-fA-F0-9]{6}'>)?(" + slot_pattern
							+ ")(</span>)?[\\s]?(</strong></span>|</span></strong>)?[\\s]{0,3}[\\-]?"
							+ "[\\s]{0,3}<span style='color:#[a-fA-F0-9]{6}'>(<strong>)?[\\[]?W[\\]]?[\\s]?</span>"
							+ "[\\s]?(</strong>[\\s]?<strong>)?(<strong>)?(" + player_pattern + ")(</strong>)?[\\s]*( - nicht bestätigt)?<br[\\s]?/>",
							Pattern.CASE_INSENSITIVE);
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						slotFound = true;
						keyText = matcher.group(1);
						slotText = matcher.group(3);
						player = matcher.group(9);
					}
				}
				if (!slotFound) {
					pattern = Pattern.compile("(<span style='color:#[a-fA-F0-9]{6}'>)?#([\\d]+)[\\s]{0,3}[\\-]?[\\s]{1,3}(" + slot_pattern
							+ ")(</span>)?[\\s]?(</strong></span>|</span></strong>)?[\\s]{0,3}[\\-]?"
							+ "[\\s]{0,3}<span style='color:#[a-fA-F0-9]{6}'>(<strong>)?[\\[]?W[\\]]?[\\s]?</span>"
							+ "[\\s]?(</strong>[\\s]?<strong>)?(<strong>)?(" + player_pattern + ")(</strong>)?[\\s]*( - nicht bestätigt)?<br[\\s]?/>",
							Pattern.CASE_INSENSITIVE);
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						slotFound = true;
						keyText = matcher.group(2);
						slotText = matcher.group(3);
						player = matcher.group(9);
					}
				}
				
				//Parse slot results
				if (slotFound) {
					keyText = keyText.trim();
					slotText = slotText.trim();
					player = player.trim();
					if (slotText.startsWith("-")) {
						slotText = slotText.substring(1);
						if (slotText.endsWith("-")) {
							slotText = slotText.substring(0, slotText.length() - 2);
						}
						slotText = slotText.trim();
					}
					if (player.startsWith("-")) {
						player = player.substring(1);
						if (player.endsWith("-")) {
							player = player.substring(0, player.length() - 2);
						}
						player = player.trim();
					}
					
					SlotType slot = parseSlotType(slotText);
					
					//Handle player exceptions
					if (player.equals("RaXus")) {
						player = "RaXuS";
					} else if (player.equals("Raxus")) {
						player = "RaXuS";
					} else if (player.equals("Fett Li")) {
						player = "Fett_Li";
					} else if (player.equals("Cleverle")) {
						player = "DasCleverle";
					} else if (player.equals("halo 75michael")) {
						player = "halo75michael";
					} else if (player.equals("Omniman")) {
						player = "OmniMan";
					} else if (player.equals("PhiGhol")) {
						player = "PhiGohl";
					} else if (player.equals("Rallen95")) {
						player = "Rallen";
					} else if (player.equals("HeartbreakeOne")) {
						player = "HeartbreakOne";
					} else if (player.equals("Dave Johnson")) {
						player = "Dave_Johnson";
					} else if (player.equals("General Axel")) {
						player = "Axel";
					} else if (player.equals("Viruz")) {
						player = "ViruZ";
					} else if (player.equals("LeCiero")) {
						player = "LeCicero";
					} else if (player.equals("Pappkamerad")) {
						player = "Pappkamerrad";
					} else if (player.equals("Waidman")) {
						player = "Waidmann";
					} else if (player.equals("Berliner")) {
						player = "Berliner19";
					} else if (player.equals("SimonPower")) {
						player = "Simon Power";
					} else if (player.equals("H0riZ0n")) {
						player = "H0RiZ0N";
					} else if (player.equals("H0RiZ0n")) {
						player = "H0RiZ0N";
					} else if (player.equals("Cerbatron")) {
						player = "cerbatron";
					} else if (player.equals("nobody")) {
						player = "Nobody";
					} else if (player.equals("Pyriel")) {
						player = "Pyraiel";
					} else if (player.equals("Nisaburo")) {
						player = "Ninsaburo";
					} else if (player.equals("Ahu")) {
						player = "ahu";
					} else if (player.equals("Aclatraz1")) {
						player = "Alcatraz1";
					} else if (player.equals("Fussel")) {
						player = "Fussel_de";
					} else if (player.equals("Phigohl")) {
						player = "PhiGohl";
					} else if (player.equals("Toko")) {
						player = "Toko1993";
					} else if (player.equals("FF_Oneil")) {
						player = "FF_Oneill";
					} else if (player.equals("Bountyhunta")) {
						player = "BountyHuntA";
					} else if (player.equals("Freaky")) {
						player = "Freacky";
					} else if (player.equals("Evilspam")) {
						player = "EvilSpam";
					} else if (player.equals("Ollum")) {
						player = "OLLUM";
					} else if (player.equals("Price")) {
						player = "Prince";
					} else if (player.equals("HeartbrakeOne")) {
						player = "HeartbreakOne";
					} else if (player.equals("Boone")) {
						player = "Booone";
					} else if (player.equals("Goike")) {
						player = "Goicke";
					} else if (player.equals("Stupus Lupus")) {
						player = "Stubus Lupus";
					} else if (player.equals("ZiniusYoko")) {
						player = "ZinuisYoko";
					} else if (player.equals("Flloyd")) {
						player = "Floyd";
					} else if (player.equals("PhiGolh")) {
						player = "PhiGohl";
					} else if (player.equals("Walter")) {
						player = "Wallter";
					} else if (player.equals("janus")) {
						player = "Janus";
					} else if (player.equals("Tobifiction")) {
						player = "Tobi";
					} else if (player.equals("EvilSPAM")) {
						player = "EvilSpam";
					} else if (player.equals("Jethro_Gibbs")) {
						player = "JethroGibbs";
					} else if (player.equals("Ulfberht")) {
						player = "Ulfberth";
					} else if (player.equals("Stubus_Lupus")) {
						player = "Stubus Lupus";
					} else if (player.equals("Odium")) {
						player = "JeremiahRose";
					} else if (player.equals("MajorVoku")) {
						player = "Voku";
					} else if (player.equals("DaveJohnson")) {
						player = "Dave_Johnson";
					} else if (player.equals("StgGoicke")) {
						player = "Goicke";
					} else if (player.equals("Dave_Jonsen")) {
						player = "Dave_Johnson";
					} else if (player.equals("SgtGoicke")) {
						player = "Goicke";
					} else if (player.equals("Major Voku")) {
						player = "Voku";
					} else if (player.equals("Resses")) {
						player = "Reeses";
					} else if (player.equals("KrigerBusch")) {
						player = "KriegerBusch";
					} else if (player.equals("Dave Johnsen")) {
						player = "Dave_Johnson";
					} else if (player.equals("fussel_de")) {
						player = "Fussel_de";
					} else if (player.equals("Alpha Mike")) {
						player = "AlphaMike";
					} else if (player.equals("Zabusa")) {
						player = "Zabuza";
					} else if (player.equals("maximax")) {
						player = "Maximax";
					} else if (player.equals("FettLi")) {
						player = "Fett_Li";
					} else if (player.equals("Fett-Li")) {
						player = "Fett_Li";
					} else if (player.equals("Zambusa")) {
						player = "Zabuza";
					} else if (player.equals("Lucky Luke")) {
						player = "LuckyLuke";
					} else if (player.equals("Wihskey")) {
						player = "Whiskey";
					} else if (player.equals("Fett_li")) {
						player = "Fett_Li";
					} else if (player.equals("Berliner 19")) {
						player = "Berliner19";
					} else if (player.equals("Max10")) {
						player = "Max-10";
					} else if (player.equals("Bunkferfaust")) {
						player = "Bunkerfaust";
					} else if (player.equals("Jan.")) {
						player = "Jan";
					} else if (player.equals("Nemesis")) {
						player = "NemesisoD";
					} else if (player.equals("Steffieth")) {
						player = "Steffie";
					} else if (player.equals("Dura")) {
						player = "Dura_Ger";
					} else if (player.equals("Shadowki")) {
						player = "Shadow";
					} else if (player.equals("Kaiser")) {
						player = "K4ISER";
					} else if (player.equals("Justice")) {
						player = "Justice92";
					} else if (player.equals("Lee")) {
						player = "Lee1337";
					} else if (player.equals("Paul")) {
						player = "Paul G";
					} else if (player.equals("Paul G.")) {
						player = "Paul G";
					} else if (player.equals("David")) {
						player = "David_1";
					} else if (player.equals(".:NemesisoD:.")) {
						player = "NemesisoD";
					} else if (player.equals("tobi28")) {
						player = "Tobi";
					} else if (player.equals("Smudoo")) {
						player = "Smudooo";
					} else if (player.equals("MrP")) {
						player = "MrPink";
					} else if (player.equals("GNRLJONSON")) {
						player = "GNRLJONSEN";
					} else if (player.equals("Steacky")) {
						player = "Steaky";
					} else if (player.equals("GNRL.JONSEN")) {
						player = "GNRLJONSEN";
					} else if (player.equals("mav993")) {
						player = "mav933";
					} else if (player.equals("Assy")) {
						player = "Assystolie";
					} else if (player.equals("Mettelus")) {
						player = "Metellus";
					} else if (player.equals("Steffi")) {
						player = "Steffie";
					} else if (player.equals("Dave")) {
						player = "Dave_Johnson";
					} else if (player.equals("Ulfberth")) {
						player = "Ulfberht";
					} else if (player.equals("DIRT")) {
						player = "H0RiZ0N";
					} else if (player.equals("GNRL.JONSON")) {
						player = "GNRLJONSEN";
					} else if (player.equals("AlexanderKnight")) {
						player = "Alexander";
					} else if (player.equals("Alexander Knight")) {
						player = "Alexander";
					} else if (player.equals("Ice_1")) {
						player = "Ice";
					} else if (player.equals("Berserker")) {
						player = "BERSERKER";
					} else if (player.equals("Jeremiahrose")) {
						player = "JeremiahRose";
					} else if (player.equals("H0Riz0N")) {
						player = "H0RiZ0N";
					} else if (player.equals("Narkoma")) {
						player = "Nakroma";
					} else if (player.equals("Horizon")) {
						player = "H0RiZ0N";
					} else if (player.equals("Lützower Jäger")) {
						player = "LuetzowerJaeger";
					} else if (player.equals("Der Fuchs")) {
						player = "DerFuchs";
					} else if (player.equals("William Temeraire")) {
						player = "Wiliam Temeraire";
					} else if (player.equals("Caldin")) {
						player = "caldin";
					}
					
					int key = Integer.parseInt(keyText);
					
					//Extract slot status from external data
					SlotStatus status = SlotStatus.UNKNOWN;
					if (extEventDate != null) {
						EventType extType = extEventDate.getType();
						if (extType == type) {
							SlotStatus extStatus = extEventDate.getPlayerStatus(player);
							if (extStatus != null) {
								status = extStatus;
							//Handle banned players
							} else if (!player.equals("element_WSC")
									&& !player.equals("GNRLJONSEN")
									&& !player.equals("GNRL.JONSON")
									&& !player.equals("GNRL.JONSEN")
									&& !player.equals("Kyrko")
									&& !player.equals("Gecko")
									&& !player.equals("Mobiusune")
									&& !player.equals("Proof")
									&& !player.equals("Strelok")
									&& !player.equals("Conan")
									&& !player.equals("Marinus")
									&& !player.equals("PrivateYoung")
									&& !player.equals("WickerMan")
									&& !player.equals("Irish")
									&& !player.equals("Ninsaburo")
									&& !player.equals("Browser")
									&& !player.equals("DeejayPro")
									&& !player.equals("Schmusebaerchi")
									&& !player.equals("ViruZ")
									&& !player.equals("Haicon")
									&& !player.equals("Evan")
									&& !player.equals("BlackHawkin")
									&& !player.equals("Daft")
									&& !player.equals("Pokertime")
									&& !player.equals("Tom")
									&& !player.equals("DIRT")
									&& !player.equals("Huddlestone")
									&& !player.equals("FredyOne")
									&& !player.equals("ToxiqVipeZ")
									&& !player.equals("Gamer")
									&& !player.equals("FredyOne")
									&& !player.equals("AdmiralMayo")
									&& !player.equals("KriegerBusch")
									&& !player.equals("Dice")
									&& !player.equals("Helljumper")
									&& !player.equals("Ceezed")
									&& !player.equals("Huni")
									&& !player.equals("Ulfberth")
									&& !player.equals("Xaro")
									&& !player.equals("Franz")
									&& !player.equals("Dice")
									&& !player.equals("Tinte")
									&& !player.equals("Valdo")
									&& !player.equals("Recold")
									&& !player.equals("Restless")
									&& !player.equals("Teufel")
									&& !player.equals("Xanthiphist")
									&& !player.equals("raunkjar")
									&& !player.equals("Waveback")
									&& !player.equals("LingLing")
									&& !player.equals("Timophy")
									&& !player.equals("Silva")
									&& !player.equals("Lukasio")
									&& !player.equals("Doc")
									&& !player.equals("Gather")
									&& !player.equals("Dave")
									&& !player.equals("Zyprus")
									&& !player.equals("CeeZed")
									&& !player.equals("Ratha")
									&& !player.equals("Varg")
									&& !player.equals("Vendetta")
									&& !player.equals("BadWolf")
									&& !player.equals("FoxXy")
									&& !player.equals("Blubber")
									&& !player.equals("Thyke")
									&& !player.equals("mobiusune")
									&& !player.equals("Steffi")
									&& !player.equals("Hex")
									&& !player.equals("Asystolie")
									&& !player.equals("Amii")
									&& !player.equals("rugerrell")
									&& !player.equals("Huni")
									&& !player.equals("Smudooo")
									&& !player.equals("Staynex")
									&& !player.equals("TheNiki")
									&& !player.equals("Miho")
									&& !player.equals("Badwolf")
									&& !player.equals("Hilker")
									&& !player.equals("Metellus")
									&& !player.equals("Imperator333")
									&& !player.equals("Infinity")
									&& !player.equals("Ch3yTac")
									&& !player.equals("Suchhund")
									&& !player.equals("infinity")
									&& !player.equals("zebedeus")
									&& !player.equals("Steffie")
									&& !player.equals("harry")
									&& !player.equals("Mettelus")
									&& !player.equals("Smudoo")
									&& !player.equals("Shugard")
									&& !player.equals("Gunni")
									&& !player.equals("Grantelbart")
									&& !player.equals("Gorwin")
									&& !player.equals("Eric")
									&& !player.equals("Jan")
									&& !player.equals("Valin")
									&& !player.equals("Assy")
									&& !player.equals("Justice92")
									&& !player.equals("superkekx")
									&& !player.equals("Alexxd_12")
									&& !player.equals("Fairborn")
									&& !player.equals("Guenni")
									&& !player.equals("Hathor")
									&& !player.equals("Mungo")
									&& !player.equals("Pr3volution")
									&& !player.equals("Hendrik")
									&& !player.equals("matze3331")
									&& !player.equals("Currie")
									&& !player.equals("Rice")
									&& !player.equals("Steff")
									&& !player.equals("Bronko40")
									&& !player.equals("mav993")
									&& !player.equals("Orthac")
									&& !player.equals("Blackii93")
									&& !player.equals("misterio234")
									&& !player.equals("LiquidBlaze")
									&& !player.equals("Plasma")
									&& !player.equals("tofl")
									&& !player.equals("Hansen")
									&& !player.equals("K-One")
									&& !player.equals("Steaky")
									&& !player.equals("KwieKevin")
									&& !player.equals("MrCrazyAndreas")
									&& !player.equals("16kb")
									&& !player.equals("Minimix")
									&& !player.equals("Michi")
									&& !player.equals("Trampeltier")
									&& !player.equals("KwieKevin")
									&& !player.equals("rocko")
									&& !player.equals("Bixby")
									&& !player.equals("KrisSnyper")
									&& !player.equals("IGEL")
									&& !player.equals("mav933")
									&& !player.equals("Cypher")
									&& !player.equals("MaxFTWi")
									&& !player.equals("Cypher")
									&& !player.equals("isku")
									&& !player.equals("ZeroTwoFourty")
									&& !player.equals("Silberwolf2k")
									&& !player.equals("Albino")
									&& !player.equals("Lester")
									&& !player.equals("BlackRabbit")
									&& !player.equals("Jenkins")
									&& !player.equals("GNRLJONSON")
									&& !player.equals("Berliner19")
									&& !player.equals("Steacky")
									&& !player.equals("Henker")
									&& !player.equals("Mohrpheus")
									&& !player.equals("maruk")
									&& !player.equals("DorsalRegent")
									&& !player.equals("Hartman")
									&& !player.equals("Zorgan")
									&& !player.equals("Opus Cincinnati")
									&& !player.equals("TheNapGamer")
									&& !player.equals("David")
									&& !player.equals("Julius")
									&& !player.equals("zelkin")
									&& !player.equals("Pit")
									&& !player.equals("Roschach")
									&& !player.equals("allter")
									&& !player.equals("jayjay")
									&& !player.equals("DEman")
									&& !player.equals("Meekman240")
									&& !player.equals("TheNapGamer")
									&& !player.equals("zinki")
									&& !player.equals("Jackal")
									&& !player.equals("Whity")
									&& !player.equals("Slinger")
									&& !player.equals("svenson")
									&& !player.equals("MobilePimp")
									&& !player.equals("Freak")
									&& !player.equals("sion")
									&& !player.equals("znoop")
									&& !player.equals("Brainslush")
									&& !player.equals("maruk")
									&& !player.equals("Roody")
									&& !player.equals("Midi")
									&& !player.equals("SPUTNIK")
									&& !player.equals("The_Kecki")
									&& !player.equals("PsychoAce")
									&& !player.equals("Ryuichiro")
									&& !player.equals("BadGuy")
									&& !player.equals("Paul G")
									&& !player.equals("brainslush")
									&& !player.equals("Bowman")
									&& !player.equals("Cigar0")
									&& !player.equals("NemesisoD")
									&& !player.equals("Jazz")
									&& !player.equals("Znooptokkie")
									&& !player.equals("Sambucus")
									&& !player.equals("Jolly Roger")
									&& !player.equals("Celle")
									&& !player.equals("Scharkk")
									&& !player.equals("noviias")
									&& !player.equals("JokerRetry")
									&& !player.equals("Sagamir")
									&& !player.equals("Rocksberg")
									&& !player.equals("Offi")
									&& !player.equals("Znooptokkie")
									&& !player.equals("Waschbier")
									&& !player.equals("HupDrop")
									&& !player.equals("Lee1337")
									&& !player.equals("CeLLe")
									&& !player.equals("Dura_Ger")
									&& !player.equals("Uber")
									&& !player.equals("Chris92")
									&& !player.equals("Jacx")
									&& !player.equals("Mango")
									&& !player.equals("RedHeadAdventure")
									&& !player.equals("Blue-Ice")
									&& !player.equals("Para")
									&& !player.equals("Shadow")
									&& !player.equals("NBRC_FOX")
									&& !player.equals("Reckless")
									&& !player.equals("Rohrkrepierer")
									&& !player.equals("defcon")
									&& !player.equals("MrP")
									&& !player.equals("Psychobastard")
									&& !player.equals("znooptokkie")
									&& !player.equals("Hupdrop")
									&& !player.equals("AlmHurricane")
									&& !player.equals("Jester")
									&& !player.equals("Fabian")
									&& !player.equals("MrFloppy")
									&& !player.equals("tobi28")
									&& !player.equals("JimPanse")
									&& !player.equals("Seras")
									&& !player.equals("themaster")
									&& !player.equals(".:NemesisoD:.")
									&& !player.equals("Arne")
									&& !player.equals("SaltatorMortis")
									&& !player.equals("Norbert")
									&& !player.equals("PhenomTaker")
									&& !player.equals("GhostForce")
									&& !player.equals("Scotty")
									&& !player.equals("Arne")
									&& !player.equals("Nemesis")
									&& !player.equals("Mezilsa")
									&& !player.equals("TorstenB")
									&& !player.equals("Red Flag")
									&& !player.equals("Stan242")
									&& !player.equals("ELIT34V3R")
									&& !player.equals("Dura")
									&& !player.equals("Paul G.")
									&& !player.equals("CooLVipeR")
									&& !player.equals("Joker")
									&& !player.equals("K4ISER")
									&& !player.equals("Dimitri Woczniek")
									&& !player.equals("Baker")
									&& !player.equals("TorstenB")
									&& !player.equals("Darkness")
									&& !player.equals("Flippy")
									&& !player.equals("Scope")
									&& !player.equals("cr4zy")
									&& !player.equals("Jan.")
									&& !player.equals("Bierchen")
									&& !player.equals("Ricky")
									&& !player.equals("Neo")
									&& !player.equals("Sacrificii")
									&& !player.equals("Revolvermann")
									&& !player.equals("maxxctv")
									&& !player.equals("Iron Eddie")
									&& !player.equals("doublewohli")
									&& !player.equals("Steffieth")
									&& !player.equals("Voold")
									&& !player.equals("Wolle")
									&& !player.equals("Snaxx")
									&& !player.equals("elec")
									&& !player.equals("Teax")
									&& !player.equals("Paul")
									&& !player.equals("Rocco")
									&& !player.equals("Alcatraz")
									&& !player.equals("TimSice")
									&& !player.equals("LoCo")
									&& !player.equals("Igel")
									&& !player.equals("Kaiser")
									&& !player.equals("Deman")
									&& !player.equals("Falke")
									&& !player.equals("Justice")
									&& !player.equals("theNiki")
									&& !player.equals("Hotte")
									&& !player.equals("Jimpanse")
									&& !player.equals("Badguy")
									&& !player.equals("Xubix")
									&& !player.equals("Roman")
									&& !player.equals("Tofl")
									&& !player.equals("Elec")
									&& !player.equals("Shadowki")
									&& !player.equals("TimSice")
									&& !player.equals("Znoopdoggydogg")
									&& !player.equals("Amech")
									&& !player.equals("Lee")
									&& !player.equals("Psycho")
									&& !player.equals("CeLLE")
									&& !player.equals("Zinki")
									&& !player.equals("Svenson")
									&& !player.equals("Sputnik")
									&& !player.equals("Ragnar")
									&& !player.equals("xDeMoNx")
									&& !player.equals("Ketzi")
									&& !player.equals("LederStiefel")
									&& !player.equals("James")
									&& !player.equals("LdW-BinarySoul")
									&& !player.equals("Bunkerfaust")
									&& !player.equals("kriz")
									&& !player.equals("BaSh")
									&& !player.equals("Odin")
									&& !player.equals("b0untY")
									&& !player.equals("Dalyr")
									&& !player.equals("K4iser")
									&& !player.equals("venox")
									&& !player.equals("Morzzan")
									&& !player.equals("Tango")
									&& !player.equals("Reacher")
									&& !player.equals("Ryu")
									&& !player.equals("Obelix")
									&& !player.equals("PlummBumm")
									&& !player.equals("Jander")
									&& !player.equals("CELLE")
									&& !player.equals("SteelBlade")
									&& !player.equals("CoolViper")
									&& !player.equals("Bleipionier")
									&& !player.equals("WinterXVX")
									&& !player.equals("Fox")
									&& !player.equals("Marc")
									&& !player.equals("darul")
									&& !player.equals("bash")
									&& !player.equals("Hellracer")
									&& !player.equals("Witwenmacher")
									&& !player.equals("Raffsn")
									&& !player.equals("Plummbumm")
									&& !player.equals("GrimReapeR")
									&& !player.equals("Frontpig")
									&& !player.equals("saynn")
									&& !player.equals("Irawulf")
									&& !player.equals("Speed")
									&& !player.equals("Jyon")
									&& !player.equals("Michi302")
									&& !player.equals("KubaLibre")
									&& !player.equals("Bunkferfaust")
									&& !player.equals("Freakii")
									&& !player.equals("Huntexv2")
									&& !player.equals("Schulz")
									&& !player.equals("Muto")
									&& !player.equals("Crier")
									&& !player.equals("Tumult")
									&& !player.equals("beamer")
									&& !player.equals("Mr kio")
									&& !player.equals("Fynus")
									&& !player.equals("chuck")
									&& !player.equals("TheDj CooLVipeR")
									&& !player.equals("Pushklick")
									&& !player.equals("LDW-BinarySoul")
									&& !player.equals("HG2012_Hackl")
									&& !player.equals("Sieb_ger")
									&& !player.equals("HG2012_Atze")
									&& !player.equals("Stopfen_ger")
									&& !player.equals("CaM")
									&& !player.equals("Ch3 Gu3vArA")
									&& !player.equals("Rustam")
									&& !player.equals("Walnuss")
									&& !player.equals("Capio")
									&& !player.equals("Defcon")
									&& !player.equals("Berliner 19")
									&& !player.equals("Max10")
									&& !player.equals("KrachbummEnte")
									&& !player.equals("Beamer")
									&& !player.equals("Max-10")
									&& !player.equals("BlackHaraz")
									&& !player.equals("Leon")
									&& !player.equals("backshift")
									&& !player.equals("Goon")
									&& !player.equals("KnightOne")
									&& !player.equals("MaikRusGer")
									&& !player.equals("Nooror")
									&& !player.equals("OnE")
									&& !player.equals("Paru")
									&& !player.equals("BackShift")
									&& !player.equals("BountyHuntA")
									&& !player.equals("Chracka")
									&& !player.equals("Frozen Malibu")
									&& !player.equals("Kuno")
									&& !player.equals("John")
									&& !player.equals("Wex")
									&& !player.equals("displaceD")
									&& !player.equals("Jerry")
									&& !player.equals("Hibbel")
									&& !player.equals("BaumRatte")
									&& !player.equals("Koala")
									&& !player.equals("Beowulf")
									&& !player.equals("Firepower")
									&& !player.equals("Simon")
									&& !player.equals("Cerbo")
									&& !player.equals("Shinra")
									&& !player.equals("Archer")
									&& !player.equals("Kodiak")
									&& !player.equals("Fabi")
									&& !player.equals("Ulfberht")
									&& !player.equals("Assystolie")
									&& !player.equals("ANCM_Eagle")
									&& !player.equals("Booone")
									&& !player.equals("David_1")
									&& !player.equals("Drone155")
									&& !player.equals("Duke")
									&& !player.equals("EvilSpam")
									&& !player.equals("Felaex")
									&& !player.equals("FF_Oneil")
									&& !player.equals("Hanuter")
									&& !player.equals("Harry")
									&& !player.equals("Icaza")
									&& !player.equals("James221")
									&& !player.equals("Jorle")
									&& !player.equals("JulianK")
									&& !player.equals("Kyler")
									&& !player.equals("Lurti")
									&& !player.equals("McPolli")
									&& !player.equals("Natsu")
									&& !player.equals("Neodym")
									&& !player.equals("Orinion")
									&& !player.equals("Pasco")
									&& !player.equals("Platinum")
									&& !player.equals("PunkToast")
									&& !player.equals("Silexius")
									&& !player.equals("Sle3perX")
									&& !player.equals("Soryu")
									&& !player.equals("Stalker")
									&& !player.equals("Stefan")
									&& !player.equals("TobsA")
									&& !player.equals("Wallter")
									&& !player.equals("Wonder")
									&& !player.equals("WrightStriker")
									&& !player.equals("ZnY")
									&& !player.equals("MrPink")
									&& !player.equals("Blackburn")
									&& !player.equals("McFly")
									&& !player.equals("FF_Oneill")
									&& !player.equals("Berliner19")
									&& !player.equals("Brainwashington")
									&& !player.equals("Guggi")
									&& !player.equals("Arjuna")
									&& !player.equals("Basox")
									&& !player.equals("Bosche")
									&& !player.equals("Cake")
									&& !player.equals("CandyMan")
									&& !player.equals("Gunny")
									&& !player.equals("IpSwitsch")
									&& !player.equals("Llama")
									&& !player.equals("LuckyLuke")
									&& !player.equals("Moore")
									&& !player.equals("rasaf")
									&& !player.equals("SkilzZ")
									&& !player.equals("Tika Bell")
									&& !player.equals("Maximax")
									&& !player.equals("Baron")
									&& !player.equals("Miller")
									&& !player.equals("JethroGibbs")
									&& !player.equals("Axel")
									&& !player.equals("Twisted")
									&& !player.equals("Wolfi")
									&& !player.equals("Eva")
									&& !player.equals("Daisy")
									&& !player.equals("Chitario")
									&& !player.equals("caldin")
									&& !player.equals("Stefano Bontade")
									&& !player.equals("JakobAigi")
									&& !player.equals("Thunder")
									&& !player.equals("Idefix")
									&& !player.equals("Prince")
									&& !player.equals("StefPlay")
									&& !player.equals("Axe")
									&& !player.equals("Chief Wiggum")
									&& !player.equals("DarkWhisperer")
									&& !player.equals("DrJekyll")
									&& !player.equals("Dynamike")
									&& !player.equals("Insane")
									&& !player.equals("Kane Nod")
									&& !player.equals("Legendz")
									&& !player.equals("Luxi")
									&& !player.equals("Marius")
									&& !player.equals("NicNac")
									&& !player.equals("QuanTas")
									&& !player.equals("Ragen")
									&& !player.equals("RoadRunner")
									&& !player.equals("Weskott")
									&& !player.equals("Whiskey")
									&& !player.equals("Bak0")
									&& !player.equals("chiccy")
									&& !player.equals("Elirah")
									&& !player.equals("Faital")
									&& !player.equals("Hope")
									&& !player.equals("ille")
									&& !player.equals("JKbaxter")
									&& !player.equals("Klon")
									&& !player.equals("Kuchenkasten")
									&& !player.equals("Lars")
									&& !player.equals("Locke")
									&& !player.equals("Ratte")
									&& !player.equals("Repkow")
									&& !player.equals("Sepp")
									&& !player.equals("Stubus Lupus")
									&& !player.equals("Sturm")
									&& !player.equals("TJ_S")
									&& !player.equals("Toko1993")
									&& !player.equals("Tone")
									&& !player.equals("Tummi")
									&& !player.equals("Whitefox")
									&& !player.equals("Wyqer")
									&& !player.equals("ZerO")
									&& !player.equals("HungryEngineer")) {
								System.err.println("External data says player '" + player
										+ "' has not participated on this event"
										+ " (" + title + ")" + ":" + CrawlerUtil.convertDateToString(date));
							}
						} else {
							System.err.println("External event has different type of '"
									+ extType + "' instead '" + type + "' ("
									+ title + ")");
						}
					} else if (date.before(Calendar.getInstance())) {
						System.err.println("Can't find external event with web events date: "
								+ CrawlerUtil.convertDateToString(date) + " (" + title + ")");
					}
					//Add found player
					if (extEventPlayers != null) {
						extEventPlayers.remove(player);
					}
					if (slotText.toLowerCase().equals(slot.toString().toLowerCase())) {
						slotlist.addSlot(key, slot, "", player, status);
					} else {
						slotlist.addSlot(key, slot, slotText, player, status);
					}
				}
			}
			
			/*
			//Search for reserve
			if (listStartFound && !slotFound) {
				
			}
			*/
			
			//Search list start if not found already
			if (!listStartFound) {
				pattern = Pattern.compile("((Slotliste)|(Slotdatenbank)|(Slotlist)|"
						+ "(Teilnehmer)|(Anmeldungen)|(Wer kommt\\?)|(Interessierte)|"
						+ "(Dabei sind)|(Lernwillige Zöglinge)|(Die Auserwählten)|"
						+ "(lotliste)|(Zeitslots))[*:]?[\\s]?(&lt;){0,3}</",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("<strong>Teilnehmer -",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("((Gruppe W - Die Herausforderer!)|(Slotliste - Server #1))[*:]?[\\s]?(&lt;){0,3}</",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("Wo:</strong> Brigade2010<br />$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("^SlotÂ´s<br />$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile(" zu vergeben:<br />$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("^<strong>Gruppe DELTA:<br />$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("wer dabei ist.<br />$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("^1.0 Slotliste:<br />$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("^<i><strong>Godfather v3</strong></i><br />$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("^So, hier nun die freien Slots:<br />$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("Slotliste der Mission anzupassen...<br />$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("^Folgende Plätze sind verfügbar:<br />$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("^Missionsstart pünktlich 2000h<br />$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
			if (!listStartFound) {
				pattern = Pattern.compile("Flughafen einnehmen, Team Rot verteidigt!</strong><br />$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					listStartFound = true;
				}
			}
		} while (!line.contains(THREAD_CONTENT_END));
		
		//Add players that are only listed in the external data and not in the thread
		//to the reserve
		if (extEventPlayers != null && extEventPlayers.size() > 0) {
			for (String player : extEventPlayers) {
				SlotStatus status = extEventDate.getPlayerStatus(player);
				slotlist.addReserve(player, status);
			}
		}
		
		
		if (!listStartFound) {
			System.err.println("Can't find threads slotlist with title: " + title);
		} else if (slotlist == null || slotlist.slotSize() == 0) {
			System.err.println("Can't find slots in threads slotlist with title: " + title);
		}
		
		return slotlist;
	}
	private static SlotType parseSlotType(String slotText) {
		SlotType slot = SlotType.NO_TYPE;
		
		//Parse slot types
		boolean slotTypeFound = false;
		Pattern pattern = Pattern.compile("^((CO|Commanding[\\s]?Officer)([\\s]?\\(.*\\))?|"
				+ "Platoon (Leader|Commander) \\(CO\\)|Kommandierender[\\s]?Offizier[\\s]?(/CO)?|"
				+ "Einsatzleiter|Ilaclar Anführer|Anführer|Kompanieführer \\(CO\\)|"
				+ "Company[\\s]?Leader|Plt Leader \\(CO,.*\\)|Troop Commander \\(CO\\)|"
				+ "CO[\\s\\-]?/[\\s\\-]?TC|Talibananführer([\\s]?\\(C.*\\))?.*|FBI Agent Teamleader|Lehrgangsleiter)$",
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(slotText);
		if (matcher.find()) {
			slotTypeFound = true;
			slot = SlotType.CO;
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^((XO|Rechte Hand|RTO|Executive[\\s]?Officer)([\\s]?\\(.*\\))?"
					+ "|Ass\\. Platoonleader \\(XO\\)|Platoon[\\s]?Sergeant \\(XO\\)|"
					+ "Kommandierender[\\s]?Offizier(/XO)?|Assistenz-Leitung|Silah Anführer|"
					+ "Ausführender Offizier/XO|Panzerzugführer \\(XO/PL\\)|"
					+ "Squad Leader[\\s]?\\(XO,.*\\)|Panzerzugführer[\\s]?\\(XO.*\\)|"
					+ "Troop Sergeant \\(XO\\)|XO[\\s\\-]?/[\\s\\-]?TC|Panzerzugführer[\\s]?\\(XO.*\\)([\\s]?-)?|"
					+ "FBI Agent)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.XO;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(CoL|Chief[\\s]?of[\\s]?Logisti[ck]s?)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.COL;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(JTAC|ATC|Joint[\\s]?Tactical[\\s]?Air[\\s]?Controller|Air[\\s]?Traffic[\\s]?Controller|"
					+ "Air Intelligence Officer)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.JTAC;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(MIO|Military[\\s]?Intelligence[\\s]?Officer)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.MIO;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(FO|Forward[\\s]?Observer)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.MIO;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^((Tank[\\s\\-]?)?Platoon[\\s-]?(Infantry[\\s\\-]?)?Lead(er)?|Platoon Command(er)?|"
					+ "Panzerzugführer \\(PL\\)|Platoonleader[\\s]?\\([^XC].*\\))$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.PL;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(Tank[\\s\\-]?)?Platoon[\\s]?(Sergeant|Seargent)([\\s]?\\([^XC].*\\))?$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.PSG;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(((Tank|BMP)[\\s\\-]?)?(Kommandant|Commander)([\\s]?\\(.*\\))?|RIOT-Fahrzeug Kommandant|"
					+ "Squad Leader[\\s]?\\(BMP Commander\\)|Tank[\\s\\-]?(Platoonleader|Commander))$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.TC;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(GNR|(Richt|Panzer)?Schütze([\\s]?\\(.*\\))?|RIOT-Fahrzeug Schütze|"
					+ "BMP[\\s]{0,2}Gunner|Gunner)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.GNR;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(DRV|(Panzer)?Fahrer([\\s]?\\(.*\\))?|RIOT-Fahrzeug Fahrer|"
					+ "BMP Driver|Driver|Panzerhaubitzen[\\s\\-]?Fahrer)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.DRV;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^((Logisti[ck][\\s\\-]?)?Team[\\s]?(leader|leiter)([\\s\\-\\(SD\\)]?)?\\*?|Batterieführer|Mörserkommandant|"
					+ "(Ärzte|Medic)[\\s\\-]?Teamleader|Rebellenführer|Gruppenleiter|Offizier|SWAT-Teamleader|"
					+ "Team Leader/Gunner|EOD[\\s\\-]?Teamlead(er)?|Mörser[\\s\\-]?Kommandant|"
					+ "Logistik[\\s\\-]?Teamleader|Panzerhaubitzen[\\s\\-]?Kommandant|"
					+ "Senior[\\s\\-]Rifleman/[\\s]?Team[\\s-]?leader|Teamleader \\(AK.*\\))$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.TL;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(Squad[\\s]?lead(er)?[\\s]?(\\([^XC].*\\)|Alpha|Bravo|Charlie|"
					+ "Delta|Echo|Romeo)?|Zellenführer)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.SL;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(FTL|Fire[\\s]?team[\\s\\-]?lead(er)?|Truppführer|"
					+ "Scout[\\s\\-]Teamleader?|Zellenführer[\\s]+\\(FTL\\).*)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.FTL;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(LMG|(Scout[\\s\\-]?)?Automati[ck][\\s]?Rifleman|Autorifleman|Auto\\. Rifleman|"
					+ "Leibwache \\(AR\\)|Automatikschütze|LMG[\\s\\-]?(Schütze)?|"
					+ "Operator \\(Weapon Specialist\\)|Maschinengewehr[\\s\\-]?Schütze|"
					+ ".*\\(RPK\\).*)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.AR;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^((Ass(ist)?\\.|Assistant)[\\s]?Automati[ck][\\s]?Rifleman|Rifleman([\\s\\-\\(SD\\)]?)?|Operator|Kämpfer|"
					+ "Polizei-Beamter|SWAT-Beamter|Krimineller|Nahsicherer|Assistant[\\s]?Grenadier|Assistent|"
					+ "Mörser Nahsicherer|(Filmproduzent|Kameramann)([\\s]?\\((AK|PKM).*\\))?|(Assist[ea]nt[\\s\\-]?Scout)?)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.RFL;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^Grenadier([\\s]\\(.*\\))?$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.GRE;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(DM|Designated[\\s]?Marksman|Marksman \\(SVDM\\)|Marksman|"
					+ "Gruppenscharfschütze|Leibwache \\(DM\\)|SVD Rifleman|"
					+ "Rifleman[\\s]?\\(SVD\\))$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.DM;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^((Scout[\\s\\-]?|Rifleman/)?Combat[\\s]?Medic|Notfallsanitäter|Leibwache \\(MED\\)|"
					+ "Para[\\s\\-]?medic|Corpsman|Rifleman[\\s]?\\(Medic\\)|"
					+ "Operator \\(Medic([\\s]?Specialist)?\\)|Rifleman/Medic|Flight[\\s\\-]Medic)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.CMDC;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^Rifleman[\\s\\-]?\\(?AT\\)?$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.ATR;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^Rifleman[\\s]?AA$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.AAR;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(MG|(Heavy)?[\\s]?Machine[\\s]?gunner([\\s\\-]?\\(PKM\\))?|Heavy[\\s]?Automati[ck][\\s]?Rifleman|"
					+ "MMG Rifleman|MG[0-9][\\s\\-]?Schütze)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.MG;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(AMG|Ass(ist)?\\.[\\s]?Machinegunner|Assistent[\\s]?Automati[ck][\\s]?Rifleman|"
					+ "PKP-Ass(ist)?(\\.)?|Assistant MMG Rifleman|Munitionsträger([\\s\\-\\(SD\\)]?)?)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.AMG;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(AT[\\s\\-]?(Specialist|Soldat)?|Missile[\\s]?Specialist[\\s]?\\(Javelin\\)|Heavy[\\s]?(Anti-Tank|AT)[\\s]?Rifleman)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.AT;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(Ass(ist)?\\.[\\s]?AT|Missile[\\s]?Specialist[\\s]?\\((Asst.|Assistent|Ass(ist)?.)\\)"
					+ "|Assistent[\\s]?(Anti-Tank|AT)[\\s]?Rifleman|(Assistant|Ass(ist)?\\.)[\\s]?AT[\\s\\-]?Specialist)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.AAT;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(EOD|Explosiv[e]?[\\s]?Ordnance[\\s]?Disposel|Explosiv[e]?[\\s]?Expert|"
					+ "Explosiv[e]?[\\s-]?Specialist|Sprengstoff Spezialist|Pionier|EOD[\\s\\-]?Specialist|"
					+ "Operator \\(Explosi(on|ve) Specialist\\))$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.CE;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^((Scout[\\s\\-]?)?Sniper|Scharfschütze|SWAT-Scharfschütze)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.SNP;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^((Scout[\\s\\-]?)?Spotter|SWAT-Spotter)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.SPT;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^Logistik(er)?([\\s]\\(.*\\))?$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.LOG;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(Pilot([\\s]\\(.*\\))?([\\s\\-]?(Medevac|Sicherung).*)?|Rottenführer)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.PIL;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^Co[\\.-]Pilot([\\s]\\(.*\\))?$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.CPIL;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^Fixed-wing Pilot [12] \\(.*\\)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.WCO;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^Gunner \\(UH-80\\)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.WSO;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^((Platoon|Company)?[\\s]?Medic([\\s]\\(.*\\)?)?|Internist|Chirurg|Senior Corpsman|"
					+ "Platoon Corpsman|Notarzt|Notfall[\\s\\-]?Sanitäter|Sanitäter|(Field[\\s\\-]?)?Surgeon)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.MDC;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(Mörser|Panzerhaubitzen?)[\\s\\-]?schütze$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.ACSO;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(Geisel|Geiselnehmer/CO\\+|Co\\+([\\s\\-]?Spieler)?)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.ZC_PLUS;
			}
		}
		if (!slotTypeFound) {
			pattern = Pattern.compile("^(Door[\\s-]?Gunner|BB(-)?Spieler[\\s]?(\\(.*\\))?|"
					+ "Air Support Control Officer|Assistant Gunner|\\?\\?\\?|"
					+ "Seiten[\\s\\-]?schütze)$",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(slotText);
			if (matcher.find()) {
				slotTypeFound = true;
				slot = SlotType.OTHER;
			}
		}
		
		//Match type exact names
		if (!slotTypeFound) {
			for (SlotType type : SlotType.values()) {
				pattern = Pattern.compile("^.*\\(" + type + "\\).*$",
						Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(slotText);
				if (matcher.find()) {
					slotTypeFound = true;
					slot = type;
				}
				if (!slotTypeFound) {
					pattern = Pattern.compile("^" + type + "$",
							Pattern.CASE_INSENSITIVE);
					matcher = pattern.matcher(slotText);
					if (matcher.find()) {
						slotTypeFound = true;
						slot = type;
					}
				}
			}
		}
		
		//Hard-match resulting types using generator SlotParseTool
		if (!slotTypeFound) {
			Map<String, SlotType> generatedMap = new HashMap<String, SlotType>();
			//Generated by SlotParseTool : 00to10.csv
			generatedMap.put("Platoon Leader (Befehligt auch FIA)", SlotType.PL);
			generatedMap.put("UAV Operator (UGV)", SlotType.UGSO);
			generatedMap.put("Marksman (MAR-10)", SlotType.DM);
			generatedMap.put("UGV Specialist", SlotType.UGSO);
			generatedMap.put("Assistant Anti-Tankspecialist", SlotType.AAT);
			generatedMap.put("ZC+", SlotType.ZC_PLUS);
			generatedMap.put("Assistant Anti-Airspecialist", SlotType.AAA);
			generatedMap.put("MMG Schütze", SlotType.MG);
			generatedMap.put("Assistant Rifleman", SlotType.AMG);
			generatedMap.put("Techniker", SlotType.OTHER);
			generatedMap.put("Chief Gunnery Sergeant", SlotType.CO);
			generatedMap.put("Gunnery Sergeant", SlotType.XO);
			generatedMap.put("Senior-JTAC", SlotType.JTAC);
			generatedMap.put("Haubitzen Schütze", SlotType.GNR);
			generatedMap.put("Haubitzen Fahrer", SlotType.DRV);
			generatedMap.put("Rifleman(AT-NLAW)", SlotType.ATR);
			generatedMap.put("MMG Schütze", SlotType.MG);
			generatedMap.put("(ZC+) Führungskraft", SlotType.ZC_PLUS);
			generatedMap.put("Zeus/CO+/etc.", SlotType.ZC_PLUS);
			generatedMap.put("Senior Superior Master Chief", SlotType.CO);
			generatedMap.put("Infantry", SlotType.RFL);
			generatedMap.put("Anti-Tank Rifleman", SlotType.ATR);
			generatedMap.put("(N/A) BB-Spieler", SlotType.OTHER);
			generatedMap.put("Lieutenant Hardboil", SlotType.CO);
			generatedMap.put("Sergeant Brown", SlotType.XO);
			generatedMap.put("RWS*", SlotType.UGSO);
			generatedMap.put("Scharfschütze (Leihgabe vom US-Marine Corps)", SlotType.SNP);
			generatedMap.put("Aufklärer (Leihgabe vom US-Marine Corps)", SlotType.SPT);
			generatedMap.put("Flanker (Leihgabe vom US-Marine Corps)", SlotType.RFL);
			generatedMap.put("Combat UGV", SlotType.UGSO);
			
			generatedMap.put("Stellvertretender Offizier", SlotType.XO);
			generatedMap.put("MG-Schütze (PKP)", SlotType.AR);
			generatedMap.put("RPG-Schütze (RPG-7)", SlotType.AT);
			generatedMap.put("Assistent RPG Schütze (AK-74M)", SlotType.AAT);
			generatedMap.put("Schütze/Medic", SlotType.CMDC);
			generatedMap.put("BTR Fahrzeugfahrer", SlotType.DRV);
			generatedMap.put("BTR Fahrzeug-Schütze", SlotType.GNR);
			generatedMap.put("MG-Schütze (PKP)", SlotType.AR);
			generatedMap.put("RPG-Schütze (RPG-7)", SlotType.AT);
			generatedMap.put("Assistent RPG Schütze (AK-74M)", SlotType.AAT);
			generatedMap.put("Schütze/Medic", SlotType.CMDC);
			generatedMap.put("BTR Fahrzeugfahrer", SlotType.DRV);
			generatedMap.put("Gruppenführer/BTR Kommandant", SlotType.TL);
			generatedMap.put("Fahrzeug-Schütze", SlotType.GNR);
			generatedMap.put("Fahrzeugfahrer", SlotType.DRV);
			generatedMap.put("Senior Schütze/NSV-Assistent", SlotType.TL);
			generatedMap.put("NSV-Schütze", SlotType.MG);
			generatedMap.put("NSV-Assistent", SlotType.AMG);
			generatedMap.put("AGS-30-Assistent", SlotType.MG);
			generatedMap.put("AGS-30-Schütze", SlotType.AMG);
			generatedMap.put("Fahrzeugführer", SlotType.TL);
			generatedMap.put("CAS Pilot - Rottenführer", SlotType.WCO);
			generatedMap.put("CAS Gunner", SlotType.WSO);
			generatedMap.put("CAS Pilot", SlotType.PL);
			generatedMap.put("SOF TL Taucher", SlotType.TL);
			generatedMap.put("SOF Taucher", SlotType.RFL);
			generatedMap.put("Maskottchen", SlotType.RFL);
			generatedMap.put("BTR Commander", SlotType.TL);
			generatedMap.put("BTR Schütze", SlotType.GNR);
			generatedMap.put("BTR Fahrer (mit Reparatur Ausbildung)", SlotType.DRV);
			generatedMap.put("Rifleman(AT-RPG7)", SlotType.ATR);
			generatedMap.put("Logistiker - Pilot", SlotType.PL);
			generatedMap.put("Logistiker - Co-Pilot", SlotType.CPIL);
			generatedMap.put("Marksman (SR-25)", SlotType.DM);
			generatedMap.put("Scharfschütze (G82)", SlotType.SNP);
			generatedMap.put("Weapon Specialist", SlotType.RFL);
			generatedMap.put("CO - Groundcontrol", SlotType.CO);
			generatedMap.put("XO - Aircontrol", SlotType.XO);
			generatedMap.put("BMP-1 Driver/Mechanic", SlotType.DRV);
			generatedMap.put("Marksman(Mk14)", SlotType.SNP);
			generatedMap.put("Platoon Sgt.", SlotType.PSG);
			generatedMap.put("Platoon Lead.", SlotType.PL);
			generatedMap.put("Executive Officer/Fahrer", SlotType.XO);
			generatedMap.put("BMP-2D Commander/Platoon Leader", SlotType.PL);
			generatedMap.put("BMP-2D Machine Gunner", SlotType.GNR);
			generatedMap.put("BMP-2D Driver/Mechanic", SlotType.DRV);
			generatedMap.put("BMP-1 Commander/Platoon Sergeant", SlotType.PL);
			generatedMap.put("BMP-1 Machine Gunner", SlotType.GNR);
			generatedMap.put("Verladeoffizier", SlotType.LOG);
			generatedMap.put("Talibanboss      (SQL)", SlotType.SL);
			generatedMap.put("Talibanboss    (SQL)", SlotType.SL);
			generatedMap.put("Mediziner       (MED)", SlotType.MDC);
			//Generated by SlotParseTool : 30to40.csv
			generatedMap.put("Reporter (RPG-7V)", SlotType.ATR);
			generatedMap.put("Team Leader (SD)", SlotType.TL);
			generatedMap.put("Rifleman (SD)", SlotType.RFL);
			generatedMap.put("Munitionsträger (SD)", SlotType.AMG);
			generatedMap.put("Scout", SlotType.RFL);
			generatedMap.put("Gruppenführer", SlotType.FTL);
			generatedMap.put("Teamführer", SlotType.TL);
			generatedMap.put("MG-Schütze (MG4)", SlotType.MG);
			generatedMap.put("MG-Schütze (MG5)", SlotType.MG);
			generatedMap.put("Spähtruppführer (Kommandant)", SlotType.TL);
			generatedMap.put("Kraftfahrer", SlotType.DRV);
			generatedMap.put("Waffensystembediener", SlotType.WSO);
			generatedMap.put("Senior Military Intelligence Officer Master Chief (to the Max)", SlotType.MIO);
			generatedMap.put("Junior MIO (Helfer)", SlotType.MIO);
			generatedMap.put("Military Intelligence Apprentice", SlotType.MIO);
			generatedMap.put("JTAC-Einsteiger", SlotType.JTAC);
			generatedMap.put("CAS-Pilot Ausbilder (Jet)", SlotType.WCO);
			generatedMap.put("CAS-Pilot (Jet)", SlotType.WCO);
			generatedMap.put("CAS-Pilot Ausbilder (Helikopter)", SlotType.PIL);
			generatedMap.put("CAS-Pilot Anwärter (Helikopter)", SlotType.PIL);
			//Generated by SlotParseTool : 40to50.csv
			generatedMap.put("Senior Elite Chief of Logistics", SlotType.COL);
			generatedMap.put("Logistik-Ausbildungshelfer", SlotType.LOG);
			generatedMap.put("CO/Platoonleader", SlotType.CO);
			generatedMap.put("XO/Platoon Sergeant", SlotType.XO);
			generatedMap.put("Dr. Pfleger", SlotType.CO);
			generatedMap.put("Korporal", SlotType.FTL);
			generatedMap.put("Rekrut", SlotType.RFL);
			generatedMap.put("BMP-2 Commander/Platoon Leader", SlotType.PL);
			generatedMap.put("BMP-2 Machine Gunner", SlotType.GNR);
			generatedMap.put("BMP-2 Driver/Mechanic", SlotType.DRV);
			generatedMap.put("Rifleman/Assistant Grenadier", SlotType.RFL);
			generatedMap.put("BMP-2 Commander/Platoon Sergeant", SlotType.PL);
			generatedMap.put("BMP-2 Commander", SlotType.TC);
			generatedMap.put("Team Leader/ Assistant Anti-Tank Specialist", SlotType.TL);
			generatedMap.put("Anti-Tank Specialist", SlotType.AT);
			generatedMap.put("Assistant Anti-Tank Specialist", SlotType.AAT);
			generatedMap.put("ExplosivSpecialist Tadome", SlotType.CO);
			generatedMap.put("Rekrut (vertauscht Grünen Draht mit Roten)", SlotType.RFL);
			generatedMap.put("Rekrut (Das 3te mal dabei)", SlotType.RFL);
			generatedMap.put("Rekrut (Schielt ein wenig)", SlotType.RFL);
			generatedMap.put("Rekrut (Mag es laut)", SlotType.RFL);
			generatedMap.put("Rekrut (findet ExplosivSpecialist Tadome ganz toll)", SlotType.RFL);
			generatedMap.put("Rekrut (Ist Farbenblind)", SlotType.RFL);
			generatedMap.put("Assistant Machinegunner", SlotType.AMG);
			generatedMap.put("Reporter", SlotType.RFL);
			generatedMap.put("Platoon Leader (Longbeard Actual)", SlotType.PL);
			generatedMap.put("Squad Leader Longbeard 1 Actual)", SlotType.SL);
			generatedMap.put("FTL (Longbeard 1-1)", SlotType.FTL);
			generatedMap.put("FTL (Longbeard 1-2)", SlotType.FTL);
			generatedMap.put("Rifleman (Munischlepper)", SlotType.RFL);
			generatedMap.put("FTL (Longbeard 2-1)", SlotType.FTL);
			generatedMap.put("FTL (Longbeard 2-2)", SlotType.FTL);
			generatedMap.put("Zellenleitung", SlotType.CO);
			generatedMap.put("Stellv.", SlotType.XO);
			generatedMap.put("Asst. Machinegunner", SlotType.AMG);
			generatedMap.put("Agency Coordination Officer", SlotType.CO);
			generatedMap.put("Agency Intelligence Officer", SlotType.XO);
			generatedMap.put("CIA Teamleader", SlotType.CO);
			generatedMap.put("CIA Sniper", SlotType.SNP);
			generatedMap.put("CIA Spotter", SlotType.SPT);
			generatedMap.put("CIA UAV-Operator", SlotType.RFL);
			generatedMap.put("CIA Combat Medic", SlotType.CMDC);
			//Generated by SlotParseTool : 50to60.csv
			generatedMap.put("MIO (Military Intelligence Officer)", SlotType.MIO);
			generatedMap.put("CoL (Chief of Logistics)", SlotType.COL);
			generatedMap.put("Gehilfe vom Übungsleiter", SlotType.XO);
			generatedMap.put("Teilnehmer", SlotType.OTHER);
			generatedMap.put("Rekrut (hat die CSAT noch nie gesehen)", SlotType.RFL);
			generatedMap.put("Rekrut (hat bei Bear Rose nicht aufgepasst)", SlotType.RFL);
			generatedMap.put("Rekrut (braucht eine neue Brille)", SlotType.RFL);
			generatedMap.put("Rekrut (hat zu viele Karotten gegessen)", SlotType.RFL);
			generatedMap.put("Rekrut (schielt)", SlotType.RFL);
			generatedMap.put("Lehrgangsleiter -", SlotType.CO);
			generatedMap.put("Hilfszeus -", SlotType.ZC_PLUS);
			generatedMap.put("Commander 1", SlotType.TC);
			generatedMap.put("Gunner 1 -", SlotType.GNR);
			generatedMap.put("Teamleader 1 -", SlotType.FTL);
			generatedMap.put("Infanterist", SlotType.RFL);
			generatedMap.put("Commander 2", SlotType.TC);
			generatedMap.put("Gunner 2", SlotType.GNR);
			generatedMap.put("Teamleader 2", SlotType.FTL);
			generatedMap.put("Gunner 3", SlotType.GNR);
			generatedMap.put("Driver 3", SlotType.DRV);
			generatedMap.put("Teamleader 3", SlotType.FTL);
			generatedMap.put("Rekrut (findet Dr. Pfleger ganz toll)", SlotType.RFL);
			generatedMap.put("Ausbilder", SlotType.CO);
			generatedMap.put("Blutiger Anfänger", SlotType.RFL);
			generatedMap.put("Maschinengewehrschütze (MG 5)", SlotType.MG);
			generatedMap.put("Asst. Maschinengewehrschütze", SlotType.AMG);
			generatedMap.put("Panzerabwehrschütze (Pzf. 3)", SlotType.AT);
			generatedMap.put("Asst. Panzerabwehrschütze", SlotType.AAT);
			generatedMap.put("Drohnenpilot (AR-2 Darter)", SlotType.MIO);
			generatedMap.put("Assistant Platoon Leader", SlotType.PSG);
			generatedMap.put("Vorfunker -", SlotType.XO);
			generatedMap.put("Nachfunker", SlotType.RFL);
			generatedMap.put("Nachfunker (wird schnell nervös) -", SlotType.RFL);
			generatedMap.put("Nachfunker (erzählt gern Geschichten)", SlotType.RFL);
			generatedMap.put("Nachfunker (glaubt, er kann das schon)", SlotType.RFL);
			generatedMap.put("Rekrut (kann keine Entfernungen einschätzen)", SlotType.RFL);
			generatedMap.put("Rekrut (schoss aus Versehen Flares auf die Gegner)", SlotType.RFL);
			generatedMap.put("Rekrut (vermisst den M32)", SlotType.RFL);
			//Generated by SlotParseTool : 60to70.csv
			generatedMap.put("Gastredner", SlotType.CO);
			generatedMap.put("Azubi", SlotType.RFL);
			generatedMap.put("RTO/ Intelligence Officer in der Basis", SlotType.XO);
			generatedMap.put("Platoon Engineer", SlotType.LOG);
			generatedMap.put("Platoon Engineer Reservist", SlotType.LOG);
			generatedMap.put("Grenadier/ UAV Operator", SlotType.GRE);
			generatedMap.put("Grenadier/ AT", SlotType.ATR);
			generatedMap.put("Grenadier/ AA", SlotType.AAR);
			generatedMap.put("Sergeant", SlotType.PSG);
			generatedMap.put("Teamlead", SlotType.FTL);
			generatedMap.put("Gunner (PKM)", SlotType.MG);
			generatedMap.put("Gunner (RPG-7V)", SlotType.ATR);
			generatedMap.put("Ass. Gunner (PKM)", SlotType.ATR);
			generatedMap.put("AT-Specialist (Metis-Stationär)", SlotType.AT);
			generatedMap.put("Ass. AT-Specialist (Metis-Stationär)", SlotType.AAT);
			generatedMap.put("Marksman (SVD)", SlotType.DM);
			generatedMap.put("Section-Commander", SlotType.SL);
			generatedMap.put("Gehilfe", SlotType.PSG);
			generatedMap.put("Stinger Spezialist", SlotType.AA);
			generatedMap.put("Späher", SlotType.RFL);
			generatedMap.put("Explosiv Spezialist", SlotType.CE);
			generatedMap.put("Mediziner", SlotType.MDC);
			generatedMap.put("Machingunner", SlotType.MG);
			generatedMap.put("Ammobearer", SlotType.AMG);
			generatedMap.put("Missile Specialist AT", SlotType.AT);
			generatedMap.put("Assistent AT", SlotType.AAT);
			generatedMap.put("Zugführer -", SlotType.PL);
			generatedMap.put("Stellv. Zugführer", SlotType.PSG);
			generatedMap.put("Zugsanitäter -", SlotType.CMDC);
			generatedMap.put("Gruppenführer -", SlotType.SL);
			generatedMap.put("MG4-Schütze -", SlotType.MG);
			generatedMap.put("Truppführer -", SlotType.FTL);
			generatedMap.put("MG5-Schütze -", SlotType.MG);
			generatedMap.put("MG5-Assistent", SlotType.AMG);
			generatedMap.put("Artilleriebeobachter", SlotType.ACSO);
			generatedMap.put("Gruppenscharfschütze -", SlotType.DM);
			generatedMap.put("Ladeschütze", SlotType.RFL);
			generatedMap.put("Survivalexperte", SlotType.RFL);
			generatedMap.put("Pfadfinder", SlotType.RFL);
			generatedMap.put("Pfadfinder (seit 11 Jahren dabei)", SlotType.RFL);
			generatedMap.put("Samweis Gamdschie", SlotType.RFL);
			generatedMap.put("ArmA-Nerd", SlotType.RFL);
			generatedMap.put("Austauschschüler (Japan)", SlotType.RFL);
			generatedMap.put("Pfadfinder (hat Identitätskrise)", SlotType.RFL);
			generatedMap.put("Pfadfinder (sieht aus wie Ron Weasley)", SlotType.RFL);
			generatedMap.put("Ehemaliger US-Marine", SlotType.RFL);
			generatedMap.put("Platoonführer", SlotType.PL);
			generatedMap.put("Assi. Platoonführer", SlotType.PSG);
			generatedMap.put("Forward Air Controller", SlotType.JTAC);
			generatedMap.put("Assi. MG5-Schütze", SlotType.AMG);
			generatedMap.put("Panzerfaust-Schütze", SlotType.AT);
			generatedMap.put("Assi. Panzerfaust-Schütze", SlotType.AAT);
			generatedMap.put("Feldsanitäter", SlotType.CMDC);
			generatedMap.put("Aufklärer", SlotType.RFL);
			generatedMap.put("Platoon Leader (Whistle Actual)", SlotType.PL);
			generatedMap.put("Fireteam Leader (Whistle 1-1)", SlotType.FTL);
			generatedMap.put("Fireteam Leader (Whistle 1-2)", SlotType.FTL);
			generatedMap.put("Fireteam Leader (Whistle 2-1)", SlotType.FTL);
			generatedMap.put("Fireteam Leader (Whistle 2-2)", SlotType.FTL);
			generatedMap.put("Munischlepper", SlotType.AMG);
			generatedMap.put("Fireteam Leader (Whistle 3-1)", SlotType.FTL);
			generatedMap.put("Fireteam Leader (Whistle 3-2)", SlotType.FTL);
			generatedMap.put("Fireteam Leader (Whistle 4-1)", SlotType.FTL);
			generatedMap.put("Fireteam Leader (Whistle 4-2l)", SlotType.FTL);
			generatedMap.put("Team Leader  (Whistle 5 Actual)", SlotType.FTL);
			generatedMap.put("UAV-Operator", SlotType.MIO);
			generatedMap.put("Senior Technician (chief of logistics)", SlotType.COL);
			generatedMap.put("Joint terminal attack controller", SlotType.JTAC);
			generatedMap.put("Squad Leader/BTR Commander", SlotType.TC);
			generatedMap.put("Senior Rifleman/Asst. Squad Leader", SlotType.PSG);
			generatedMap.put("BTR Driver/Mechanic", SlotType.DRV);
			generatedMap.put("BTR Machine Gunner", SlotType.GNR);
			generatedMap.put("Senior Rifleman/Asst. Team Leader", SlotType.RFL);
			generatedMap.put("Sniper (VSS Vintorez)", SlotType.SNP);
			generatedMap.put("Senior Logistiker (Leader)", SlotType.LOG);
			//Generated by SlotParseTool : 70to80.csv
			generatedMap.put("Platoon Leader -", SlotType.PL);
			generatedMap.put("Rifleman (AT-Assist)", SlotType.AAT);
			generatedMap.put("UAV Operator", SlotType.MIO);
			generatedMap.put("Abdar Sorour", SlotType.ZC_PLUS);
			generatedMap.put("Achmed Salik", SlotType.ZC_PLUS);
			generatedMap.put("Company Sergeant", SlotType.PSG);
			generatedMap.put("Teamleader -", SlotType.TL);
			generatedMap.put("AA Specialist", SlotType.AA);
			generatedMap.put("Assistant AA Specialist", SlotType.AAA);
			generatedMap.put("Tank Commander -", SlotType.TC);
			generatedMap.put("Commander -", SlotType.TC);
			generatedMap.put("Gunner -", SlotType.GNR);
			generatedMap.put("Recon Teamleader", SlotType.FTL);
			generatedMap.put("Recon Marksman", SlotType.DM);
			generatedMap.put("Recon Scout", SlotType.RFL);
			generatedMap.put("Recon UAV-Operator", SlotType.RFL);
			generatedMap.put("Logistics Teamleader -", SlotType.TL);
			generatedMap.put("Logistician", SlotType.LOG);
			generatedMap.put("Pilot (Hellcat) -", SlotType.PIL);
			generatedMap.put("Zugführer", SlotType.TL);
			generatedMap.put("Zugführer (Stellvertreter)", SlotType.XO);
			generatedMap.put("Truppführer (Stellvertreter)", SlotType.XO);
			generatedMap.put("MG Assistent", SlotType.AMG);
			generatedMap.put("JTAC-Senior (TAC-Team)", SlotType.JTAC);
			generatedMap.put("JTAC-Apprentice (TAC-Team)", SlotType.JTAC);
			generatedMap.put("Anti-Air Specialist", SlotType.AA);
			generatedMap.put("Reconlead", SlotType.TL);
			generatedMap.put("Rifleman JTAC", SlotType.JTAC);
			generatedMap.put("Rfileman", SlotType.RFL);
			generatedMap.put("Doc Brown**", SlotType.MDC);
			generatedMap.put("AT-Soldier", SlotType.AT);
			generatedMap.put("Ass. AT-Soldier", SlotType.AAT);
			generatedMap.put("Ass. Machinegunner -", SlotType.AMG);
			generatedMap.put("Demolition Expert", SlotType.CE);
			generatedMap.put("Sniper (SVD)", SlotType.SNP);
			generatedMap.put("Sprengstoffexperte", SlotType.CE);
			generatedMap.put("UAV-Operator (Crusher UGV)", SlotType.MIO);
			generatedMap.put("Senior Rifleman", SlotType.RFL);
			generatedMap.put("UAV-Operator (Tayran AR-2)", SlotType.MIO);
			generatedMap.put("Senior Soldat", SlotType.RFL);
			generatedMap.put("Geisel (General des russischen Innenministeriums)", SlotType.ZC_PLUS);
			generatedMap.put("First Sergeant", SlotType.TL);
			generatedMap.put("Sniper (Rahim)", SlotType.SNP);
			generatedMap.put("Gunner (Zafir)", SlotType.GNR);
			generatedMap.put("Gunner (RPG-42)", SlotType.GNR);
			generatedMap.put("Assistant Sergeant", SlotType.PSG);
			generatedMap.put("Gunner (Titan AA)", SlotType.GNR);
			generatedMap.put("Assistant Teamleader", SlotType.RFL);
			//Generated by SlotParseTool : 80to90.csv
			generatedMap.put("Logistics Teamleader", SlotType.TL);
			generatedMap.put("Team Leader Dagger Actual", SlotType.TL);
			generatedMap.put("Demo Specialist", SlotType.CE);
			generatedMap.put("Ingenieur", SlotType.CE);
			generatedMap.put("Jeep Fahrer", SlotType.DRV);
			generatedMap.put("MG Jeep Schütze", SlotType.AR);
			generatedMap.put("Rebell Kommandant", SlotType.TC);
			generatedMap.put("Rebell Richtschütze", SlotType.GNR);
			generatedMap.put("Rebell Fahrer", SlotType.DRV);
			generatedMap.put("Seargeant", SlotType.PSG);
			generatedMap.put("Gunner (RPG)", SlotType.ATR);
			generatedMap.put("Hand bedienbarer Panzerabwehr-Granatwerfer Schütze", SlotType.GRE);
			generatedMap.put("Deputy Commander", SlotType.TL);
			generatedMap.put("Senior Technician", SlotType.TL);
			generatedMap.put("BM2T Commander", SlotType.TC);
			generatedMap.put("BM2T Machine Gunner", SlotType.GNR);
			generatedMap.put("BM2T Driver/Mechanic", SlotType.DRV);
			generatedMap.put("Anti-Air Assisant", SlotType.AAA);
			generatedMap.put("Leitender Sanitäter", SlotType.TL);
			generatedMap.put("UAV Operator (Darter)", SlotType.MIO);
			generatedMap.put("TPz Gunner", SlotType.GNR);
			generatedMap.put("TPz Driver", SlotType.DRV);
			generatedMap.put("Recon Team Leader", SlotType.TL);
			generatedMap.put("Recon Scout / UAV Operator", SlotType.RFL);
			generatedMap.put("Tank Section Leader (Commander)", SlotType.PL);
			generatedMap.put("Führung", SlotType.TL);
			generatedMap.put("Slot", SlotType.RFL);
			//Generated by SlotParseTool : 90to100.csv
			generatedMap.put("Autmatic Rifleman", SlotType.AR);
			generatedMap.put("TPz Kommandant", SlotType.TC);
			generatedMap.put("TPz Schütze", SlotType.GNR);
			generatedMap.put("TPz Fahrer", SlotType.DRV);
			generatedMap.put("SPz Kommandant", SlotType.TC);
			generatedMap.put("SPz Schütze", SlotType.GNR);
			generatedMap.put("SPz Fahrer", SlotType.DRV);
			generatedMap.put("Light Machinegunner", SlotType.AR);
			generatedMap.put("AA-Spezialist", SlotType.AA);
			generatedMap.put("AT-Spezialist", SlotType.AT);
			generatedMap.put("Special warfare boat operator", SlotType.TL);
			generatedMap.put("SWCC Crewmen (GAU-17/A)", SlotType.MG);
			generatedMap.put("SWCC Crewmen (40 mm Grenade launcher)", SlotType.GRE);
			generatedMap.put("APD Lieutenant", SlotType.PL);
			generatedMap.put("APD Sergeant", SlotType.PSG);
			generatedMap.put("APD Officer (Medic)", SlotType.MDC);
			generatedMap.put("APD Officer", SlotType.RFL);
			generatedMap.put("APD Officer (Marksman)", SlotType.DM);
			generatedMap.put("Rebellenanführer", SlotType.TL);
			generatedMap.put("Rebell (AKMS GL)", SlotType.GRE);
			generatedMap.put("Rebell (Medic)", SlotType.CMDC);
			generatedMap.put("Rebell (AKS74U)", SlotType.RFL);
			generatedMap.put("Rebell (AKM)", SlotType.RFL);
			generatedMap.put("Rebell (Machinegunner)", SlotType.AR);
			generatedMap.put("JTAC (Senior)", SlotType.JTAC);
			generatedMap.put("JTAC (Apprentice)", SlotType.JTAC);
			generatedMap.put("Jetpilot (Senior)", SlotType.WCO);
			generatedMap.put("JetPilot (Senior)", SlotType.WCO);
			generatedMap.put("HelikopterPilot (Senior)", SlotType.PIL);
			generatedMap.put("Commanding Officer/ATC", SlotType.TL);
			generatedMap.put("Crew Chief", SlotType.TL);
			generatedMap.put("Co-Pilot / Notarzt", SlotType.CPIL);
			generatedMap.put("Kampfpilot 1", SlotType.WCO);
			generatedMap.put("Kampfpilot 2", SlotType.WCO);
			generatedMap.put("Kampfpilot 3", SlotType.WCO);
			generatedMap.put("Kampfpilot 4", SlotType.WCO);
			generatedMap.put("WSO 1", SlotType.WSO);
			generatedMap.put("WSO 2", SlotType.WSO);
			generatedMap.put("WSO 3", SlotType.WSO);
			generatedMap.put("WSO 4", SlotType.WSO);
			//Generated by SlotParseTool : 100to110.csv
			generatedMap.put("Radio Operator", SlotType.XO);
			generatedMap.put("AT-Schütze", SlotType.AT);
			generatedMap.put("Apache Pilot -", SlotType.PIL);
			generatedMap.put("Apache Co-Pilot", SlotType.WSO);
			generatedMap.put("Co+ Spieler/Bauer", SlotType.ZC_PLUS);
			generatedMap.put("Co+ Spieler/Hobby-Jäger", SlotType.ZC_PLUS);
			generatedMap.put("Speznas Squad Leader", SlotType.SL);
			generatedMap.put("Speznas Forward Observer", SlotType.FO);
			generatedMap.put("Speznas Forward Air Controller", SlotType.JTAC);
			generatedMap.put("Speznas Machine Gunner (PKP)", SlotType.MG);
			generatedMap.put("Speznas Medic", SlotType.CMDC);
			generatedMap.put("Speznas Grenadier (RPG-7V)", SlotType.GRE);
			generatedMap.put("Speznas Rifleman/Assistant Grenadier", SlotType.RFL);
			generatedMap.put("Speznas Senior Rifleman/Asst. Squad Leader", SlotType.XO);
			generatedMap.put("Speznas Mortar Teamlead", SlotType.TL);
			generatedMap.put("Speznas Mortar Gunner", SlotType.ACSO);
			generatedMap.put("Speznas Mortar Loader", SlotType.RFL);
			generatedMap.put("Frontschütze", SlotType.OTHER);
			generatedMap.put("Rechter Heckschütze", SlotType.OTHER);
			generatedMap.put("Linker Heckschütze", SlotType.OTHER);
			generatedMap.put("Squadleader -", SlotType.SL);
			generatedMap.put("Fireteamleader -", SlotType.FTL);
			generatedMap.put("Rifleman (M136)", SlotType.RFL);
			generatedMap.put("Driver -", SlotType.DRV);
			generatedMap.put("Mortarteam Leader", SlotType.TL);
			generatedMap.put("Mortar Gunner", SlotType.ACSO);
			generatedMap.put("Mortar Loader", SlotType.RFL);
			generatedMap.put("Apache Pilot", SlotType.PIL);
			generatedMap.put("Apache Gunner", SlotType.WSO);
			generatedMap.put("Rifleman/Assistant Grenadier -", SlotType.RFL);
			generatedMap.put("Platoon Radio Operator", SlotType.PSG);
			generatedMap.put("MMG-Schütze", SlotType.MG);
			generatedMap.put("ICOM-Operator", SlotType.PSG);
			generatedMap.put("Section Lead", SlotType.SL);
			generatedMap.put("Team Lead", SlotType.FTL);
			generatedMap.put("Maschinegunner", SlotType.MG);
			generatedMap.put("Asst. Maschinegunner", SlotType.AMG);
			generatedMap.put("Casevac Pilot", SlotType.PIL);
			generatedMap.put("Casevac Door Gunner", SlotType.OTHER);
			generatedMap.put("Casevac Medic", SlotType.MDC);
			generatedMap.put("Wildcat Pilot", SlotType.PIL);
			generatedMap.put("Wildcat Gunner", SlotType.WSO);
			generatedMap.put("Operations Command", SlotType.PL);
			generatedMap.put("Assistend Operations Command", SlotType.PSG);
			generatedMap.put("SF-Anführer", SlotType.FTL);
			generatedMap.put("SF-JTAC", SlotType.JTAC);
			generatedMap.put("SF-Marksmann", SlotType.DM);
			generatedMap.put("SF-Medic", SlotType.CMDC);
			generatedMap.put("SF-Operator", SlotType.RFL);
			generatedMap.put("Panzerzug-Führer", SlotType.PL);
			generatedMap.put("A-10/ Rottenführer", SlotType.WCO);
			generatedMap.put("A-10 Pilot", SlotType.WCO);
			generatedMap.put("Medevac/Pilot", SlotType.PIL);
			generatedMap.put("Medevac/Medic", SlotType.CMDC);
			generatedMap.put("Interpreter (ANA) -", SlotType.RFL);
			generatedMap.put("Assault", SlotType.RFL);
			generatedMap.put("Demolitions", SlotType.CE);
			generatedMap.put("OrdnanceMaintenance", SlotType.CE);
			generatedMap.put("Infantry Logistics", SlotType.LOG);
			//Generated by SlotParseTool : 110to120.csv
			generatedMap.put("Copilot", SlotType.CPIL);
			generatedMap.put("Produzent", SlotType.ZC_PLUS);
			generatedMap.put("Zivilist", SlotType.ZC_PLUS);
			generatedMap.put("Hirte", SlotType.ZC_PLUS);
			generatedMap.put("Bauer", SlotType.ZC_PLUS);
			generatedMap.put("Alim Gurams Seken", SlotType.ZC_PLUS);
			generatedMap.put("Händler", SlotType.ZC_PLUS);
			generatedMap.put("SOF Teamlead (SD)", SlotType.TL);
			generatedMap.put("SOF Forward Observer/JTAC (SD)", SlotType.JTAC);
			generatedMap.put("SOF Medic (SD)", SlotType.CMDC);
			generatedMap.put("Designated Marksman (SD) -", SlotType.DM);
			generatedMap.put("Designated Marksman (SD)", SlotType.DM);
			generatedMap.put("Medic Teamlead", SlotType.TL);
			generatedMap.put("Medic -", SlotType.MDC);
			generatedMap.put("M252 Mortar Teamlead", SlotType.TL);
			generatedMap.put("M252 Mortar Gunner", SlotType.ACSO);
			generatedMap.put("M252 Mortar Loader", SlotType.RFL);
			generatedMap.put("M252 Mortar Ammobear", SlotType.RFL);
			generatedMap.put("Übersetzter", SlotType.ZC_PLUS);
			generatedMap.put("Machiengunner", SlotType.MG);
			generatedMap.put("Asst. Machiengunner", SlotType.AMG);
			generatedMap.put("AT Soldier", SlotType.AT);
			generatedMap.put("Explosives Specialist", SlotType.CE);
			generatedMap.put("Chinook Pilot", SlotType.PIL);
			generatedMap.put("Chinook Co-Pilot", SlotType.CPIL);
			generatedMap.put("MedEvac Pilot", SlotType.PIL);
			generatedMap.put("MedEvac Sanitäter", SlotType.CMDC);
			generatedMap.put("Operationsleitung", SlotType.TL);
			generatedMap.put("Drohnenaufklärung", SlotType.MIO);
			generatedMap.put("Team-Leader", SlotType.TL);
			generatedMap.put("Designated Marksman SD)", SlotType.DM);
			generatedMap.put("M252 Mortar Teamlead -", SlotType.TL);
			generatedMap.put("Harrier Pilot", SlotType.WCO);
			generatedMap.put("Hauptgruppenführer", SlotType.PL);
			generatedMap.put("stellv. Hauptgruppenführer", SlotType.PSG);
			generatedMap.put("PzF3-Schütze", SlotType.ATR);
			generatedMap.put("MG4-Schützen Assi.", SlotType.AMG);
			generatedMap.put("Funker", SlotType.PSG);
			//Generated by SlotParseTool : 120to130.csv
			generatedMap.put("Rifleman M136", SlotType.RFL);
			generatedMap.put("AT-Specialist (SMAW)", SlotType.AT);
			generatedMap.put("Assistant SMAW", SlotType.AAT);
			generatedMap.put("Spieler", SlotType.ZC_PLUS);
			generatedMap.put("Offizier (UAV)", SlotType.MIO);
			generatedMap.put("AT-Rifleman", SlotType.ATR);
			generatedMap.put("Asst. AT-Rifleman", SlotType.AAT);
			generatedMap.put("BTR Driver", SlotType.DRV);
			generatedMap.put("BTR Gunner", SlotType.GNR);
			generatedMap.put("Machine Gunner (RPK-74)", SlotType.MG);
			generatedMap.put("Assistant Squad Leader / Senior Rifleman", SlotType.RFL);
			generatedMap.put("Boss", SlotType.TL);
			generatedMap.put("Handlanger (Medic)", SlotType.CMDC);
			generatedMap.put("Handlanger (Grenadier)", SlotType.GRE);
			generatedMap.put("PMC Squad Leader", SlotType.SL);
			generatedMap.put("PMC Fireteam Leader", SlotType.FTL);
			generatedMap.put("PMC Grenadier", SlotType.GRE);
			generatedMap.put("PMC Automatic Rifleman", SlotType.AR);
			generatedMap.put("PMC MG Gunner", SlotType.MG);
			generatedMap.put("PMC Field Medic", SlotType.CMDC);
			generatedMap.put("PMC Sniper", SlotType.SNP);
			generatedMap.put("PMC Spotter", SlotType.SPT);
			generatedMap.put("Teamlead Alpha", SlotType.FTL);
			generatedMap.put("AT-Javelin Operator", SlotType.AT);
			generatedMap.put("MMG", SlotType.MG);
			generatedMap.put("Teamlead Bravo", SlotType.FTL);
			generatedMap.put("Teamlead Charlie", SlotType.FTL);
			generatedMap.put("AT-MAAWS-Operator", SlotType.AT);
			generatedMap.put("Teamlead Angel", SlotType.TL);
			generatedMap.put("M252 Gunner", SlotType.MG);
			generatedMap.put("M252 Gunner Assistent", SlotType.AMG);
			generatedMap.put("Zellenführer Mohammed", SlotType.PL);
			generatedMap.put("Gruppenführer - Aman", SlotType.SL);
			generatedMap.put("AA-Schütze", SlotType.AA);
			generatedMap.put("Schütze mit Jagdgewehr", SlotType.RFL);
			generatedMap.put("AK-Schütze", SlotType.RFL);
			generatedMap.put("AR-Schütze", SlotType.RFL);
			generatedMap.put("RPG-Schütze", SlotType.AT);
			generatedMap.put("RPG-Hilfsschütze", SlotType.AAT);
			generatedMap.put("US-Soldat", SlotType.RFL);
			generatedMap.put("SEAL Squadlead", SlotType.SL);
			generatedMap.put("SEAL Teamlead", SlotType.TL);
			generatedMap.put("SEAL Saboteur", SlotType.CE);
			generatedMap.put("MAWWS-Operator", SlotType.AT);
			generatedMap.put("SEAL MG-Schütze", SlotType.MG);
			generatedMap.put("Spetznas Anführer", SlotType.TL);
			generatedMap.put("Spetznas", SlotType.RFL);
			generatedMap.put("Arzt", SlotType.MDC);
			generatedMap.put("PKM-Schütze", SlotType.MG);
			generatedMap.put("Spetznas Operator", SlotType.RFL);
			generatedMap.put("Haupttruppenführer", SlotType.TL);
			generatedMap.put("Saboteur", SlotType.CE);
			//Generated by SlotParseTool : 130to140.csv
			generatedMap.put("Platoonleader / OP-Leader", SlotType.PL);
			generatedMap.put("MG-Schütze", SlotType.MG);
			generatedMap.put("Crew Chief (Blackhawk)", SlotType.OTHER);
			generatedMap.put("SMAW-Schütze", SlotType.AT);
			generatedMap.put("Botschafter", SlotType.ZC_PLUS);
			generatedMap.put("Botschaftsmitarbeiter", SlotType.ZC_PLUS);
			generatedMap.put("Pressemitarbeiter", SlotType.ZC_PLUS);
			generatedMap.put("UNO-Wache", SlotType.ZC_PLUS);
			generatedMap.put("Valentin Bosko - Elenas Bruder", SlotType.TL);
			generatedMap.put("Bohumil Hornik", SlotType.RFL);
			generatedMap.put("Miloslav Hornik", SlotType.RFL);
			generatedMap.put("Dominik Kriz", SlotType.RFL);
			generatedMap.put("Lukas Medved", SlotType.RFL);
			generatedMap.put("Kristof Kovac", SlotType.RFL);
			generatedMap.put("Vendelin Sykora", SlotType.RFL);
			generatedMap.put("Ctirad Slavik", SlotType.RFL);
			generatedMap.put("Timotej Cermak", SlotType.RFL);
			generatedMap.put("Bronislav Dolezal", SlotType.RFL);
			generatedMap.put("Interpreter (ANA)", SlotType.ZC_PLUS);
			generatedMap.put("Loader", SlotType.RFL);
			generatedMap.put("SQL", SlotType.SL);
			generatedMap.put("CQB", SlotType.RFL);
			generatedMap.put("Breacher", SlotType.RFL);
			generatedMap.put("GL", SlotType.GRE);
			generatedMap.put("RM", SlotType.RFL);
			generatedMap.put("Überlebender (Fireteamleader)", SlotType.FTL);
			generatedMap.put("Überlebender (MG-Schütze)", SlotType.MG);
			generatedMap.put("Überlebender (Sanitäter)", SlotType.MDC);
			generatedMap.put("Chiropraktiker", SlotType.ZC_PLUS);
			generatedMap.put("ALPHA Squadleader", SlotType.SL);
			generatedMap.put("Rifleman (Combat Medic)", SlotType.CMDC);
			generatedMap.put("Rifleman (Designated Marksman)", SlotType.DM);
			generatedMap.put("Rifleman (AT4)", SlotType.ATR);
			generatedMap.put("BRAVO Squadleader", SlotType.SL);
			generatedMap.put("Gestrandeter 01", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 02", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 03", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 04", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 05", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 06", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 07", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 08", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 09", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 10", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 11", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 12", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 13", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 14", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 15", SlotType.ZC_PLUS);
			generatedMap.put("Gestrandeter 16", SlotType.ZC_PLUS);
			generatedMap.put("?", SlotType.ZC_PLUS);
			generatedMap.put("USMC Squadleader", SlotType.SL);
			generatedMap.put("USMC Operator (GLTDII SOFLAM)", SlotType.JTAC);
			generatedMap.put("USMC Fireteamleader", SlotType.FTL);
			generatedMap.put("USMC Operator (Explosives)", SlotType.CE);
			generatedMap.put("USMC Schütze (M136)", SlotType.ATR);
			generatedMap.put("USMC Medic", SlotType.CMDC);
			generatedMap.put("USMC MG-Schütze", SlotType.MG);
			generatedMap.put("USMC Designated Marksman", SlotType.DM);
			generatedMap.put("USMC Pilot (AH-6J)", SlotType.PIL);
			generatedMap.put("USMC Pilot (Schütze AH-6J)", SlotType.WSO);
			generatedMap.put("Avenger-Schütze", SlotType.WSO);
			generatedMap.put("LKW-Fahrer 1", SlotType.DRV);
			generatedMap.put("LKW-Fahrer 2", SlotType.DRV);
			generatedMap.put("Gasthörer", SlotType.ZC_PLUS);
			//Generated by SlotParseTool : 140to150.csv
			generatedMap.put("Hilfsausbilder", SlotType.PSG);
			generatedMap.put("USMC Radio Operator", SlotType.PSG);
			generatedMap.put("USMC Platoonmedic", SlotType.MDC);
			generatedMap.put("USMC Grenadier", SlotType.GRE);
			generatedMap.put("USMC LMG-Schütze", SlotType.AR);
			generatedMap.put("USMC Sanitäter", SlotType.CMDC);
			generatedMap.put("USMC Hundeführer", SlotType.ZC_PLUS);
			generatedMap.put("Hund", SlotType.ZC_PLUS);
			generatedMap.put("2. Anführer", SlotType.TL);
			generatedMap.put("Jäger", SlotType.ZC_PLUS);
			generatedMap.put("M252 Assistant Gunner", SlotType.AMG);
			generatedMap.put("MAAWS-Specialist", SlotType.AT);
			generatedMap.put("Combat Medic (ehemalig bei Ärzte ohne Grenzen)", SlotType.CMDC);
			generatedMap.put("Pilot Boar 1-1", SlotType.PIL);
			generatedMap.put("Pilot Boar 1-2", SlotType.PIL);
			generatedMap.put("Kartellsoldat (M16A2)", SlotType.RFL);
			generatedMap.put("Kartellsoldat (M16A2+M203)", SlotType.GRE);
			generatedMap.put("Kartellsoldat (AK74M)", SlotType.RFL);
			generatedMap.put("Kartellsoldat (PKM)", SlotType.AR);
			generatedMap.put("Kartellsoldat (RPK74M)", SlotType.RFL);
			generatedMap.put("Sani", SlotType.CMDC);
			generatedMap.put("USMC Squad Leader", SlotType.SL);
			generatedMap.put("USMC Fireteam Leader", SlotType.FTL);
			generatedMap.put("USMC Pionier", SlotType.CE);
			generatedMap.put("USMC Scharfschütze", SlotType.DM);
			generatedMap.put("USMC Nahsicherer", SlotType.RFL);
			generatedMap.put("USMC Pilot", SlotType.PIL);
			generatedMap.put("Warlord", SlotType.CO);
			generatedMap.put("Waffenhändler (El Abib)", SlotType.ZC_PLUS);
			generatedMap.put("MG4 Hilfsschütze", SlotType.AMG);
			generatedMap.put("Panzerfaust Schütze", SlotType.AT);
			generatedMap.put("Panzerfaust Hilfsschütze", SlotType.AAT);
			generatedMap.put("USMC Teamleader", SlotType.FTL);
			generatedMap.put("USMC Designted Markman", SlotType.DM);
			generatedMap.put("USMC Crew Chief", SlotType.RFL);
			generatedMap.put("USMC Doorgunner", SlotType.OTHER);
			//Generated by SlotParseTool : 150to160.csv
			generatedMap.put("USMC Team-Leader", SlotType.TL);
			generatedMap.put("USMC Designted Marksman", SlotType.DM);
			generatedMap.put("Profikommandant", SlotType.TC);
			generatedMap.put("Profifahrer", SlotType.DRV);
			generatedMap.put("Profischütze", SlotType.GNR);
			generatedMap.put("Schützin", SlotType.GNR);
			generatedMap.put("Talibanzellenführer (1. Funke)", SlotType.TL);
			generatedMap.put("Rebel (SKS)", SlotType.RFL);
			generatedMap.put("Rebel (AKM)", SlotType.RFL);
			generatedMap.put("Rebel (SVD, 2. Funke)", SlotType.DM);
			generatedMap.put("Rebel (RPG7)", SlotType.ATR);
			generatedMap.put("Rebel (AKS-74)", SlotType.RFL);
			generatedMap.put("Chiropraktiker (3. Funke)", SlotType.ZC_PLUS);
			generatedMap.put("Rebel (PKM)", SlotType.AR);
			generatedMap.put("MMG Assistent", SlotType.AMG);
			generatedMap.put("Boss Gecko", SlotType.FTL);
			generatedMap.put("PKM Schütze", SlotType.AR);
			generatedMap.put("RPK Schütze", SlotType.AR);
			//Generated by SlotParseTool : 160to170.csv
			generatedMap.put("Avenger Fahrer", SlotType.DRV);
			generatedMap.put("Avenger Schütze", SlotType.GNR);
			generatedMap.put("Reparatur LKW Fahrer", SlotType.LOG);
			generatedMap.put("Treibstoff LKW Fahrer", SlotType.LOG);
			generatedMap.put("PzF-Schütze", SlotType.AT);
			generatedMap.put("OPZ Kampf (Mk16 Mk4 CQ/T) 117/148/343", SlotType.CO);
			generatedMap.put("OPZ Logistik (HK416-D10RS CompM3) 117/148/343", SlotType.COL);
			generatedMap.put("Schütze 1-1 (UMP-45 CCO) 148", SlotType.WSO);
			generatedMap.put("Pilot 1-2 (UMP-45 CCO) 148", SlotType.PIL);
			generatedMap.put("Schütze 2-1 (UMP-45 CCO) 148", SlotType.WSO);
			generatedMap.put("Pilot 2-2 (UMP-45 CCO) 148", SlotType.PIL);
			generatedMap.put("Teamlead (Mk17 EGLM RCO, M136 AT4 CS/RS) 117/343", SlotType.TL);
			generatedMap.put("Operator (SD) (Mk17 CCO SD) 343", SlotType.RFL);
			generatedMap.put("TWS-Marksman (SD) (Mk17 TWS SD) 148/343", SlotType.DM);
			generatedMap.put("Saboteur (Mk17 CCO, Wirecutter) 343", SlotType.CE);
			generatedMap.put("AT-Operator (Mk17 CCO, Mk153 Mod 0 SMAW) 343", SlotType.AT);
			generatedMap.put("MG-Operator (M249 PIP RCO) 343", SlotType.MG);
			generatedMap.put("MG-Assistant-Operator (Mk17 RCO) 343", SlotType.AMG);
			generatedMap.put("Saboteur (SD) (Mk17 CCO SD, Wirecutter) 148/343", SlotType.CE);
			generatedMap.put("Grenadier (SD) (M4A1 M203 CCO SD) 343", SlotType.GRE);
			generatedMap.put("Spotter (Mk17 EGLM Holo) 117/343", SlotType.SPT);
			generatedMap.put("Logistik/Medevac Pilot 1 (UMP-45 CCO) 148", SlotType.LOG);
			generatedMap.put("Logistik/Medevac Pilot 2 (UMP-45 CCO) 148", SlotType.LOG);
			generatedMap.put("Teamleader (HK416-D10RS microCCO) 117/148/343", SlotType.TL);
			generatedMap.put("Crewman 2 (HK416-D10RS microCCO) 148/343", SlotType.RFL);
			generatedMap.put("Crewman 3 (HK416-D10RS microCCO) 148/343", SlotType.RFL);
			generatedMap.put("Crewman 4 (HK416-D10RS microCCO) 148/343", SlotType.RFL);
			generatedMap.put("Air-Medic 1 (Mk16 CQC Holo) 148/343", SlotType.MDC);
			generatedMap.put("Air-Medic 2 (Mk16 CQC Holo) 148/343", SlotType.MDC);
			generatedMap.put("S.W.A.T. Lead", SlotType.TL);
			generatedMap.put("S.W.A.T. Einheit (Breaching)", SlotType.RFL);
			generatedMap.put("S.W.A.T. Einheit (CQC)", SlotType.RFL);
			generatedMap.put("S.W.A.T. Einheit", SlotType.RFL);
			generatedMap.put("S.W.A.T. Einheit (CN Gas)", SlotType.RFL);
			generatedMap.put("S.W.A.T. Sniper", SlotType.SNP);
			generatedMap.put("S.W.A.T. Pilot", SlotType.PIL);
			generatedMap.put("SMAW-Specialist", SlotType.AT);
			generatedMap.put("AntiAir-Specialist", SlotType.AA);
			generatedMap.put("Gefangener (Panzerbesatzung, wird gefoltert)", SlotType.ZC_PLUS);
			generatedMap.put("Plus-Spieler", SlotType.ZC_PLUS);
			generatedMap.put("Stellv. Truppführer", SlotType.XO);
			generatedMap.put("PA-Schütze", SlotType.RFL);
			generatedMap.put("Pilot Phoenix-1", SlotType.PIL);
			generatedMap.put("Sanitäter Phoenix-1", SlotType.MDC);
			generatedMap.put("Machinegunner (Eine Art FTL)", SlotType.MG);
			generatedMap.put("Assistant AT-Gunner", SlotType.AAT);
			generatedMap.put("Sanitätsgruppenleiter", SlotType.TL);
			generatedMap.put("LAV - Schütze", SlotType.GNR);
			generatedMap.put("LAV Fahrer", SlotType.DRV);
			generatedMap.put("Kompaniechef", SlotType.PL);
			generatedMap.put("Fernmelder", SlotType.PSG);
			generatedMap.put("MG4 - Schütze", SlotType.MG);
			generatedMap.put("MG4 - Hilfsschütze", SlotType.AMG);
			generatedMap.put("Ersthelfer/Schütze", SlotType.CMDC);
			generatedMap.put("Pzf3 - Schütze", SlotType.AT);
			generatedMap.put("Pzf3 - Hilfsschütze", SlotType.AAT);
			generatedMap.put("MG3 - Schütze", SlotType.AT);
			generatedMap.put("MG3 - Hilfschütze", SlotType.AAT);
			generatedMap.put("Rottenführer-Pionier", SlotType.CE);
			generatedMap.put("Sturmpionier", SlotType.CE);
			generatedMap.put("Kriegsberichterstatter", SlotType.ZC_PLUS);
			//Generated by SlotParseTool : 170to180.csv
			generatedMap.put("AA-Specialist", SlotType.AA);
			generatedMap.put("Crewman", SlotType.RFL);
			generatedMap.put("HMG-Lead", SlotType.MG);
			generatedMap.put("HMG-Support", SlotType.AMG);
			generatedMap.put("Don Pepperoni", SlotType.CO);
			generatedMap.put("Linke Hand", SlotType.XO);
			generatedMap.put("Lee-Enfield-Schütze", SlotType.DM);
			generatedMap.put("HMG-Schütze", SlotType.MG);
			generatedMap.put("HMG-Support(LMG)", SlotType.AMG);
			generatedMap.put("Machinegunner (LMG)", SlotType.AR);
			generatedMap.put("Engineer", SlotType.CE);
			generatedMap.put("Machinegunner (MMG)", SlotType.MG);
			generatedMap.put("Logistic Leader", SlotType.TL);
			generatedMap.put("Corpsman / Navigator", SlotType.CMDC);
			generatedMap.put("Zug- und Seitenführer", SlotType.TL);
			generatedMap.put("US Artillery Spotter", SlotType.FO);
			generatedMap.put("MG-Assistant", SlotType.AMG);
			generatedMap.put("Lance Corporal", SlotType.PSG);
			generatedMap.put("MG Assistant", SlotType.AMG);
			generatedMap.put("Artillery Commander, Group Leader", SlotType.TL);
			generatedMap.put("Pioneer", SlotType.CE);
			generatedMap.put("Artillery Commander", SlotType.TL);
			generatedMap.put("Koordinator (Basis)", SlotType.TL);
			//Generated by SlotParseTool : 180to190.csv
			generatedMap.put("Platoon RTO", SlotType.XO);
			generatedMap.put("Platoon FO", SlotType.FO);
			generatedMap.put("Vehicle Commander", SlotType.TC);
			generatedMap.put("Vehicle Driver", SlotType.DRV);
			generatedMap.put("Vehicle Gunner", SlotType.GNR);
			generatedMap.put("Logistikführung", SlotType.TL);
			generatedMap.put("Pilot/Fahrzeugkommandant", SlotType.PIL);
			generatedMap.put("Fireteam Leader -", SlotType.FTL);
			generatedMap.put("OPZ", SlotType.CO);
			generatedMap.put("Teamleader Charlie", SlotType.FTL);
			generatedMap.put("Commander Bradley", SlotType.TC);
			generatedMap.put("Pilot Blackhawk (Nachschub und Seals)", SlotType.PIL);
			generatedMap.put("Teamleader Sealteam", SlotType.TL);
			generatedMap.put("Kapitän Landing Craft", SlotType.TL);
			generatedMap.put("Pilot -", SlotType.PIL);
			generatedMap.put("Corpsman (Funker)", SlotType.CMDC);
			generatedMap.put("Platoon Sergant", SlotType.PSG);
			generatedMap.put("Section Commander (Corporal)", SlotType.PL);
			generatedMap.put("Second In Command (Lance-Corporal)", SlotType.PSG);
			generatedMap.put("Crew Chief Captain", SlotType.TL);
			generatedMap.put("Doorgunner (Corpsman)", SlotType.OTHER);
			generatedMap.put("ALPHA - Gruppenführer", SlotType.PL);
			generatedMap.put("ALPHA - Funker", SlotType.PSG);
			generatedMap.put("Speznaz", SlotType.RFL);
			generatedMap.put("Speznaz (RPG 18)", SlotType.ATR);
			generatedMap.put("Speznaz (RPKM + 1P29)", SlotType.AR);
			generatedMap.put("Speznaz (Grenadier, NSPU)", SlotType.GRE);
			generatedMap.put("Speznaz (Grenadier)", SlotType.GRE);
			generatedMap.put("BRAVO - Gruppenführer", SlotType.PL);
			generatedMap.put("BRAVO - Funker", SlotType.PSG);
			generatedMap.put("Speznaz -", SlotType.RFL);
			generatedMap.put("Anti Tank", SlotType.AT);
			generatedMap.put("MG Gunner", SlotType.MG);
			generatedMap.put("Fieldmedic", SlotType.CMDC);
			generatedMap.put("Operator (SD)", SlotType.RFL);
			generatedMap.put("FAC", SlotType.JTAC);
			//Generated by SlotParseTool : 190to200.csv
			generatedMap.put("QRF-Anführer", SlotType.FTL);
			generatedMap.put("Kommandant Marder", SlotType.TC);
			generatedMap.put("Fahrer Marder", SlotType.DRV);
			generatedMap.put("Richtschütze Marder", SlotType.DRV);
			generatedMap.put("PzF-3 Schütze", SlotType.AT);
			generatedMap.put("MG-3 Schütze", SlotType.AT);
			generatedMap.put("Nahsicherer MG-3", SlotType.RFL);
			generatedMap.put("Machinegunner (HMG)", SlotType.MG);
			generatedMap.put("stelvv. Teamleader", SlotType.PSG);
			generatedMap.put("Grendier", SlotType.GRE);
			generatedMap.put("Squad Leader(148) -", SlotType.SL);
			generatedMap.put("Fireteamleader(148)", SlotType.FTL);
			generatedMap.put("Designated Marksman -", SlotType.DM);
			generatedMap.put("Combat Medic(148)", SlotType.CMDC);
			generatedMap.put("Seal-Squadleader", SlotType.SL);
			generatedMap.put("Grenadier/2IC", SlotType.GRE);
			generatedMap.put("Schütze/Squadleadsicherung", SlotType.RFL);
			generatedMap.put("Sanitäter -", SlotType.MDC);
			generatedMap.put("MG-Schütze (MK48)", SlotType.MG);
			generatedMap.put("MG-Schütze 2 (Munischlepper)", SlotType.AMG);
			generatedMap.put("Gangster (AK+GL)", SlotType.GRE);
			generatedMap.put("Gangster (AK)", SlotType.RFL);
			generatedMap.put("Gangster (Pistole)", SlotType.RFL);
			generatedMap.put("Gangster (G36c)", SlotType.RFL);
			generatedMap.put("Gangster (M249)", SlotType.RFL);
			generatedMap.put("Gangster (Vintorez)", SlotType.DM);
			generatedMap.put("Gangster (M1014)", SlotType.RFL);
			generatedMap.put("Geisel (Arzt, dient den Gangstern als Sani)", SlotType.ZC_PLUS);
			generatedMap.put("Geisel (Pilot)", SlotType.PIL);
			//Generated by SlotParseTool : 200to210.csv
			generatedMap.put("Söldner", SlotType.RFL);
			generatedMap.put("Beobachter", SlotType.SPT);
			generatedMap.put("Attentäter", SlotType.SNP);
			generatedMap.put("Minenarbeiter", SlotType.ZC_PLUS);
			generatedMap.put("Offizier (AEK-973/GL)", SlotType.TL);
			generatedMap.put("MG-Schütze (PKP-Pecheng)", SlotType.MG);
			generatedMap.put("LAT-Schütze (AEK-971/RPG7)", SlotType.ATR);
			generatedMap.put("Sanitäter (AEK-971)", SlotType.CMDC);
			generatedMap.put("Luftabwehrspezialist (AEK-971/IGLA)", SlotType.AA);
			generatedMap.put("Team - Anführer (AEK-973/GL)", SlotType.TL);
			generatedMap.put("LMG-Schütze (RPK-74(45Schuss))", SlotType.AR);
			generatedMap.put("Gruppenscharfschütze (SVD)", SlotType.DM);
			generatedMap.put("Sturmpionier (AEK-971/Sprengsätze)", SlotType.CE);
			generatedMap.put("Einsatzleitung", SlotType.CO);
			generatedMap.put("2nd Machinegunner", SlotType.MG);
			generatedMap.put("RTO/FAC", SlotType.XO);
			generatedMap.put("Nahsicherer (M203)", SlotType.RFL);
			generatedMap.put("MG4", SlotType.RFL);
			generatedMap.put("MG Hilfsschütze", SlotType.AMG);
			generatedMap.put("PzF3", SlotType.AT);
			generatedMap.put("PzF Hilfsschütze", SlotType.AAT);
			generatedMap.put("Godfather", SlotType.CO);
			generatedMap.put("Funker und stelv. OPZ", SlotType.XO);
			generatedMap.put("SMAW", SlotType.AT);
			generatedMap.put("Javelin", SlotType.AT);
			generatedMap.put("Commander MBT I", SlotType.TC);
			generatedMap.put("M136", SlotType.RFL);
			generatedMap.put("Medic / Navigator Medevac", SlotType.CMDC);
			generatedMap.put("Pilot CAS", SlotType.WCO);
			generatedMap.put("Konvoiführer", SlotType.TL);
			generatedMap.put("Humvee Schütze", SlotType.GNR);
			generatedMap.put("Humvee Fahrer", SlotType.DRV);
			generatedMap.put("Crewchief (Navigator)", SlotType.RFL);
			generatedMap.put("Anti-Tank Specialist (MAWS)", SlotType.AT);
			generatedMap.put("2nd AT Specialist", SlotType.AT);
			generatedMap.put("Anti-Tank Specialist (MAAWS)", SlotType.AT);
			//Generated by SlotParseTool : 210to220.csv
			generatedMap.put("Batallionskammandeur", SlotType.TL);
			generatedMap.put("Fahrzeugkommandant", SlotType.TC);
			generatedMap.put("MG-Hilfsschütze", SlotType.AMG);
			generatedMap.put("MG-Schütze (MG3)", SlotType.MG);
			generatedMap.put("Panzerabwehrschütze (PzF3)", SlotType.AT);
			generatedMap.put("Luftabwehrspezialist (Stinger)", SlotType.AA);
			generatedMap.put("Aufklärungsoffizier", SlotType.FO);
			generatedMap.put("Sanitätsoffizier", SlotType.MDC);
			generatedMap.put("Versorgungsoffizier", SlotType.MDC);
			generatedMap.put("Nachschub", SlotType.LOG);
			generatedMap.put("PL Radio-Telephone-Operator (RTO)", SlotType.XO);
			generatedMap.put("Mörserführer", SlotType.TL);
			generatedMap.put("Hilfsschütze", SlotType.ACSO);
			generatedMap.put("Munitionsvorbereiter", SlotType.ACSO);
			generatedMap.put("Logistic", SlotType.LOG);
			generatedMap.put("Platoon-Section Sergeant", SlotType.PSG);
			generatedMap.put("Rifleman (L85A2 ULG)", SlotType.RFL);
			generatedMap.put("Rifleman (L110A1 LMG)", SlotType.RFL);
			generatedMap.put("Rifleman (L85A2)", SlotType.RFL);
			generatedMap.put("Combat Medic (L85A2)", SlotType.CMDC);
			generatedMap.put("2IC (Lance Corporal)", SlotType.PL);
			generatedMap.put("Minensucher", SlotType.CE);
			generatedMap.put("RPG-7-Schütze", SlotType.AT);
			generatedMap.put("RPG-7 Munitionsschlepper", SlotType.AAT);
			generatedMap.put("Chiropraktiker (Sani)", SlotType.MDC);
			generatedMap.put("W-Instrukteur", SlotType.TL);
			generatedMap.put("Kommandeur", SlotType.TC);
			generatedMap.put("Hauptschütze", SlotType.GNR);
			generatedMap.put("ALPHA Squad Leader", SlotType.SL);
			generatedMap.put("BRAVO Squad Leader", SlotType.SL);
			generatedMap.put("CHARLIE Weapon Squad Leader", SlotType.SL);
			generatedMap.put("Assistant Machine Gunner", SlotType.AMG);
			generatedMap.put("AT4 Gunner", SlotType.AT);
			generatedMap.put("Assistant AT4 Gunner", SlotType.AAT);
			generatedMap.put("Tower", SlotType.LOG);
			//Generated by SlotParseTool : 220to230.csv
			generatedMap.put("Pilot Apache", SlotType.PIL);
			generatedMap.put("Gunner Apache", SlotType.WSO);
			generatedMap.put("Commander KPz1", SlotType.TC);
			generatedMap.put("Teamlead Covert Ops", SlotType.PL);
			generatedMap.put("Offizier Stellv. Opz", SlotType.PSG);
			generatedMap.put("General", SlotType.TL);
			generatedMap.put("2. MG Schütze", SlotType.MG);
			generatedMap.put("Roleplayer", SlotType.ZC_PLUS);
			generatedMap.put("Rifleman (Lee Enfield)", SlotType.DM);
			generatedMap.put("Rifleman (AK74)", SlotType.RFL);
			generatedMap.put("Marksman (SVD Bipod)", SlotType.DM);
			generatedMap.put("Zivile Bevölkerung", SlotType.ZC_PLUS);
			generatedMap.put("Soldier (M136)", SlotType.RFL);
			generatedMap.put("Combat Medic -", SlotType.CMDC);
			generatedMap.put("Kommandant M1A2", SlotType.TC);
			generatedMap.put("Panzerzugführer T90A", SlotType.PL);
			generatedMap.put("Kommandant T90A", SlotType.TC);
			generatedMap.put("Squadleader Stellv", SlotType.PSG);
			generatedMap.put("Angriffstrupp-Anführer", SlotType.TL);
			generatedMap.put("Leader Alpha One", SlotType.FTL);
			generatedMap.put("Leader Alpha Two", SlotType.FTL);
			generatedMap.put("Leader Alpha Three", SlotType.FTL);
			generatedMap.put("Pilot Eagle", SlotType.PIL);
			generatedMap.put("Seiten MG", SlotType.OTHER);
			generatedMap.put("Heck MG", SlotType.OTHER);
			generatedMap.put("Maschinengewehr (mk 48 mod 0)", SlotType.MG);
			generatedMap.put("AT (SMAW)", SlotType.AT);
			generatedMap.put("Humvee Fahrer / Rifleman", SlotType.DRV);
			generatedMap.put("Humvee Fahrer / Medic", SlotType.DRV);
			generatedMap.put("LKW Fahrer / Rifleman", SlotType.DRV);
			generatedMap.put("Little-Bird (Bewaffnet) Co-Pilot", SlotType.WSO);
			generatedMap.put("El Präsidente", SlotType.ZC_PLUS);
			//Generated by SlotParseTool : 230to240.csv
			generatedMap.put("Automatic Rifleman (LMG)", SlotType.AR);
			generatedMap.put("Anti-Tank Soldier (SMAW)", SlotType.AT);
			generatedMap.put("Anti-Tank Soldier (Javelin)", SlotType.AT);
			generatedMap.put("Teamleitung", SlotType.TL);
			generatedMap.put("Panzerabwehrspezialist", SlotType.AT);
			generatedMap.put("Support-Teamleitung", SlotType.TL);
			generatedMap.put("BTR70 - Kommandant", SlotType.TC);
			generatedMap.put("BTR70 - Schütze", SlotType.GNR);
			generatedMap.put("BTR70 - Fahrer", SlotType.DRV);
			generatedMap.put("AT MAAWS", SlotType.AT);
			generatedMap.put("CBT Medic", SlotType.CMDC);
			generatedMap.put("PzF3 Schütze", SlotType.AT);
			generatedMap.put("Stinger Schütze", SlotType.AA);
			generatedMap.put("Driver - Rifleman (LKW)", SlotType.DRV);
			generatedMap.put("Grenadier/Stellv.", SlotType.GRE);
			generatedMap.put("Zweiter MG-Schütze", SlotType.MG);
			generatedMap.put("Angriffstruppführer", SlotType.FTL);
			generatedMap.put("Panzerabwehrschütze", SlotType.AT);
			generatedMap.put("Policeman", SlotType.RFL);
			generatedMap.put("AT Schütze", SlotType.AT);
			generatedMap.put("Fire Team Leader Alpha 2", SlotType.FTL);
			generatedMap.put("AA Schütze", SlotType.AA);
			generatedMap.put("Fire Team Leader Alpha 3", SlotType.FTL);
			generatedMap.put("Javelin Schütze", SlotType.AA);
			generatedMap.put("Javelin Schlepper", SlotType.AAA);
			//Generated by SlotParseTool : 240to250.csv
			generatedMap.put("Squad-Leader", SlotType.SL);
			generatedMap.put("Geisel (Polizist)", SlotType.ZC_PLUS);
			generatedMap.put("Trupp-Anführer", SlotType.TL);
			generatedMap.put("IAR-Schütze", SlotType.AR);
			generatedMap.put("Apache - Pilot", SlotType.PIL);
			generatedMap.put("Apache - Schütze", SlotType.WSO);
			generatedMap.put("Rollkontrolle", SlotType.OTHER);
			generatedMap.put("AT-Schütze (SMAW)", SlotType.AT);
			generatedMap.put("LMG Soldier", SlotType.AR);
			generatedMap.put("ECM Operator", SlotType.CE);
			generatedMap.put("EOD Sniper", SlotType.CE);
			generatedMap.put("EOD Dog", SlotType.CE);
			generatedMap.put("Spotter Alpha", SlotType.SPT);
			generatedMap.put("Sniper Alpha", SlotType.SNP);
			generatedMap.put("Spotter Bravo", SlotType.SPT);
			generatedMap.put("Sniper Bravo", SlotType.SNP);
			generatedMap.put("Spotter Charlie", SlotType.SPT);
			generatedMap.put("Sniper Charlie", SlotType.SNP);
			generatedMap.put("EOD-Truppführer", SlotType.TL);
			//Generated by SlotParseTool : 250to255.csv
			generatedMap.put("Commander MBT", SlotType.TC);
			generatedMap.put("Javelin Support", SlotType.AA);
			generatedMap.put("Commander KPz", SlotType.TC);
			generatedMap.put("Commander  KPz2", SlotType.TC);
			generatedMap.put("Fire Team Leader / OPZ", SlotType.FTL);
			generatedMap.put("Cougar - Trupp-Anführer", SlotType.TL);
			generatedMap.put("Cougar - Grenadier", SlotType.GRE);
			generatedMap.put("Cougar - LMG", SlotType.MG);
			generatedMap.put("Cougar - AT4", SlotType.AT);
			generatedMap.put("Cougar - Angriffstrupp Anführer", SlotType.FTL);
			generatedMap.put("Cougar - Mörserleitschütze", SlotType.ACSO);
			generatedMap.put("Cougar - Mörserlitschütze", SlotType.ACSO);
			generatedMap.put("Cougar - Mörserschütze -", SlotType.ACSO);
			generatedMap.put("Medevac - Pilot", SlotType.PIL);
			generatedMap.put("Logistikpilot", SlotType.PIL);
			generatedMap.put("Logistikhilfe", SlotType.LOG);
			
			for (Entry<String, SlotType> entry : generatedMap.entrySet()) {
				if (slotText.toLowerCase().equals(entry.getKey().toLowerCase())) {
					slotTypeFound = true;
					slot = entry.getValue();
					break;
				}
			}
		}
		
		if (slot == SlotType.NO_TYPE) {
			System.err.println("Can not parse slot type out of: " + slotText);
		}
		
		return slot;
	}
	/**
	 * Gets the events name by extracting it from the thread title.
	 * @param title Title of the thread
	 * @return Name of the event or the title if failure occurred
	 */
	private static String getThreadName(String title) {
		String name = "";
		
		//Work trough exceptions
		if (title.trim().contains("[S-PvP 21] - 30.09.2012 - 19 Uhr")) {
			name = "Der Prototyp";
			return name;
		} else if (title.trim().contains("[20.04] Co37 - (OP) Red Tsunami")) {
			name = "Red Tsunami";
			return name;
		} else if (title.trim().contains("Co54 Desert Storm - 21.07.2012 1900h - Spezial Slots")) {
			name = "Desert Storm";
			return name;
		} else if (title.trim().contains("Übung: Sniper und Spotter")) {
			name = "Sniper und Spotter";
			return name;
		} else if (title.trim().contains("Spontanevent!!! 8.6.2012 - TvT Bridgefight")) {
			name = "Bridgefight";
			return name;
		} else if (title.trim().contains("[26.04.2013] - Operation Seelandung - Tag 1")) {
			name = "Operation Seelandung - Tag 1";
			return name;
		} else if (title.trim().contains("Eventeinladung: Brigade 2010")) {
			name = "Eventeinladung: Brigade 2010";
			return name;
		} else if (title.trim().contains("[02. März] Dessert Op Part 1 COOP 45")) {
			name = "Dessert Op Part 1";
			return name;
		} else if (title.trim().contains("[29. Dez] 3.JgKp Operation Frozen Thunder")) {
			name = "Operation Frozen Thunder";
			return name;
		} else if (title.trim().contains("Brig2010 Event")) {
			name = "Brig2010 Event";
			return name;
		} else if (title.trim().contains("[23. August] CO36 Operation Red Overload - Tag 1")) {
			name = "Operation Red Overload - Tag 1";
			return name;
		} else if (title.trim().contains("1.6. Event: Coop 15 - Baker Post EP1")) {
			name = "Baker Post EP1";
			return name;
		} else if (title.trim().contains("[Coop24] 26. Juli Sex, drugs and Guns")) {
			name = "Sex, drugs and Guns";
			return name;
		}
		
		//Extract name from title
		boolean found = false;
		//Extract size from title
		Pattern pattern = Pattern.compile("[A-Za-züäöÜÄÖ\\s\\+ß]+[\\d]{1,2}[\\s]+[-]?[\\s]{0,2}([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)$");
		Matcher matcher = pattern.matcher(title);
		if (matcher.find()) {
			found = true;
			name = matcher.group(1).trim();
		}
		if (!found) {
			pattern = Pattern.compile("[\"']([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)[\"']");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("[A-Za-züäöÜÄÖ\\s\\+ß]+[\\d]{1,2}[\\s]+[-]?[\\s]{0,2}([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)[\\d]{0,2}[vV][\\.]?[\\d]{1,2}$");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("- ([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)[\\s]?[\\[,]");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("[A-Za-züäöÜÄÖ\\s\\+]+[\\d]{1,2}[\\s]+[-]?[\\s]{0,2}([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)[\\d]\\.[\\d]$");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("[A-Za-züäöÜÄÖ\\s\\+]+[\\d]{1,2}[\\s]+[-]?[\\s]{0,2}([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)\\(");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("- ([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)$");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("^([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)$");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("[A-Za-züäöÜÄÖ\\s\\+-]+[\\d]{1,2}[\\s]+[-]?[\\s]{0,2}([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)\\[");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\] ([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)$");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("- ([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+) - [\\d]");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("[\\d]+[\\s]+([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)$");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("[A-Za-züäöÜÄÖ\\s\\+]+[\\d]{1,2}[\\s]+[-]?[\\s]{0,2}([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)[\\d]+$");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("[\\d]+ ([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)$");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("[\\d]+[\\.]? ([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)\\[");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("[\\d]+[\\.]? ([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+) - [\\d]");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("[\\d]+[\\.]? ([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)[\\d]+");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		if (!found) {
			pattern = Pattern.compile("[\\d]+[\\.]? ([A-Za-züäöÜÄÖ\\s-',\\.!:ß]+)$");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				name = matcher.group(1).trim();
			}
		}
		
		if (!found) {
			System.err.println("Can't parse thread name from title (using title instead): " + title);
			name = title;
		}
		
		return name;
	}
	/**
	 * Gets the map the event takes place at by
	 * extracting it from the event thread web content.
	 * @param content Content of the event threads web site
	 * @param curContentIndex Current index in the content which
	 * should be placed near starting of the true content
	 * @return Name of the map the event takes place at or {@link MAP_UNKNOWN} if not known
	 */
	private static String getThreadMap(List<String> content, int curContentIndex) {
		String map = MAP_UNKNOWN;
		int i = curContentIndex;
		String line = "";
		
		//Search for map until content end
		Pattern pattern;
		Matcher matcher;
		do {
			i++;
			line = content.get(i);
			pattern = Pattern.compile("((Map)|(Karte))[\\s]?:[\\s]?(.+)<", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(line);
			if (matcher.find()
					&& !matcher.group(4).trim().toUpperCase().contains(MAP_REJECT.toUpperCase())) {
				map = matcher.group(4);
				break;
			}
		} while (!line.contains(THREAD_CONTENT_END));
		
		map = map.replaceAll("<strong>", "");
		map = map.replaceAll("</strong>", "");
		map = map.trim();
		
		if (map.equals("Imrali-Island") || map.equals("Imrali")) {
			map = "Imrali Island";
		} else if (map.equals("Sangin")) {
			map = "Summer Sangin";
		} else if (map.equals("Panthera")) {
			map = "Island Panthera";
		} else if (map.equals("Aliabad Region") || map.equals("Aliabat Region")) {
			map = "Aliabad";
		} else if (map.equals("Clafgan")) {
			map = "Clafghan";
		} else if (map.equals("<strong>Clafghan</strong>")) {
				map = "Clafghan";
		} else if (map.equals("Duala")) {
			map = "Isla Duala";
		} else if (map.equals("Chernarus (Summer)")) {
			map = "Chernarus Summer";
		} else if (map.equals("Podagorsk")) {
			map = "FDF Podagorsk";
		} else if (map.equals("Capraia")) {
			map = "Isola di Capraia";
		}
		
		return map;
	}
	/**
	 * Gets the events thread id in the forum by extracting it from its url.
	 * @param url Url to the event thread
	 * @return Id of events thread or {@link NO_ID} if an error occurred
	 */
	private static int getThreadId(String url) {
		int id = NO_ID;
		//Extract id from url
		Pattern pattern = Pattern.compile("id=([\\d]+)");
		Matcher matcher = pattern.matcher(url);
		if (matcher.find()) {
			id = Integer.parseInt(matcher.group(1));
		} else {
			System.err.println("Can't parse thread id from url: " + url);
		}
		return id;
	}
	/**
	 * Gets the events size by extracting it from its title.
	 * @param title Title of the event
	 * @return Size of the events or {@link NO_SIZE} if failure occurred
	 */
	private static int getEventSize(String title) {
		int size = NO_SIZE;
		
		//Work trough exceptions
		if (title.trim().contains("[11.05] -1730 - Häuserkampf")) {
			size = 27;
			return size;
		} else if (title.trim().contains("[16.04.] Close Air Support")) {
			size = 11;
			return size;
		} else if (title.trim().contains("[15.04.] Close Air Support")) {
			size = 11;
			return size;
		} else if (title.trim().contains("Mini Sylvester Event")) {
			size = 20;
			return size;
		} else if (title.trim().contains("[03.10.] 19:00 - Mörsertraining")) {
			size = 21;
			return size;
		} else if (title.trim().contains("[30.09] 20:00   Häuserkampf Theorie und Praxis")) {
			size = 22;
			return size;
		} else if (title.trim().contains("[23.09.] - 1900 - JTAC-Training")) {
			size = 13;
			return size;
		} else if (title.trim().contains("[19.09.] 1900 - Panzertraining")) {
			size = 24;
			return size;
		} else if (title.trim().contains("[15.09.] - 1900 - Vortrag über Sprengstoffe")) {
			size = 21;
			return size;
		} else if (title.trim().contains("[09.09] ORGA X - Das Squad und Führung im Allgemeinen")) {
			size = 27;
			return size;
		} else if (title.trim().contains("[26.04.2013] - Operation Seelandung - Tag 1")) {
			size = 40;
			return size;
		} else if (title.trim().contains("Eventeinladung: Brigade 2010")) {
			size = 9;
			return size;
		} else if (title.trim().contains("[16.02.] Grantiger Löwe")) {
			size = 9;
			return size;
		} else if (title.trim().contains("[29. Dez] 3.JgKp Operation Frozen Thunder")) {
			size = 6;
			return size;
		} else if (title.trim().contains("[28.12.12] - 20:00 - Operation Godfather XII")) {
			size = 39;
			return size;
		} else if (title.trim().contains("Brig2010 Event")) {
			size = 10;
			return size;
		} else if (title.trim().contains("[23.10.2012] CoX - SMK Bewegungsausbildung")) {
			size = 18;
			return size;
		} else if (title.trim().contains("13.10.12 Time is Running")) {
			size = 33;
			return size;
		} else if (title.trim().contains("[29. Aug] TvCoop - Die Attentäter")) {
			size = 27;
			return size;
		} else if (title.trim().contains("[TvT-event] 27.07. Dark Business [15vs13+2]")) {
			size = 30;
			return size;
		} else if (title.trim().contains("Übung: Sniper und Spotter")) {
			size = 8;
			return size;
		} else if (title.trim().contains("12.06. Ein Tag auf Patrouille")) {
			size = 17;
			return size;
		} else if (title.trim().contains("Spontanevent!!! 8.6.2012 - TvT Bridgefight")) {
			size = 20;
			return size;
		} else if (title.trim().contains("05.06.2012 - Sniperduell")) {
			size = 10;
			return size;
		}
		
		boolean found = false;
		//Extract size from title
		Pattern pattern = Pattern.compile("[A-Za-z]+[\\+\\s]?(\\d\\d)[\\s\\]]");
		Matcher matcher = pattern.matcher(title);
		if (matcher.find()) {
			found = true;
			size = Integer.parseInt(matcher.group(1));
		}
		if (!found) {
			pattern = Pattern.compile("[A-Za-z]{2}\\+ (\\d\\d)\\s");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				size = Integer.parseInt(matcher.group(1));
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\s[A-Za-z]{2}(\\d\\d)");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				size = Integer.parseInt(matcher.group(1));
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\s[A-Za-z]{4}[\\s]?-[\\s]?(\\d\\d)");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				size = Integer.parseInt(matcher.group(1));
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\s[A-Za-z]{2}(\\d)\\s");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				size = Integer.parseInt(matcher.group(1));
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\s[A-Za-z]{4}(\\d)\\s");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				size = Integer.parseInt(matcher.group(1));
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\s[A-Za-z]{2}\\s(\\d)\\s");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				size = Integer.parseInt(matcher.group(1));
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\s[A-Za-z]{4}\\s(\\d\\d)");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				size = Integer.parseInt(matcher.group(1));
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\s[A-Za-z]{3}(\\d\\d)\\+");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				size = Integer.parseInt(matcher.group(1));
			}
		}
		
		if (!found || size == NO_SIZE) {
			System.err.println("Can't parse event size from title: " + title);
		}
		
		return size;
	}
	/**
	 * Gets the events type by extracting it from its title.
	 * @param title Title of the event
	 * @return Size of the event or {@link EventType.NO_TYPE} if failure occurred
	 */
	private static EventType getEventType(String title) {
		EventType type = EventType.NO_TYPE;
		
		//Work trough exceptions
		if (title.trim().contains("[11.05] -1730 - Häuserkampf")) {
			type = EventType.ORGA;
			return type;
		} else if (title.trim().contains("[16.04.] Close Air Support")) {
			type = EventType.ORGA;
			return type;
		} else if (title.trim().contains("[15.04.] Close Air Support")) {
			type = EventType.ORGA;
			return type;
		} else if (title.trim().contains("Mini Sylvester Event")) {
			type = EventType.BLACKBOX;
			return type;
		} else if (title.trim().contains("[26.04.2013] - Operation Seelandung - Tag 1")) {
			type = EventType.COOP;
			return type;
		} else if (title.trim().contains("Eventeinladung: Brigade 2010")) {
			type = EventType.COOP;
			return type;
		} else if (title.trim().contains("[16.02.] Grantiger Löwe")) {
			type = EventType.COOP;
			return type;
		} else if (title.trim().contains("[29. Dez] 3.JgKp Operation Frozen Thunder")) {
			type = EventType.COOP;
			return type;
		} else if (title.trim().contains("[28.12.12] - 20:00 - Operation Godfather XII")) {
			type = EventType.COOP;
			return type;
		} else if (title.trim().contains("Brig2010 Event")) {
			type = EventType.COOP;
			return type;
		} else if (title.trim().contains("13.10.12 Time is Running")) {
			type = EventType.COOP;
			return type;
		} else if (title.trim().contains("[29. Aug] TvCoop - Die Attentäter")) {
			type = EventType.COOP_PLUS;
			return type;
		} else if (title.trim().contains("12.06. Ein Tag auf Patrouille")) {
			type = EventType.COOP;
			return type;
		} else if (title.trim().contains("05.06.2012 - Sniperduell")) {
			type = EventType.TVT;
			return type;
		} else if (title.trim().contains("[23.10.2012] CoX - SMK Bewegungsausbildung")) {
			type = EventType.ORGA;
			return type;
		}
		
		boolean found = false;
		//Extract type from title
		Pattern pattern = Pattern.compile("((CO)|(COOP))[\\s]?[\\d]", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(title);
		if (matcher.find()) {
			found = true;
			type = EventType.COOP;
		}
		if (!found) {
			pattern = Pattern.compile("((CO)|(COOP))\\+[\\s]?[\\d]", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				type = EventType.COOP_PLUS;
			}
		}
		if (!found) {
			pattern = Pattern.compile("(TVT[\\s\\+]{0,2}[\\d])|(TVT-EVENT)|(TVT [A-Za-z])|(S-PVP)",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				type = EventType.TVT;
			}
		}
		if (!found) {
			pattern = Pattern.compile("((BB)|(BLACKBOX))[\\s]?[\\d]", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				type = EventType.BLACKBOX;
			}
		}
		if (!found) {
			pattern = Pattern.compile("COMP[\\s]?[\\d]", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				type = EventType.COMPETITION;
			}
		}
		if (!found) {
			pattern = Pattern.compile("((MILSIM)|(MIL)|(MILSIM\\+))[\\s]?[\\d]", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				type = EventType.MILSIM;
			}
		}
		if (!found) {
			pattern = Pattern.compile("(ORG[A\\s-]{0,4}[\\dX])|(TRAINING)|(ÜBUNG)|(THEORIE)|(VORTRAG)",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				type = EventType.ORGA;
			}
		}
		
		if (!found) {
			System.err.println("Can't parse event type from title: " + title);
		}
		
		return type;
	}
	/**
	 * Gets the time when the event has started by extracting
	 * it from the content.
	 * @param content Content of events thread web site
	 * @param curContentIndex Current index of the content which
	 * should be placed near threads title
	 * @param title Title of the event
	 * @return Time when the event has started or null if an error occurred
	 */
	private static Calendar getEventTime(List<String> content, int curContentIndex, String title) {
		String time = null;
		int i = curContentIndex;
		String line = "";
		
		//Search for time until content end
		Pattern pattern;
		Matcher matcher;
		do {
			i++;
			line = content.get(i);
			line = line.replaceAll("â€", "-");
			String beforeTimePattern = "^[\\s]*(<(strong|i)>)?(Eventbeginn|Beginn|Eventstart|"
					+ "Treffen im (Teamspeak|TS)|Start|Treffen)(<\\/(strong|i)>)?:(<\\/(strong|i)>)?";
			String timePattern = "([0-9]{2}[\\.:]?[0-9]{2})";
			pattern = Pattern.compile(beforeTimePattern + "(\\s|&gt;|\\-|ab)*(<strong>)?"
					+ timePattern
					+ "[\\s]*(Uhr|h)?(<\\/strong>)?(\\s|&lt;)*<br[\\s]?\\/>", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(line);
			if (matcher.find()) {
				time = matcher.group(11).replaceAll("[\\.:]", "");
				break;
			}
		} while (!line.contains(THREAD_CONTENT_END));
		
		if (time == null) {
			System.err.println("Can not parse starting time of event: " + title);
		} else {
			time = time.substring(0, 2) + ":" + time.substring(2) + ":00";
		}
		
		return CrawlerUtil.convertStringToTime(time);
	}
	/**
	 * Gets the date when the event took place at by extracting
	 * it from its title and by using the year of thread creation.
	 * @param title Title of the event
	 * @param content Content of events thread web site
	 * @param curContentIndex Current index of the content which
	 * should be placed near threads title
	 * @return Date when the event took place at or null if an error occurred
	 */
	private static Calendar getEventDate(String title, List<String> content, int curContentIndex) {
		//Get date
		boolean found = false;
		String date = null;
		
		//Work trough exceptions
		if (title.trim().contains("Mini Sylvester Event")) {
			date = "31.12.2013";
			return CrawlerUtil.convertStringToDate(date);
		} else if (title.trim().contains("Eventeinladung: Brigade 2010")) {
			date = "23.03.2013";
			return CrawlerUtil.convertStringToDate(date);
		} else if (title.trim().contains("Brig2010 Event")) {
			date = "22.12.2012";
			return CrawlerUtil.convertStringToDate(date);
		} else if (title.trim().contains("[Coop] CO22 BAF EOD Patrol")) {
			date = "01.07.2012";
			return CrawlerUtil.convertStringToDate(date);
		} else if (title.trim().contains("Übung: Sniper und Spotter")) {
			date = "26.06.2012";
			return CrawlerUtil.convertStringToDate(date);
		} else if (title.trim().contains("[Coop] CO24 Sex, Drugs and Guns")) {
			date = "24.06.2012";
			return CrawlerUtil.convertStringToDate(date);
		} else if (title.trim().contains("[09.01] Co29 Restrepo")) {
			date = "09.01.2015";
			return CrawlerUtil.convertStringToDate(date);
		} else if (title.trim().contains("[10.01] TvT 40 Riot")) {
			date = "10.01.2015";
			return CrawlerUtil.convertStringToDate(date);
		} else if (title.trim().contains("[12.01.] Co33 - Der Nachschub")) {
			date = "12.01.2013";
			return CrawlerUtil.convertStringToDate(date);
		} else if (title.trim().contains("[23.10.2012] CoX - SMK Bewegungsausbildung")) {
			date = "24.10.2012";
			return CrawlerUtil.convertStringToDate(date);
		}
		
		//Replace months with correct date
		title = title.replaceAll("[\\s]?(Januar)[\\.]?", "01");
		title = title.replaceAll("[\\s]?(Februar)[\\.]?", "02");
		title = title.replaceAll("[\\s]?(März)[\\.]?", "03");
		title = title.replaceAll("[\\s]?(April)[\\.]?", "04");
		title = title.replaceAll("[\\s]?(Mai)[\\.]?", "05");
		title = title.replaceAll("[\\s]?(Juni)[\\.]?", "06");
		title = title.replaceAll("[\\s]?(Juli)[\\.]?", "07");
		title = title.replaceAll("[\\s]?(August)[\\.]?", "08");
		title = title.replaceAll("[\\s]?(September)[\\.]?", "09");
		title = title.replaceAll("[\\s]?(Oktober)[\\.]?", "10");
		title = title.replaceAll("[\\s]?(November)[\\.]?", "11");
		title = title.replaceAll("[\\s]?(Dezember)[\\.]?", "12");
		title = title.replaceAll("[\\s]?(Jan)[\\.]?", "01");
		title = title.replaceAll("[\\s]?(Feb)[\\.]?", "02");
		title = title.replaceAll("[\\s]?(Mär)[\\.]?", "03");
		title = title.replaceAll("[\\s]?(Apr)[\\.]?", "04");
		title = title.replaceAll("[\\s]?(Mai)[\\.]?", "05");
		title = title.replaceAll("[\\s]?(Jun)[\\.]?", "06");
		title = title.replaceAll("[\\s]?(Jul)[\\.]?", "07");
		title = title.replaceAll("[\\s]?(Aug)[\\.]?", "08");
		title = title.replaceAll("[\\s]?(Sep)[\\.]?", "09");
		title = title.replaceAll("[\\s]?(Okt)[\\.]?", "10");
		title = title.replaceAll("[\\s]?(Nov)[\\.]?", "11");
		title = title.replaceAll("[\\s]?(Dez)[\\.]?", "12");
		
		//Extract date from title
		Pattern pattern = Pattern.compile("\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d");
		Matcher matcher = pattern.matcher(title);
		if (matcher.find()) {
			found = true;
			date = title.substring(matcher.start(), matcher.end());
			//15.03.2014
		}
		if (!found) {
			pattern = Pattern.compile("[^\\d]\\d\\.\\d\\.\\d\\d\\d\\d");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start() + 1, matcher.end());
				//7.2.2014
				date = "0" + date.substring(0, date.length() - 6) + "0" + date.substring(date.length() - 6);
				//07.02.2014
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\d\\d\\.\\d\\d\\.\\d\\d");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start(), matcher.end());
				//15.03.14
				date = date.substring(0, date.length() - 2) + DATE_YEAR_PRE + date.substring(date.length() - 2);
				//15.03.2014
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\d\\.\\d\\d\\.\\d\\d\\d\\d");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start(), matcher.end());
				//3.03.2014
				date = "0" + date;
				//03.03.2014
			}
		}
		if (!found) {
			pattern = Pattern.compile("[^\\d]\\d\\.\\d\\d\\.");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start() + 1, matcher.end());
				//3.03.
				date = "0" + date;
				String wroteAt = content.get(curContentIndex + THREAD_TITLE_OFFSET_DATE);
				pattern = Pattern.compile("\\d\\d\\d\\d");
				matcher = pattern.matcher(wroteAt);
				if (matcher.find()) {
					date += wroteAt.substring(matcher.start(), matcher.end());
					//03.03.2014
				} else {
					System.err.println("Can't read 'wroteAt' date from thread.");
				}
			}
		}
		if (!found) {
			pattern = Pattern.compile("[^\\d\\.]\\d\\.\\d\\d[^\\d]");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start() + 1, matcher.end() - 1);
				//3.03
				date = "0" + date;
				String wroteAt = content.get(curContentIndex + THREAD_TITLE_OFFSET_DATE);
				pattern = Pattern.compile("\\.\\d\\d\\d\\d");
				matcher = pattern.matcher(wroteAt);
				if (matcher.find()) {
					date += wroteAt.substring(matcher.start(), matcher.end());
					//03.03.2014
				} else {
					System.err.println("Can't read 'wroteAt' date from thread.");
				}
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\d\\d\\.\\d\\d[^\\.]");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start(), matcher.end() - 1);
				//15.03
				String wroteAt = content.get(curContentIndex + THREAD_TITLE_OFFSET_DATE);
				pattern = Pattern.compile("\\.\\d\\d\\d\\d");
				matcher = pattern.matcher(wroteAt);
				if (matcher.find()) {
					date += wroteAt.substring(matcher.start(), matcher.end());
					//15.03.2014
				} else {
					System.err.println("Can't read 'wroteAt' date from thread.");
				}
			}
		}
		if (!found) {
			pattern = Pattern.compile("[^\\.]\\d\\d\\.\\d\\d\\.");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start() + 1, matcher.end());
				//15.03.
				String wroteAt = content.get(curContentIndex + THREAD_TITLE_OFFSET_DATE);
				pattern = Pattern.compile("\\d\\d\\d\\d");
				matcher = pattern.matcher(wroteAt);
				if (matcher.find()) {
					date += wroteAt.substring(matcher.start(), matcher.end());
					//15.03.2014
				} else {
					System.err.println("Can't read 'wroteAt' date from thread.");
				}
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\d\\d\\.\\d\\d\\.[^\\d]");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start(), matcher.end() - 1);
				//15.03.
				String wroteAt = content.get(curContentIndex + THREAD_TITLE_OFFSET_DATE);
				pattern = Pattern.compile("\\d\\d\\d\\d");
				matcher = pattern.matcher(wroteAt);
				if (matcher.find()) {
					date += wroteAt.substring(matcher.start(), matcher.end());
					//15.03.2014
				} else {
					System.err.println("Can't read 'wroteAt' date from thread.");
				}
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\d\\d\\.\\d[^\\d\\.]");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start(), matcher.end() - 1);
				//15.3
				date = date.substring(0, date.length() - 1) + "0" + date.substring(date.length() - 1);
				String wroteAt = content.get(curContentIndex + THREAD_TITLE_OFFSET_DATE);
				pattern = Pattern.compile("\\.\\d\\d\\d\\d");
				matcher = pattern.matcher(wroteAt);
				if (matcher.find()) {
					date += wroteAt.substring(matcher.start(), matcher.end());
					//15.03.2014
				} else {
					System.err.println("Can't read 'wroteAt' date from thread.");
				}
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\d\\d\\.\\d\\.\\d\\d\\d\\d");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start(), matcher.end());
				//15.3.2014
				date = date.substring(0, date.length() - 6) + "0" + date.substring(date.length() - 6);
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\d\\d\\.\\d\\.");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start(), matcher.end());
				//15.3.
				date = date.substring(0, date.length() - 2) + "0" + date.substring(date.length() - 2);
				String wroteAt = content.get(curContentIndex + THREAD_TITLE_OFFSET_DATE);
				pattern = Pattern.compile("\\d\\d\\d\\d");
				matcher = pattern.matcher(wroteAt);
				if (matcher.find()) {
					date += wroteAt.substring(matcher.start(), matcher.end());
					//15.03.2014
				} else {
					System.err.println("Can't read 'wroteAt' date from thread.");
				}
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\d\\.\\d\\.\\d\\d");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start(), matcher.end());
				//7.2.14
				date = "0" + date.substring(0, date.length() - 2) + DATE_YEAR_PRE + date.substring(date.length() - 2);
				date = date.substring(0, date.length() - 6) + "0" + date.substring(date.length() - 6);
				//07.02.2014
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\d\\.\\d[^\\d\\.]");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start(), matcher.end() - 1);
				//5.3
				date = "0" + date.substring(0, date.length() - 1) + "0" + date.substring(date.length() - 1);
				String wroteAt = content.get(curContentIndex + THREAD_TITLE_OFFSET_DATE);
				pattern = Pattern.compile("\\.\\d\\d\\d\\d");
				matcher = pattern.matcher(wroteAt);
				if (matcher.find()) {
					date += wroteAt.substring(matcher.start(), matcher.end());
					//05.03.2014
				} else {
					System.err.println("Can't read 'wroteAt' date from thread.");
				}
			}
		}
		if (!found) {
			pattern = Pattern.compile("\\d\\.\\d\\.");
			matcher = pattern.matcher(title);
			if (matcher.find()) {
				found = true;
				date = title.substring(matcher.start(), matcher.end());
				//5.3.
				date = "0" + date.substring(0, date.length() - 2) + "0" + date.substring(date.length() - 2);
				String wroteAt = content.get(curContentIndex + THREAD_TITLE_OFFSET_DATE);
				pattern = Pattern.compile("\\d\\d\\d\\d");
				matcher = pattern.matcher(wroteAt);
				if (matcher.find()) {
					date += wroteAt.substring(matcher.start(), matcher.end());
					//05.03.2014
				} else {
					System.err.println("Can't read 'wroteAt' date from thread.");
				}
			}
		}
		if (!found) {
			System.err.println("Can't parse date from title: " + title);
		}
		
		//Validate date
		int day = Integer.parseInt(date.substring(0, 2));
		int month = Integer.parseInt(date.substring(3, 5));
		int year = Integer.parseInt(date.substring(6));
		if (day < 1 || day > 31
				|| month < 1 || month > 12
				|| year < DATE_FIRST_YEAR || year > Calendar.getInstance().get(Calendar.YEAR)) {
			System.err.println("No valid date: " + date);
		}
		
		return CrawlerUtil.convertStringToDate(date);
	}
	/**
	 * Gets the urls to all events by using the event sub-forum.
	 * @param path Path to the event sub-forum
	 * @return List of urls to all events
	 * @throws IOException If an I/O-Exception occurs
	 */
	private static List<String> getEventUrls(String path) throws IOException {
		List<String> events = new ArrayList<String>();
		int curPage = 0;
		
		boolean continueCrawling = true;
		while (continueCrawling) {
			//Work trough the current page
			String pageUrl = EVENTS_PATH + EVENTS_PATH_SUFFIX + curPage;
			List<String> content = CrawlerUtil.getWebContent(pageUrl);
			
			//Reject everything before the mask
			int i = -1;
			String line = "";
			do {
				i++;
				line = content.get(i);
			} while (!line.contains(EVENTS_MASK_START));
			
			int urlsOnThisPage = 0;
			do {
				i++;
				line = content.get(i);
				if (line.contains(EVENTS_REJECT_STICKY)) {
					urlsOnThisPage++;
					//Reject lines of this thread
					i += EVENTS_REJECT_LINE_SIZE;
				} else if (line.contains(EVENTS_ACCEPT_THREAD)) {
					//Accept this thread
					urlsOnThisPage++;
					int urlStart = line.indexOf(EVENTS_URL_TAG, line.indexOf(EVENTS_ACCEPT_THREAD));
					int urlEnd = line.indexOf(EVENTS_URL_TAG, urlStart + 1);
					String url = line.substring(urlStart + 1, urlEnd);
					events.add(SERVERPATH + url);
					//Reject the last lines of this thread
					i += EVENTS_REJECT_LINE_SIZE - 1;
				}
			} while (!line.contains(EVENTS_MASK_END));
			
			//End crawl if there are no more pages left (indicated by no threads)
			if (urlsOnThisPage == 0) {
				continueCrawling = false;
			}
			
			curPage += EVENTS_THREAD_AMOUNT;
		}
		
		return events;
	}
}