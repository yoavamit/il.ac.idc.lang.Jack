package il.ac.idc.lang.emulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.xtext.util.StringInputStream;

public class CPUEmulator {

	private int[] memory = new int[1 << 16];
	private List<Integer> breakpoints = new ArrayList<Integer>();
	private static final int instructionsOffset = 128;
	private final int A_INSTR_VALUE_MASK = -1 >> 17;
	private final int C_INSTR_DEST_MASK = 56;
	private final int C_INST_JUMP_MASK = 7;
	private final int C_INST_OP_MASK = 8128;
	private final int EOP = 60039;
	private int regA;
	private int regD;
	private int currentInstructionAddress;
	private boolean isTerminated, isPaused;

	private void resetCPU() {
		regA = 0;
		regD = 0;
		isTerminated = false;
		isPaused = true;
		currentInstructionAddress = instructionsOffset;
		for (int i = 0; i < memory.length; i++) {
			memory[i] = 0;
		}
	}

	public void loadProgram(String program) throws IOException {
		loadProgram(new StringInputStream(program));
	}
	
	public void loadProgram(File input) throws IOException {
		loadProgram(new FileInputStream(input));
	}

	/**
	 * Return the current CPU instruction
	 * @return
	 */
	public int getCurrentInstruction() {
		return memory[currentInstructionAddress];
	}
	
	/**
	 * Get the current value of the A register
	 * @return
	 */
	public int getRegA() {
		return regA;
	}
	
	/**
	 * Get the current value of the D register
	 * @return
	 */
	public int getRegD() {
		return regD;
	}
	
	/**
	 * Get the current value of the "M" register
	 * Note: M is actually the value stored in memory at the address specified by the A register
	 * @return
	 */
	public int getRegM() {
		return memory[regA];
	}
	
	/**
	 * Returns an array containing the values stored in memory
	 * @param offset
	 * @param size
	 * @return The values stored in the memory block specified by offset and size (number of cells)
	 */
	public int[] getMemoryRange(int offset, int size) {
		int[] block = new int[size];
		for (int i = 0; i < size; i++) {
			block[i] = memory[offset + i];
		}
		return block;
	}
	
	/**
	 * Add the specified address to the list of program breakpoints
	 * @param address
	 * @return true if the addition was successful (only of this address wasn't already a breakpoint)
	 */
	public boolean addBreakpoint(int address) {
		if (breakpoints.contains(address))
			return false;
		breakpoints.add(address);
		return true;
	}
	
	/**
	 * Remove this address from the breakpoints list
	 * @param address
	 * @return true if this address was removed
	 */
	public boolean removeBreakpoint(int address) {
		return breakpoints.remove(new Integer(address));
	}
	
	/**
	 * Clear all breakpoints
	 */
	public void removeAllBreakpoints() {
		breakpoints.clear();
	}
	
	
	/**
	 * Get all current breakpoints
	 * @return
	 */
	public List<Integer> getAllBreakpoints() {
		return breakpoints;
	}
	
	public void loadProgram(InputStream input) throws IOException {
		resetCPU();
		BufferedReader stream = new BufferedReader(new InputStreamReader(input));
		String line = stream.readLine();
		int currentInstructionAddress = instructionsOffset;
		while (line != null) {
			try {
				int instruction = Integer.parseInt(line, 2);
				memory[currentInstructionAddress++] = instruction;
			} catch (NumberFormatException e) {
				stream.close();
				throw new IOException("Invalid program instruction encountered: " + line);
			}
			line = stream.readLine();
		}
		stream.close();
	}

	public void run() {
		isPaused = false;
		while (!isTerminated) {
			if (breakpoints.contains(currentInstructionAddress)) {
				// hit breakpoint
				isPaused = true;
				break;
			} else {
				executeCurrentInstruction();	
			}
		}
	}
	
	public boolean isPaused() {
		return isPaused;
	}
	
