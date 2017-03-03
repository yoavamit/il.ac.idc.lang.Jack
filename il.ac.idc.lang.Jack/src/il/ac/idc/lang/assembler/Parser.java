package il.ac.idc.lang.assembler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Parser {

	public enum CommandType {
		A_COMMAND,
		C_COMMAND,
		L_COMMAND
	}
	
	private int currentCommandIndex;
	private String currentCommand;
	private List<String> commands;
	
	public Parser(InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		commands = new ArrayList<String>();
		String currentLine = reader.readLine();
		while(currentLine != null) {
			currentLine = currentLine.trim();
			if (!currentLine.isEmpty() && !currentLine.startsWith("//")) {
				if (currentLine.indexOf("//") > 0) {
					currentLine = currentLine.substring(0, currentLine.indexOf("//")).trim();
				}
				commands.add(currentLine);	
			}
			currentLine = reader.readLine();
		}
		currentCommand = null;
		currentCommandIndex = 0;
	}
	
	public void reset() {
		currentCommandIndex = 0;
		currentCommand = null;
	}
	
	public boolean hasMoreCommands() {
		return currentCommandIndex != commands.size();
	}
	
	public void advance() {
		currentCommand = commands.get(currentCommandIndex++);
	}
	
	public CommandType commandType() {
		if (currentCommand.startsWith("@")) {
			return CommandType.A_COMMAND;
		}
		if (currentCommand.startsWith("(")) {
			return CommandType.L_COMMAND;
		}
		return CommandType.C_COMMAND;
	}
	
	public String symbol() {
		if (commandType() == CommandType.L_COMMAND) {
			return currentCommand.substring(1, currentCommand.indexOf(")"));
		} else {
			String[] cmd = currentCommand.split(" ");
			return cmd[0].substring(1);
		}
	}
	
	public String dest() {
		int destEnd = currentCommand.indexOf("=");
		if (destEnd > 0) {
			return currentCommand.substring(0, destEnd);
		}
		
		return "";
	}
	
	public String comp() {
		int startIndex = currentCommand.indexOf("=");
		int endIndex = Math.min(currentCommand.indexOf(";"), currentCommand.indexOf(" "));
		if (startIndex == -1) {
			startIndex = 0;
		} else {
			startIndex++;
		}
		if (endIndex == -1) {
			endIndex = currentCommand.length();
		}
		return currentCommand.substring(startIndex, endIndex);
	}
	
	public String jump() {
		int startIndex = currentCommand.indexOf(";");
		if (startIndex == -1) {
			return "";
		}
		startIndex++;
		if (currentCommand.indexOf(" ") > 0) {
			return currentCommand.substring(startIndex, currentCommand.indexOf(" "));
		} else {
			return currentCommand.substring(startIndex);
		}
	}
}
