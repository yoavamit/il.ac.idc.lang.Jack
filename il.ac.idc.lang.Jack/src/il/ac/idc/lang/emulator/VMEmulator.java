package il.ac.idc.lang.emulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class VMEmulator {

	public enum CommandType {
		C_ARITHMETIC, C_PUSH, C_POP, C_GOTO, C_IF, C_FUNCTION, C_RETURN, C_CALL
	}

	private static final Map<String, CommandType> commandsMap = new HashMap<>();
	private static Map<String, Integer> labels = new HashMap<>();
	private static Map<String,Integer> functions = new HashMap<>();
	private static Map<String, List<String>> functionLocalsDebugInfo = new HashMap<>();
	private static Map<String, List<String>> functionArgsDebugInfo = new HashMap<>();
	private static Map<String, List<String>> classFieldsDebugInfo = new HashMap<>();

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
		commandsMap.put("goto", CommandType.C_GOTO);
		commandsMap.put("if-goto", CommandType.C_IF);
		commandsMap.put("function", CommandType.C_FUNCTION);
		commandsMap.put("return", CommandType.C_RETURN);
		commandsMap.put("call", CommandType.C_CALL);
	}

	class FrameReference {
		private String function;
		private int stackPointer, args;
		
		public FrameReference(String function, int ref, int args) {
			this.function = function;
			this.stackPointer = ref;
			this.args = args;
		}
		
		public String getValue(String varName) {
			int index = 0;
			if (functionArgsDebugInfo.get(function).contains(varName)) {
				index = functionArgsDebugInfo.get(function).indexOf(varName) + (stackPointer - args - 5);
			} else {
				index = functionLocalsDebugInfo.get(function).indexOf(varName) + stackPointer;
			}
			return "" + ram[index]; 
		}
		
		/**
		 * Returns a serialized form of the stack frame
		 * "functionName|pc|var1|var2|var3..."
		 */
		@Override
		public String toString() {
			List<String> locals = functionLocalsDebugInfo.get(function);
			if (locals == null) {
				locals = new ArrayList<String>();
			}
			List<String> args = functionArgsDebugInfo.get(function);
			int pcPointer = stackPointer - 5;
			int argPointer = ram[stackPointer - 3];
			String[] frame = new String[2 + locals.size() + args.size()];
			frame[0] = function;
			frame[1] = "" + (ram[pcPointer] - 1);
			for (int i = 0; i < args.size(); i++) {
				frame[i + 2] = args.get(i) + ":" + (argPointer + i);
			}
			for (int i = 0; i < locals.size(); i++) {
				frame[i + 2 + args.size()] = locals.get(i) + ":" + (stackPointer + i);
			}
			return String.join("|", frame);
		}

		public void setValue(String varName, int value) {
			int index = 0;
			if (functionArgsDebugInfo.get(function).contains(varName)) {
				index = stackPointer - 5 - args + functionArgsDebugInfo.get(function).indexOf(varName);
			} else {
				index = stackPointer + functionLocalsDebugInfo.get(function).indexOf(varName);
			}
			ram[index] = (short) value;
		}
	}
	
	private static final short HEAP_OFFSET = 2048;
	private static final short STACK_OFFSET = 256;
	private static final short STATIC_OFFSET = 16;
	private static final short TEMP_OFFSET = 8;
	private short freeHeapPointer = HEAP_OFFSET;
	private short[] ram = new short[1 << 16];
	private short stackPointer;
	private short thisPointer, thatPointer, localPointer, argPointer;
	private ServerSocket server, eventServer;
	private Socket client, eventClient;
	private BufferedReader reader;
	private boolean debug = false;
	private List<String> program = new ArrayList<>();
	private List<Integer> breakpoints = new ArrayList<>();
	private List<FrameReference> frames = new ArrayList<>();
	private Map<Integer, Set<Integer>> sourceCodeLineMap = new HashMap<>();
	private int pc;

	private boolean isPaused = false;
	private boolean isTerminated = false;

	public VMEmulator(File file, int requestPort, int eventPort) throws IOException {
		this(file);
		server = new ServerSocket(requestPort);
		eventServer = new ServerSocket(eventPort);
		isPaused = true;
		this.debug = true;
	}
	
	private static int commandAddress;
	
	public VMEmulator(File file) throws IOException {
		program.add("call Sys.init 0");
		program.add("call Sys.halt 0");
		commandAddress = 2;
		if (file.isFile()) {
			readFile(file);
		} else if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				if (f.getName().endsWith(".vm")) {
					readFile(f);
				}
			}
		}
		stackPointer = STACK_OFFSET;
		localPointer = (short) (stackPointer + 5);
	}
	
	private void readFile(File file) throws IOException {
		InputStream stream = new FileInputStream(file);
		Scanner scanner = new Scanner(stream);
		int sourceCodeLine = 0;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = line.trim();
			if (line.startsWith("// var:")) {
				int splitIndex = line.indexOf(":") + 1;
				String[] data = line.substring(splitIndex).split("\\.");
				String klass = data[0];
				String subroutine = data[1];
				String varName = data[2];
				String type = data[3];
				if (!functionLocalsDebugInfo.containsKey(klass + "." + subroutine)) {
					functionLocalsDebugInfo.put(klass + "." + subroutine, new ArrayList<String>());
				}
				functionLocalsDebugInfo.get(klass + "." + subroutine).add(type + ":" + varName);
			} else if (line.startsWith("// arg:")) {
				int splitIndex = line.indexOf(":") + 1;
				String[] data = line.substring(splitIndex).split("\\.");
				String klass = data[0];
				String subroutine = data[1];
				String name = data[2];
				String type = data[3];
				if (!functionArgsDebugInfo.containsKey(klass + "." + subroutine)) {
					functionArgsDebugInfo.put(klass + "." + subroutine, new ArrayList<String>());
				}
				functionArgsDebugInfo.get(klass + "." + subroutine).add(type + ":" + name);
			} else if (line.startsWith("// classVar:")) {
				int splitIndex = line.indexOf(":") + 1;
				String[] data = line.substring(splitIndex).split("\\.");
				String klass = data[0];
				String modifier = data[1];
				String name = data[2];
				String type = data[3];
				if (!classFieldsDebugInfo.containsKey(klass)) {
					classFieldsDebugInfo.put(klass, new ArrayList<String>());
				}
				classFieldsDebugInfo.get(klass).add(modifier + "|" + type + "|" + name);
			} else if (line.startsWith("// sourceLine:")) {
				int colons = line.indexOf(":");
				sourceCodeLine = Integer.parseInt(line.substring(colons + 1));
				continue;
			}
			if (line.isEmpty() || line.startsWith("//")) {
				continue;
			}
			if (line.startsWith("label")) {
				labels.put(line.split(" ")[1], commandAddress);
			} else if (line.startsWith("function")) {
				String[] func = line.split(" ");
				if (!functionArgsDebugInfo.containsKey(func[1])) {
					functionArgsDebugInfo.put(func[1], new ArrayList<String>());
				}
				functions.put(func[1], Integer.parseInt(func[2]));
				if (!sourceCodeLineMap.containsKey(sourceCodeLine)) {
					sourceCodeLineMap.put(sourceCodeLine, new HashSet<Integer>());
				}
				sourceCodeLineMap.get(sourceCodeLine).add(commandAddress);
				labels.put(func[1], commandAddress++);
				program.add(line);
			} else {
				if (!sourceCodeLineMap.containsKey(sourceCodeLine)) {
					sourceCodeLineMap.put(sourceCodeLine, new HashSet<Integer>());
				}
				sourceCodeLineMap.get(sourceCodeLine).add(commandAddress);
				program.add(line);
				commandAddress++;
			}
		}
		scanner.close();
	}

	private void sendDebugEvent(String event) throws IOException {
		if (eventClient == null) {
			eventClient = eventServer.accept();
		}
		PrintWriter out = new PrintWriter(eventClient.getOutputStream());
		out.println(event);
		out.flush();
	}
	
	private String parseStack() {
		String[] frameStrings = new String[frames.size()];
		for (int i = 0; i < frameStrings.length; i++) {
			frameStrings[i] = frames.get(i).toString();
		}
		return String.join("#", frameStrings);
	}
	
	private String parseHeap(int address, String type) {
		int size = ram[address - 1];
		List<String> objectFields = classFieldsDebugInfo.get(type);
		String[] parsedObject = new String[size];
		for (int i = 0; i < size; i++) {
			String[] field = objectFields.get(i).split("\\|");
			String modifier = field[0];
			String fieldType = field[1];
			String fieldName = field[2];
			if (modifier.equals("field")) {
				parsedObject[i] = fieldType + ":" + fieldName + "=" + ram[address + i];
			}
		}
		return String.join("|", parsedObject);
	}
	
	private String getValue(String type, int address) {
		switch(type) {
		case "int":
		case "char":
		case "boolean":
			return "" + ram[address];
		default:
			List<String> fields = classFieldsDebugInfo.get(type);
			String[] values = new String[fields.size()];
			for (int i = 0; i < values.length; i++) {
				values[i] = fields.get(i) + "|" + (address + i);
			}
			return String.join(",", values);
		}
	}
	
	private void sendResponse(Socket client, String response) throws IOException {
		PrintWriter out = new PrintWriter(client.getOutputStream());
		out.println(response);
		out.flush();
	}
	
	private void processDebugCommand() throws IOException {
		if (client == null) {
			client = server.accept();
			reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
		}
		String cmd = reader.readLine();
		String[] command = cmd.split("\\|");
		String response = "ok";
		switch(command[0]) {
		case "clear":
			Set<Integer> toRemove = sourceCodeLineMap.get(new Integer(command[1]));
			if (toRemove != null) {
				breakpoints.addAll(toRemove);
			}
			breakpoints.removeAll(toRemove);
			break;
		case "data":
			int objectAddress = Integer.parseInt(command[1]);
			String objectType = command[2];
			response = parseHeap(objectAddress, objectType);
			break;
		case "exit":
			isTerminated = true;
			break;
		case "resume":
			isPaused = false;
			sendDebugEvent("resumed|client");
			processCommand();
			break;
		case "set":
			Set<Integer> toAdd = sourceCodeLineMap.get(new Integer(command[1]));
			if (toAdd != null) {
				breakpoints.addAll(toAdd);
			}
			break;
		case "stack":
			response = parseStack();
			break;
		case "step":
			if (hasMoreCommands()) {
				processCommand();
			}
			isPaused = true;
			sendDebugEvent("suspended|step");
			break;
		case "suspend":
			isPaused = true;
			sendDebugEvent("suspended|client");
			break;
		case "value-get":
			String type = command[1];
			int address = Integer.parseInt(command[2]);
			response = getValue(type, address);
			break;
		case "value-set":
			int frameId = Integer.parseInt(command[1]);
			String varName = command[2];
			int value = Integer.parseInt(command[3]);
			frames.get(frameId).setValue(varName, value);
			// TODO
			break;
		default:
			response = "Unknown command: " + command[0];
		}
		sendResponse(client, response);
	}
	
	private short popStack() {
		stackPointer--;
		short val = ram[stackPointer];
		ram[stackPointer] = 0;
		return val;
	}

	private void pushStack(short val) {
		ram[stackPointer] = val;
		stackPointer++;
	}

	private boolean hasMoreCommands() {
		return pc < program.size();
	}

	public void run() throws IOException {
		if (debug) {
			sendDebugEvent("started");
			isPaused = true;
			sendDebugEvent("suspended|client");
		}
		while (hasMoreCommands()) {
			if (isTerminated) {
				break;
			}
			if (!isPaused) {
				if (breakpoints.contains(pc)) {
					isPaused = true;
					sendDebugEvent("suspended|breakpoint|" + pc);
				} else {
					processCommand();
				}
			} else {
				processDebugCommand();
			}
		}
		if (debug) {
			sendDebugEvent("terminated");
		}
		if (client != null) {
			client.close();
		}
		if (eventClient != null) {
			eventClient.close();
		}
		server.close();
		eventServer.close();
	}

	private void processCommand() {
		String[] command = program.get(pc).split(" ");
		CommandType type = commandsMap.get(command[0]);
		System.out.println("PC:" + pc + " " + program.get(pc));
		switch (type) {
		case C_ARITHMETIC:
			processArithmetic(command[0]);
			break;
		case C_CALL:
			processCall(command[1], Integer.parseInt(command[2]));
			break;
		case C_FUNCTION:
			processFunction(command[1], Integer.parseInt(command[2]));
			break;
		case C_GOTO:
			processGoto(command[1]);
			break;
		case C_IF:
			processIf(command[1]);
			break;
		case C_POP:
			processPop(command[1], Integer.parseInt(command[2]));
			break;
		case C_PUSH:
			processPush(command[1], Integer.parseInt(command[2]));
			break;
		case C_RETURN:
			processReturn();
			return;
		default:
			break;
		}
		pc++;
	}

	private void processArithmetic(String cmd) {
		int y;
		switch (cmd) {
		case "add":
			pushStack((short) (popStack() + popStack()));
			break;
		case "sub":
			y = popStack();
			pushStack((short) (popStack() - y));
			break;
		case "neg":
			pushStack((short) (-popStack()));
			break;
		case "eq":
			pushStack((short) (popStack() == popStack() ? -1 : 0));
			break;
		case "gt":
			y = popStack();
			pushStack((short) (popStack() > y ? -1 : 0));
			break;
		case "lt":
			y = popStack();
			pushStack((short) (popStack() < y ? -1 : 0));
			break;
		case "and":
			pushStack((short) (popStack() & popStack()));
			break;
		case "or":
			pushStack((short) (popStack() | popStack()));
			break;
		case "not":
			pushStack((short) (popStack() ^ -1));
			break;
		}
	}

	private void processCall(String functionName, int args) {
		switch (functionName) {
		case "Sys.halt":
			System.out.println("VM halted");
			pc = program.size();
			break;
		case "Sys.wait":
			int duration = popStack();
			try {
				Thread.sleep(duration);
			} catch (InterruptedException e) {

			}
			break;
		case "Sys.error":
			int errorCode = popStack();
			System.err.println("ERR" + errorCode);
			pc = program.size();
			break;
		case "Sys.init":
			processCall("Main.main", 0);
			break;
		case "Memory.peek":
			short address = popStack();
			pushStack(ram[address]);
			break;
		case "Memory.poke":
			short val = popStack();
			address = popStack();
			ram[address] = val;
			break;
		case "Memory.alloc":
			short size = popStack();
			ram[freeHeapPointer] = size;
			freeHeapPointer++;
			pushStack(freeHeapPointer);
			freeHeapPointer += size;
			break;
		case "Memory.dealloc":
			break;
		case "Math.abs":
			short abs = popStack();
			if (abs < 0) {
				abs = (short) -abs;
			}
			pushStack(abs);
			break;
		case "Math.multiply":
			short x = popStack();
			short y = popStack();
			pushStack((short) (x * y));
			break;
		case "Math.divide":
			y = popStack();
			x = popStack();
			pushStack((short) (x / y));
			break;
		case "Math.min":
			x = popStack();
			y = popStack();
			pushStack((short) Math.min(x, y));
			break;
		case "Math.max":
			x = popStack();
			y = popStack();
			pushStack((short) Math.max(x, y));
			break;
		case "Math.sqrt":
			x = popStack();
			pushStack((short) Math.sqrt(x));
			break;
		case "String.new":
			short len = popStack();
			ram[freeHeapPointer] = len;
			pushStack(++freeHeapPointer);
			freeHeapPointer += len;
			break;
		case "String.dispose":
			short pointer = popStack();
			size = ram[pointer - 1];
			ram[pointer - 1] = 0;
			for (int i = 0; i < size; i++) {
				ram[pointer + i] = 0;
			}
			break;
		case "String.length":
			pointer = popStack();
			pushStack(ram[pointer - 1]);
			break;
		case "String.charAt":
			short charAt = popStack();
			pointer = popStack();
			pushStack(ram[pointer + charAt]);
			break;
		case "String.setCharAt":
			short ch = popStack();
			charAt = popStack();
			pointer = popStack();
			ram[pointer + charAt] = ch;
			break;
		case "String.appendChar":
			ch = popStack();
			pointer = popStack();
			for (short i = 0; i < ram[pointer - 1]; i++) {
				if (ram[pointer + i] == '\0') {
					ram[pointer + i] = ch;
					ram[pointer + i + 1] = '\0';
					break;
				}
			}
			pushStack(pointer);
			break;
		case "String.eraseLastChar":
			pointer = popStack();
			for (short i = 0; i < ram[pointer - 1]; i++) {
				if (ram[pointer + i] == '\0') {
					ram[pointer + i - 1] = '\0';
					ram[pointer + i] = 0;
					break;
				}
			}
			break;
		case "String.intValue":
			pointer = popStack();
			short digits = 0;
			while (ram[pointer + digits] >= '0' && ram[pointer + digits] <= '9') {
				digits++;
			}
			val = 0;
			for (short i = 0; i < digits; i++) {
				val += (ram[pointer + i] - '0') * Math.pow(10, digits - i - 1);
			}
			pushStack(val);
			break;
		case "String.setInt":
			String stringVal = "" + popStack();
			pointer = popStack();
			for (int i = 0; i < stringVal.length(); i++) {
				ram[pointer + i] = (short) stringVal.charAt(i);
			}
			break;
		case "String.backspace":
			pushStack((short) 8);
			break;
		case "String.doubleQuote":
			pushStack((short) '\"');
			break;
		case "String.newLine":
			pushStack((short) '\n');
			break;
		case "Array.new":
			size = popStack();
			ram[freeHeapPointer++] = size;
			pushStack(freeHeapPointer);
			freeHeapPointer += size;
			break;
		case "Array.dispose":
			break;
		default:
			pushStack((short) (pc + 1));
			pushStack(localPointer);
			pushStack(argPointer);
			pushStack(thisPointer);
			pushStack(thatPointer);
			FrameReference ref = new FrameReference(functionName, stackPointer, args);
			frames.add(ref);
			argPointer = (short) (stackPointer - args - 5);
			localPointer = stackPointer;
			pc = labels.get(functionName);
		}
	}

	private void processFunction(String functionName, int vars) {
		for (int i = 0; i < vars; i++) {
			pushStack((short) 0);
		}
	}

	private void processGoto(String label) {
		pc = labels.get(label) - 1;
	}

	private void processIf(String label) {
		int val = popStack();
		if (val == 0) {
			pc = labels.get(label);
		}
	}

	private void processPop(String segment, int index) {
		short val = popStack();
		switch (segment) {
		case "constant":
			ram[index] = val;
			break;
		case "argument":
			ram[argPointer + index] = val;
			break;
		case "local":
			ram[localPointer + index] = val;
			break;
		case "static":
			ram[STATIC_OFFSET + index] = val;
			break;
		case "this":
			ram[thisPointer + index] = val;
			break;
		case "that":
			ram[thatPointer + index] = val;
			break;
		case "pointer":
			ram[3 + index] = val;
			if (index == 0) {
				thisPointer = val;
			} else {
				thatPointer = val;
			}
			break;
		case "temp":
			ram[TEMP_OFFSET + index] = val;
			break;
		}
	}

	private void processPush(String segment, int index) {
		short val = 0;
		switch (segment) {
		case "constant":
			val = (short) index;
			break;
		case "argument":
			val = ram[argPointer + index];
			break;
		case "local":
			val = ram[localPointer + index];
			break;
		case "static":
			val = ram[STATIC_OFFSET + index];
			break;
		case "this":
			val = ram[thisPointer + index];
			break;
		case "that":
			val = ram[thatPointer + index];
			break;
		case "pointer":
			val = ram[3 + index];
			break;
		case "temp":
			val = ram[TEMP_OFFSET + index];
			break;
		}
		pushStack(val);
	}

	private void processReturn() {
		short frame = localPointer;
		int returnAddress = ram[frame - 5];
		ram[argPointer] = popStack();
		stackPointer = (short) (argPointer + 1);
		localPointer = ram[frame - 4];
		argPointer = ram[frame - 3];
		thisPointer = ram[frame - 2];
		thatPointer = ram[frame - 1];
		pc = returnAddress;
		frames.remove(frames.size() - 1);
	}

	public static void main(String[] args) {
		String inputFile = args[0];
		int requestPort = 0, eventPort = 0;
		boolean debug = false;
		if (args.length == 6) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("--debug") || args[i].equals("-d")) {
					debug = true;
				} else if (args[i].equals("--eventPort")) {
					eventPort = Integer.parseInt(args[i+1]);
				} else if (args[i].equals("--requestPort")) {
					requestPort = Integer.parseInt(args[i+1]);
				}
			}
		}
		try {
			VMEmulator vm = null;
			if (debug) {
				vm = new VMEmulator(new File(inputFile), requestPort, eventPort);
			} else {
				vm = new VMEmulator(new File(inputFile));
			}
			vm.run();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
