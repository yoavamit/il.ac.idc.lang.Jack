package il.ac.idc.lang.hvm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {
	
	public enum CommandType {
		C_ARITHMETIC,
		C_PUSH,
		C_POP,
		C_LABEL,
		C_GOTO,
		C_IF,
		C_FUNCTION,
		C_RETURN,
		C_CALL
	}
	
	private static final Map<String, CommandType> commandsMap = new HashMap<>();
	
	static {
		commandsMap.put("push", CommandType.C_PUSH);
		commandsMap.put("pop", CommandType.C_POP);
		commandsMap.put("add", CommandType.C_ARITHMETIC);
		commandsMap.put("sub", CommandType.C_ARITHMETIC);
		commandsMap.put("neg", CommandType.C_ARITHMETIC);
		commandsMap.put("eq", CommandType.C_ARITHMETIC);
		commandsMap.put("lt", CommandType.C_ARITHMETIC);
		commandsMap.put("gt", CommandType.C_ARITHMETIC);
		commandsMap.put("and", CommandType.C_ARITHMETIC);
		commandsMap.put("or", CommandType.C_ARITHMETIC);
		commandsMap.put("not", CommandType.C_ARITHMETIC);
		commandsMap.put("label", CommandType.C_LABEL);
		commandsMap.put("goto", CommandType.C_GOTO);
		commandsMap.put("if-goto", CommandType.C_IF);
		commandsMap.put("function", CommandType.C_FUNCTION);
		commandsMap.put("return", CommandType.C_RETURN);
		commandsMap.put("call", CommandType.C_CALL);
	}
	
	private List<String> commands;
	private String currentCommand;
	private int commandIndex;
	
	public Parser(InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String currentCommand = reader.readLine();
		while(currentCommand != null) {
			if (!currentCommand.isEmpty() && !currentCommand.startsWith("//")) {
				if (currentCommand.indexOf("//") >= 0) {
					currentCommand = currentCommand.substring(0, currentCommand.indexOf("//"));
				}
				commands.add(currentCommand.trim());
			}
		}
		commandIndex = 0;
		this.currentCommand = null;
	}
	
	public boolean hasMoreCommands() {
		return commandIndex != commands.size();
	}
	
	/**
	 * Reads the next command from the input and makes it the current command. 
	 * Should be called only if hasMoreCommands is true. 
	 * Initially there is no current command
	 */
	public void advance() {
		currentCommand = commands.get(commandIndex++);
	}
	
	/**
	 * Returns the type of the current VM command.
	 * C_ARITHMETIC is returned for all the arithmetic commands
	 * @return
	 */
	public CommandType commandType() {
		String[] cmd = currentCommand.split(" ");
		return commandsMap.get(cmd[0]);
	}
	
	/**
	 * Returns the first argument of the current command.
	 * In the case of C_ARITHMETIC, the command itself (add, sub, etc.) is returned. 
	 * Should not be called if the current command is C_RETURN.
	 * @return
	 */
	public String arg1() {
		if (commandType() == CommandType.C_ARITHMETIC) {
			return currentCommand;
		}
		return currentCommand.split(" ")[1];
	}
	
	/**
	 * Returns the second argument of the current command. 
	 * Should be called only if the current command is C_PUSH, C_POP, C_FUNCTION, or C_CALL
	 * @return
	 */
	public int arg2() {
		String[] cmd = currentCommand.split(" ");
		return Integer.parseInt(cmd[2]);
	}
}
