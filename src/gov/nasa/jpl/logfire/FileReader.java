package gov.nasa.jpl.logfire;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

class FileReader {
	private BufferedReader file;
	private String fileName;

	public FileReader(String fileName) {
		try {
			FileInputStream fstream = new FileInputStream(fileName);
			DataInputStream in = new DataInputStream(fstream);
			this.file = new BufferedReader(new InputStreamReader(in));
			this.fileName = fileName;
		} catch (Exception e) {
			System.err.println("*** File open error: " + fileName + " - "
					+ e.getMessage());
		}
	}

	public String readLine() {
		String line = null;
		try {
			line = file.readLine();
		} catch (Exception e) {
			System.err.println("*** Line reading error: " + fileName + " - "
					+ e.getMessage());
		}
		return line;
	}

	public List<String> readFile() {
		List<String> lines = new ArrayList<String>();
		try {
			String line;
			while ((line = file.readLine()) != null) {
				lines.add(line);
			}
			file.close();
		} catch (Exception e) {
			System.err.println("*** File reading error: " + fileName + " - " + e.getMessage());
		}
		return lines;
	}

	public void close() {
		try {
			file.close();
		} catch (Exception e) {
			System.err.println("File closing error: " + fileName + " - " + e.getMessage());
		}
	}
}