	public boolean isTerminated() {
		return isTerminated;
	}
	
	/**
	 * Emulate the execution of the current instruction specified by the current instruction memory address
	 */
	public void executeCurrentInstruction() {
		int currentInstruction = memory[currentInstructionAddress];
		if (currentInstruction == EOP) {
			isTerminated = true;
			return;
		}
		int instructionType = (currentInstruction >> 15) & 1;
		if (instructionType == 0) {
			// A-instruction
			int value = currentInstruction & A_INSTR_VALUE_MASK;
			regA = value;
			currentInstructionAddress++;
		} else if (instructionType == 1) {
			// C-instruction
			int comp = currentInstruction & C_INST_OP_MASK;
			int op = (comp & (C_INST_OP_MASK >> 1)) >> 6;
			int aValue = (comp >> 12) == 0 ? regA : memory[regA];
			int result = 0;
			switch (op) {
			case 42:
				result = 0; // 0
				break;
			case 63:
				result = 1; // 1
				break;
			case 52:
				result = -1 >> 16; // -1
				break;
			case 12:
				result = regD; // D
				break;
			case 48:
				result = aValue; // A
				break;
			case 13:
				result = regD ^ (-1 >> 16); // !D
				break;
			case 49:
				result = aValue ^ (-1 >> 16); // !A
				break;
			case 15:
				result = regD ^ (1 << 16); // -D
				break;
			case 51:
				result = aValue ^ (1 << 16); // -A
				break;
			case 31:
				result = regD + 1; // D+1
				break;
			case 55:
				result = aValue + 1; // A+1
				break;
			case 14:
				result = regD - 1; // D-1
				break;
			case 50:
				result = aValue - 1; // A-1
				break;
			case 2:
				result = aValue + regD; // D+A
				break;
			case 19:
				result = regD - aValue; // D-A
				break;
			case 7:
				result = aValue - regD; // A-D
				break;
			case 0:
				result = aValue & regD; // A&D
				break;
			case 21:
				result = aValue | regD; // A|D
				break;
			}
			
			int dest = (currentInstruction & C_INSTR_DEST_MASK) >> 3;
			if ((dest & 1) > 0) {
				memory[regA] = result;
			}
			if ((dest & 2) > 0) {
				regD = result;
			}
			if ((dest & 4) > 0) {
				regA = result;
			}
			
			int jump = currentInstruction & C_INST_JUMP_MASK;
			boolean shouldJump = false;
			switch(jump) {
			case 0: // no jump
				break;
			case 1:
				if (result > 0) {
					shouldJump = true;
				}
			case 2:
				if (result == 0) {
					shouldJump = true;
				}
			case 3:
				if (result >= 0) {
					shouldJump = true;
				}
			case 4:
				if (result < 0) {
					shouldJump = true;
				}
			case 5:
				if (result != 0) {
					shouldJump = true;
				}
			case 6:
				if (result <= 0) {
					shouldJump = true;
				}
			case 7:
				shouldJump = true;
			}
			currentInstructionAddress = shouldJump ? regA : currentInstructionAddress + 1;
		}
	}
	
	public static void main(String[] args) {
		String program = "0100000000000000\n"  // @a
				       + "1110111111001000\n"  // M=1 (a=1)
				       + "0100000000000001\n"  // @b
				       + "1110111111001000\n"  // M=A (b=1)
				       + "1111110000010000\n"  // D=M (D=b)
				       + "0100000000000000\n"  // @a
				       + "1111110000100000\n"  // A=M (A=a)
				       + "1111000010010000\n"  // D=A+D
				       + "0100000000000010\n"  // @res
				       + "1110001100001000\n"  // M=D (@res=2)
				       + "1110101010000111\n"; // 0;JMP
		CPUEmulator emulator = new CPUEmulator();
		try {
			emulator.loadProgram(program);
			emulator.run();
		} catch (IOException e) {
			System.out.println("Couldn't load program...");
		}
	}
}
