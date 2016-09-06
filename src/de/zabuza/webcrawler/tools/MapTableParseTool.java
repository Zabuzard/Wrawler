package de.zabuza.webcrawler.tools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility tool for map table parsing.
 * @author Zabuza
 *
 */
public class MapTableParseTool {
	/**
	 * Path to the file that contains external data.
	 */
	private static final String FILEPATH = "C:\\Users\\Isaac Clarke\\Desktop\\";
	
	/**
	 * Utility class. No implementation.
	 */
	private MapTableParseTool() {

	}
	/**
	 * Gets the content of a file and returns it as list of lines.
	 * @param path Path to the file
	 * @return List of lines from the content
	 * @throws IOException If an I/O-Exception occurs
	 */
	public static List<String> getFileContent(String path) throws IOException {
		BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		List<String> content = new ArrayList<String>();
		
		String line = file.readLine();
		while (line != null){
			content.add(line);
			line = file.readLine();
		}
		
		file.close();
		return content;
	}
	
	/**
	 * Gets the content of a map table file and generates code for it.
	 * @param args
	 *            Not supported
	 * @throws IOException 
	 */
	public static void main(final String[] args) throws IOException {
		String filename = "mapTable.csv";
		List<String> list = getFileContent(FILEPATH + filename);
		
		String header = "//Generated by MapTableParseTool : " + filename;
		System.out.println(header);
		
		for (String line : list) {
			String[] data = line.split(",");
			int id = Integer.parseInt(data[0].replaceAll("\"", ""));
			System.out.println("idToMap.put(" + id + ", " + data[1] + ");");
			data[2] = data[2].replaceAll("\"", "").trim();
			System.out.println("mapToActive.put(" + data[1] + ", " + data[2] + ");");
		}
	}
}
