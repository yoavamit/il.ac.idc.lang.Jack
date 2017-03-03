package il.ac.idc.lang.assembler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.xtext.util.StringInputStream;

import il.ac.idc.lang.assembler.Parser.CommandType;

public class Assembler {

	private SymbolTable symbols;
	private Parser parser;
	private int userVariablesAddress = 16;
	private static final int programStartAddress = 128;
	
	public Assembler(InputStream stream) throws IOException {
		parser = new Parser(stream);
		symbols = new SymbolTable();
	}
	
	public List<Integer> assemble() {
		List<Integer> instructions = new ArrayList<>();
		int currentInstructionAddress = 0;
		while(parser.hasMoreCommands()) {
			parser.advance();
			if (parser.commandType() == CommandType.L_COMMAND) {
				symbols.addEntry(parser.symbol(), currentInstructionAddress + programStartAddress);
			} else {
				currentInstructionAddress++;
			}
		}
		
		parser.reset();
		
		while(parser.hasMoreCommands()) {
			parser.advance();
			switch(parser.commandType()) {
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
				int instruction = 57344 + 
				Code.comp(parser.comp()) + 
				Code.dest(parser.dest()) + 
				Code.jump(parser.jump());
				instructions.add(instruction);
				break;
			case L_COMMAND:
				break;
			}
		}
		return instructions;
	}
	
	public static void main(String[] args) {
		String program = "// Computes 1+...+RAM[0]\n"
				+ "// And store the sum in RAM[1]\n"
				+ "@i\n"
				+ "M=1 // i = 1\n"
				+ "@sum\n"
				+ "M=0 // sum = 0\n"
				+ "(LOOP)\n"
				+ "@i // if i>RAM[0] goto WRITE\n"
				+ "D=M\n"
				+ "@R0\n"
				+ "D=D-M\n"
				+ "@WRITE\n"
				+ "D;JGT\n"
				+ "@i // sum += i\n"
				+ "D=M\n"
				+ "@sumv"
				+ "M=D+M\n"
				+ "@i // i++\n"
				+ "M=M+1\n"
				+ "@LOOP // goto LOOP\n"
				+ "0;JMP\n"
				+ "(WRITE)\n"
				+ "@sum\n"
				+ "D=M\n"
				+ "@R1\n"
				+ "M=D // RAM[1] = the sum\n"
				+ "(END)\n"
				+ "@END\n"
				+ "0;JMP";
		try {
			Assembler asm = new Assembler(new StringInputStream(program));
			List<Integer> instructions = asm.assemble();
			for (Integer instruction : instructions) {
				String inst = Integer.toBinaryString(instruction);
				if (inst.length() < 16) {
					for (int i = inst.length(); i < 16; i++) {
						inst = "0" + inst;
					}
				}
				System.out.println(inst);
			}
		} catch (IOException e) {
			System.out.println("Coudln't read program...");
		}
	}
}
