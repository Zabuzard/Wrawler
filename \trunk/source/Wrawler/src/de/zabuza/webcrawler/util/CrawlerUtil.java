package de.zabuza.webcrawler.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import de.zabuza.webcrawler.struct.EventList;

/**
 * Utility class for crawlers.
 * 
 * @author Zabuza
 *
 */
public final class CrawlerUtil {
	
	/**
	 * Utility class. No implementation.
	 */
	private CrawlerUtil() {

	}
	
	/**
	 * Converts date in the string format 'dd.mm.yyyy'
	 * to an date object.
	 * @param date String in format 'dd.mm.yyyy' where months starts at '01'
	 * @return Date object of the given date
	 */
	public static Calendar convertStringToDate(String date) {
		int day = Integer.parseInt(date.substring(0, 2));
		//Month is 0-based
		int month = Integer.parseInt(date.substring(3, 5)) - 1;
		int year = Integer.parseInt(date.substring(6));
		
		return new GregorianCalendar(year, month, day);
	}
	/**
	 * Converts date as Calendar in the string format 'dd.mm.yyyy'.
	 * @param date String in format 'dd.mm.yyyy' where months starts at '01'
	 * @return Date object of the given date
	 */
	public static String convertDateToString(Calendar date) {
		String dateText = "";
		int day = date.get(Calendar.DAY_OF_MONTH);
		if (day < 10) {
			dateText += "0";
		}
		dateText += day + ".";
		int month = date.get(Calendar.MONTH) + 1;
		if (month < 10) {
			dateText += "0";
		}
		dateText += month + ".";
		int year = date.get(Calendar.YEAR);
		dateText += year;
		
		return dateText;
	}
	/**
	 * Converts time in the string format 'hh:mm:ss' to an date object.
	 * @param time String in format 'hh:mm:ss'
	 * @return Date object of the given time
	 */
	public static Calendar convertStringToTime(String time) {
		if (time == null) {
			return null;
		}
		int hour = Integer.parseInt(time.substring(0, 2));
		int minute = Integer.parseInt(time.substring(3, 5));
		int second = Integer.parseInt(time.substring(6, 7));
		
		Calendar calendar = new GregorianCalendar();
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, second);
		return calendar;
	}
	/**
	 * Converts time as Calendar in the string format 'hh:mm:ss'.
	 * @param time Calendar of the time to convert
	 * @return Date object of the given time
	 */
	public static String convertTimeToString(Calendar time) {
		String timeText = "";
		
		int hour = time.get(Calendar.HOUR_OF_DAY);
		if (hour < 10) {
			timeText += "0";
		}
		timeText += hour + ":";
		int minute = time.get(Calendar.MINUTE);
		if (minute < 10) {
			timeText += "0";
		}
		timeText += minute + ":";
		int second = time.get(Calendar.SECOND);
		if (second < 10) {
			timeText += "0";
		}
		timeText += second;
		
		return timeText;
	}
	
	/**
	 * Gets the content of a file and returns it as list of lines.
	 * @param path Path to the file
	 * @return List of lines from the content
	 * @throws IOException If an I/O-Exception occurs
	 */
	public static List<String> getFileContent(String path) throws IOException {
		BufferedReader site = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		List<String> content = new ArrayList<String>();
		
		String line = site.readLine();
		while (line != null){
			content.add(line);
			line = site.readLine();
		}
		
		site.close();
		return content;
	}
	
	/**
	 * Gets the content of a web page and returns it as list of lines.
	 * @param path Path to the web page
	 * @return List of lines from the content
	 * @throws IOException If an I/O-Exception occurs
	 */
	public static List<String> getWebContent(String path) throws IOException {
		URL url = new URL(path);
		BufferedReader site = new BufferedReader(new InputStreamReader(url.openStream()));
		List<String> content = new ArrayList<String>();
		
		String line = site.readLine();
		while (line != null){
			content.add(line);
			line = site.readLine();
		}
		
		site.close();
		return content;
	}
	
	/**
	 * Serializes a given event list to the given path.
	 * @param list list to serialize
	 * @param path path where object should be saved
	 */
	public static void serialize(EventList list, String path) {
		OutputStream fos = null;
		try {
			fos = new FileOutputStream(path);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(list);
		} catch (IOException e) {
			System.err.println("Error while serializing event list.");
			System.err.println(e);
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				System.err.println("Error while closing serialization stream of event list.");
				System.err.println(e);
			}
		}
	}
	/**
	 * Deserializes an event list from given path.
	 * @param path Path where event list is serialized
	 * @return Deserialized event list object
	 */
	public static EventList deserialize(String path) {
		EventList list = null;
		InputStream fis = null;
		try {
			fis = new FileInputStream(path);
			ObjectInputStream ois = new ObjectInputStream(fis);
			list = (EventList) ois.readObject();
		} catch (IOException|ClassNotFoundException e) {
			System.err.println("Error while deserializing event list.");
			System.err.println(e);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				System.err.println("Error while closing deserialization stream of event list.");
				System.err.println(e);
			}
		}
		return list;
	}
	/**
	 * Parses a line from the database format and returns it as list of values.
	 * @param databaseFormatLine Line in the database format
	 * @return List of values that where contained in the line
	 */
	public static String[] parseDatabaseFormatLine(String databaseFormatLine) {
		databaseFormatLine = databaseFormatLine.replaceAll(",$", ",\"\"").replaceAll("^,", "\"\",");
		String[] values = databaseFormatLine.split("\",\"");
		values[0] = values[0].substring(1);
		values[values.length - 1] = values[values.length - 1]
				.substring(0, values[values.length - 1].length() - 1);
		return values;
	}
}
