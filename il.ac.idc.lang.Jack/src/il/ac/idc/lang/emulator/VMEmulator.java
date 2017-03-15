package il.ac.idc.lang.emulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

	private static final short HEAP_OFFSET = 2048;
	private static final short STACK_OFFSET = 256;
	private static final short STATIC_OFFSET = 16;
	private static final short TEMP_OFFSET = 8;
	private short freeHeapPointer = HEAP_OFFSET;
	private short[] ram = new short[1 << 16];
	private short stackPointer;
	private short thisPointer, thatPointer, localPointer, argPointer;
	List<String> program = new ArrayList<>();
	List<Integer> breakpoints = new ArrayList<>();
	int pc;

	private boolean isPaused = false;

	public VMEmulator(InputStream stream, int port) {

	}

	public VMEmulator(InputStream stream) {
		Scanner scanner = new Scanner(stream);
		program.add("call Sys.init 0");
		program.add("call Sys.halt 0");
		int commandAddress = 2;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = line.trim();
			if (line.isEmpty() || line.startsWith("//")) {
				continue;
			}
			if (line.startsWith("label")) {
				labels.put(line.split(" ")[1], commandAddress);
			} else if (line.startsWith("function")) {
				labels.put(line.split(" ")[1], commandAddress++);
				program.add(line);
			} else {
				program.add(line);
				commandAddress++;
			}
		}
		stackPointer = STACK_OFFSET;
		localPointer = (short) (stackPointer + 5);
		scanner.close();
	}

	public short[] getStack() {
		short[] stack = new short[stackPointer - STACK_OFFSET];
		System.arraycopy(ram, 256, stack, 0, stackPointer - STACK_OFFSET);
		return stack;
	}

	short popStack() {
		stackPointer--;
		short val = ram[stackPointer];
		ram[stackPointer] = 0;
		return val;
	}

	void pushStack(short val) {
		ram[stackPointer] = val;
		stackPointer++;
	}

	public boolean hasMoreCommands() {
		return pc < program.size();
	}

	public void run() {
		isPaused = false;
		while (hasMoreCommands() && !isPaused) {
			if (breakpoints.contains(pc)) {
				isPaused = true;
			} else {
				processCommand();
			}
		}
	}

	public boolean isTerminated() {
		return hasMoreCommands();
	}

	public boolean isPaused() {
		return isPaused;
	}

	public void processCommand() {
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
		if (args.length != 1) {
			System.err.println("Error parsing command line arguments");
			System.exit(1);
		}
		String program = args[0];
		try {
			VMEmulator vm = new VMEmulator(new FileInputStream(new File(program)));
			while (vm.hasMoreCommands()) {
				vm.processCommand();
			}
		} catch (IOException e) {
			System.err.println("Failed reading VM program: " + program);
			System.err.println(e.getMessage());
		}
	}
}
