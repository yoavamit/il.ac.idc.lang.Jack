package il.ac.idc.lang.assembler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.xtext.util.StringInputStream;

import il.ac.idc.lang.assembler.Parser.CommandType;
import il.ac.idc.lang.emulator.CPUEmulator;

public class Assembler {

	private SymbolTable symbols;
	private Parser parser;
	private int userVariablesAddress = 16;
	private static final int programStartAddress = 128;

	public Assembler(InputStream stream) throws IOException {
		parser = new Parser(stream);
		symbols = new SymbolTable();
	}

	public Integer[] assemble() {
		List<Integer> instructions = new ArrayList<>();
		int currentInstructionAddress = 0;
		while (parser.hasMoreCommands()) {
			parser.advance();
			if (parser.commandType() == CommandType.L_COMMAND) {
				symbols.addEntry(parser.symbol(), currentInstructionAddress + programStartAddress);
			} else {
				currentInstructionAddress++;
			}
		}

		parser.reset();

		while (parser.hasMoreCommands()) {
			parser.advance();
			switch (parser.commandType()) {
			case A_COMMAND:
				String value = parser.symbol();
				boolean isNumeric = false;
				try {
					Integer.parseInt(value);
					isNumeric = true;
				} catch (NumberFormatException e) {

				}
				if (isNumeric) {
					instructions.add(Integer.parseInt(value));
					break;
				}
				if (!symbols.contains(value)) {
					symbols.addEntry(value, userVariablesAddress++);
				}
				int address = symbols.getAddress(value);
				instructions.add(address);
				break;
			case C_COMMAND:
				int instruction = 57344 + Code.comp(parser.comp()) + Code.dest(parser.dest())
						+ Code.jump(parser.jump());
				instructions.add(instruction);
				break;
			case L_COMMAND:
				break;
			}
		}
		Integer[] output = new Integer[instructions.size()];
		instructions.toArray(output);
		return output;
	}

	public static void main(String[] args) {
		String program = "@256\n"
				+ "D=A\n"
				+ "@SP\n"
				+ "M=D\n"
				+ "// push constant 7\n" 
				+ "@7\n" 
				+ "D=A\n" 
				+ "@SP\n" 
				+ "A=M\n" 
				+ "M=D\n" 
				+ "@SP\n" 
				+ "M=M+1\n"
				+ "// push constant 8\n" 
				+ "@8\n" 
				+ "D=A\n" 
				+ "@SP\n" 
				+ "A=M\n" 
				+ "M=D\n" 
				+ "@SP\n" 
				+ "M=M+1\n" 
				+ "// add\n" 
				+ "@SP\n"
				+ "M=M-1\n" 
				+ "A=M\n" 
				+ "D=M\n" 
				+ "M=0\n" 
				+ "@R13\n" 
				+ "M=D\n" 
				+ "@SP\n" 
				+ "M=M-1\n" 
				+ "A=M\n" 
				+ "D=M\n" 
				+ "@R13\n" 
				+ "D=D+M\n"
				+ "@SP\n" 
				+ "A=M\n" 
				+ "M=D\n"
				+ "EOP";
		try {
			Assembler asm = new Assembler(new StringInputStream(program));
			Integer[] instructions = asm.assemble();
			for (Integer instruction : instructions) {
				String inst = Integer.toBinaryString(instruction);
				if (inst.length() < 16) {
					for (int i = inst.length(); i < 16; i++) {
						inst = "0" + inst;
					}
				}
				System.out.println(inst);
			}
			CPUEmulator emulator = new CPUEmulator();
			emulator.loadProgram(instructions);
			emulator.run();
			System.out.println(emulator.getRegM());
		} catch (IOException e) {
			System.out.println("Coudln't read program...");
		}
	}
}
