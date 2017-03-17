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
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class VMEmulator {

	public enum CommandType {
		C_ARITHMETIC, C_PUSH, C_POP, C_GOTO, C_IF, C_FUNCTION, C_RETURN, C_CALL
	}

	private static final Map<String, CommandType> commandsMap = new HashMap<>();
	private static Map<String, Integer> labels = new HashMap<>();
	private static Map<String,Integer> functions = new HashMap<>();

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
		
		/**
		 * Returns a serialized form of the stack frame
		 * "functionName|pc|arg0:val|arg1:val|local0:val|local1:val|local2:val...
		 */
		@Override
		public String toString() {
			int numLocals = functions.get(function);
			int argPointer = stackPointer - args - 5;
			int localPointer = stackPointer;
			int pcPointer = stackPointer - 5;
			String[] frame = new String[numLocals + args + 2];
			frame[0] = function;
			frame[1] = "" + (ram[pcPointer] - 1);
			for (int i = 0; i < args; i++) {
				frame[i + 2] = "arg" + i + ":" + ram[argPointer + i];
			}
			for (int i = 0; i < numLocals; i++) {
				frame[args + i + 2] = "local" + i + ":" + ram[localPointer + i];
			}
			return String.join("|", frame);
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
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = line.trim();
			if (line.isEmpty() || line.startsWith("//")) {
				continue;
			}
			if (line.startsWith("label")) {
				labels.put(line.split(" ")[1], commandAddress);
			} else if (line.startsWith("function")) {
				String[] func = line.split(" ");
				functions.put(func[1], Integer.parseInt(func[2]));
				labels.put(func[1], commandAddress++);
				program.add(line);
			} else {
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
		System.out.println("Sent event: " + event);
	}
	
	private String parseStack() {
		String[] frameStrings = new String[frames.size()];
		for (int i = 0; i < frameStrings.length; i++) {
			frameStrings[i] = frames.get(i).toString();
		}
		return String.join("#", frameStrings);
	}
	
	private String parseHeap(short[] heap) {
		List<String> objects = new ArrayList<>();
		int i = 0;
		while(i < heap.length) {
			int size = heap[i++];
			String[] object = new String[size];
			for (int j = i; j < i + size - 1; j++) {
				object[j-i] = "" + heap[j];
			}
			object[size - 1] += "" + heap[i + size - 1];
			i += size;
			objects.add(String.join(",", object));
		}
		String[] heapString = objects.toArray(new String[]{});
		return String.join(",", heapString);
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
			breakpoints.remove(new Integer(command[1]));
			break;
		case "data":
			short[] heap = new short[freeHeapPointer - HEAP_OFFSET];
			System.arraycopy(ram, HEAP_OFFSET, heap, 0, heap.length);
			response = parseHeap(heap);
			break;
		case "exit":
			isTerminated = true;
			break;
		case "resume":
			isPaused = false;
			sendDebugEvent("resumed|client");
			break;
		case "set":
			breakpoints.add(new Integer(command[1]));
			break;
		case "stack":
//			short[] stack = new short[stackPointer - STACK_OFFSET];
//			System.arraycopy(ram, 256, stack, 0, stack.length);
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
		case "var":
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
					sendDebugEvent("suspended|breakpoint");
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
			address = popStack();
			short val = popStack();
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
		pc = labels.get(label);
	}

	private void processIf(String label) {
		int val = popStack();
		if (val != 0) {
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
