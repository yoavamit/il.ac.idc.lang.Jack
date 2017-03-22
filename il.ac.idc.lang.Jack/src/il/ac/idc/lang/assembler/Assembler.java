package il.ac.idc.lang.assembler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
		String filename = null;
		String format = "strings";
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--format")) {
				format = args[i+1];
			}
		}
		filename = args[args.length - 1];
		
		try {
			File file = new File(filename);
			Assembler asm = new Assembler(new FileInputStream(file));
			FileOutputStream output = null;
			if (file.isDirectory()) {
				output = new FileOutputStream(new File(file.getPath() + "a.hack"));
			} else {
				output = new FileOutputStream(new File(file.getParentFile().getPath() + File.separator + "a.hack"));
			}
			Integer[] instructions = asm.assemble();
			switch (format) {
			case "binary":
				for (Integer inst : instructions) {
					output.write(inst);
				}
				break;
			case "strings":
				PrintWriter out = new PrintWriter(output);
				for (Integer instruction : instructions) {
					String inst = Integer.toBinaryString(instruction);
					if (inst.length() < 16) {
						for (int i = inst.length(); i < 16; i++) {
							inst = "0" + inst;
						}
					}
					out.println(inst);
				}
				out.flush();
				out.close();
				break;
			}
			output.close();
		} catch (IOException e) {
			System.out.println("Coudln't read program...");
		}
	}
}
