package il.ac.idc.lang.assembler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.eclipse.xtext.util.StringInputStream;

public class Parser {

	public enum CommandType {
		A_COMMAND,
		C_COMMAND,
		L_COMMAND
	}
	
	private StringBuilder readLines = new StringBuilder();
	private String currentCommand;
	private Scanner scanner;
	
	public Parser(InputStream stream) throws IOException {
		scanner = new Scanner(stream);
		currentCommand = null;
	}
	
	public void reset() {
		scanner.close();
		scanner = new Scanner(new StringInputStream(readLines.toString()));
		readLines = new StringBuilder();
		currentCommand = null;
	}
	
	public boolean hasMoreCommands() {
		return scanner.hasNextLine();
	}
	
	public void advance() {
		String line = scanner.nextLine();
		currentCommand = line.trim();
		readLines.append(line);
		readLines.append("\n");
		while(currentCommand.startsWith("//") || currentCommand.isEmpty())
			advance();
		if (currentCommand.indexOf("//") > 0)
			currentCommand = currentCommand.substring(0, currentCommand.indexOf("//"));
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
