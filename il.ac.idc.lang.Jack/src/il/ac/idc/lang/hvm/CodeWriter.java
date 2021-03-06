package il.ac.idc.lang.hvm;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Random;

import il.ac.idc.lang.hvm.Parser.CommandType;

public class CodeWriter {

	private PrintWriter out;
	private String currentFilename;
	private String currentFunction;
	
	public CodeWriter(OutputStream stream) {
		out = new PrintWriter(stream);
		currentFilename = "";
		currentFunction = "";
	}
	
	public void setFileName(String filename) {
		currentFilename = filename;
	}
	
	private void writeToStream(String code) {
		out.print(code);
		out.flush();
	}
	
	/**
	 * Writes the assembly code that effects the VM initialization, also called <i>bootstrap  code</i>.
	 * This code must be placed at the beginning of the output file.
	 */
	public void writeInit() {
		String code = "@256\n"
				+ "D=A\n"
				+ "@SP\n"
				+ "M=D\n";
		writeToStream(code);
		writeCall("Sys.init", 0);
	}
	
	/**
	 * Writes the assembly code that is the translation of the <b>label</b> command.
	 * @param label
	 */
	public void writeLabel(String label) {
		String code = "(" + currentFunction + "." + label + ")\n";
		writeToStream(code);
	}
	
	/**
	 * Writes the assembly code that is the translation of the <b>goto</b> command.
	 * @param label
	 */
	public void writeGoto(String label) {
		String code = "@" + currentFunction + "." + label + "\n"
				+ "0:JMP\n";
		writeToStream(code);
	}
	
	/**
	 * Writes the assembly code that is the translation of the <b>if-goto</b> command.
	 * @param label
	 */
	public void writeIf(String label) {
		String randomLabel = "";
		Random random = new Random();
		char[] characters = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
		for (int i = 0; i < 8; i++) {
			randomLabel += characters[random.nextInt(characters.length)];
		}
		System.out.println("// if-goto " + label);
		String code = "@SP\n"
				+ "M=M-1\n"
				+ "A=M\n"
				+ "D=M\n"
				+ "M=0\n"
				+ "@IF_GOTO-" + randomLabel + "\n"
				+ "D;JEQ\n"
				+ "@" + currentFunction + "." + label + "\n"
				+ "0;JMP\n"
				+ "(IF_GOTO-" + randomLabel + ")\n";
		writeToStream(code);
	}
	
	/**
	 * Writes the assembly code that is the translation of the <b>call</b> command.
	 * @param functionName
	 * @param numArgs
	 */
	public void writeCall(String functionName, int numArgs) {
		String returnAddressLabel = "";
		Random random = new Random(0);
		char[] characters = new char[] {'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
		for (int i = 0; i < 8; i++) {
			returnAddressLabel += characters[random.nextInt(characters.length)];
		}
		System.out.println("// call + " + currentFilename + "." + functionName + " " + numArgs);
		String code = "@" + returnAddressLabel + "\n"
				+ "D=A\n"
				+ "@SP\n"
				+ "A=M\n"
				+ "M=D\n"
				+ "@SP\n"
				+ "M=M+1\n"
				+ "@LCL\n"
				+ "D=M\n"
				+ "@SP\n"
				+ "A=M\n"
				+ "M=D\n"
				+ "@SP\n"
				+ "M=M+1\n"
				+ "@ARG\n"
				+ "D=M\n"
				+ "@SP\n"
				+ "A=M\n"
				+ "M=D\n"
				+ "@SP\n"
				+ "M=M+1\n"
				+ "@THIS\n"
				+ "D=M\n"
				+ "@SP\n"
				+ "A=M\n"
				+ "M=D\n"
				+ "@SP\n"
				+ "M=M+1\n"
				+ "@THAT\n"
				+ "D=M\n"
				+ "@SP\n"
				+ "A=M\n"
				+ "M=D\n"
				+ "@SP\n"
				+ "M=M+1\n"
				+ "@SP\n"
				+ "D=M\n"
				+ "@R13\n"
				+ "M=D\n"
				+ "@5\n"
				+ "D=A\n"
				+ "@R13\n"
				+ "M=M-D\n"
				+ "@" + numArgs + "\n"
				+ "D=A\n"
				+ "@R13\n"
				+ "D=M-D\n"
				+ "@ARG\n"
				+ "M=D\n"
				+ "@SP\n"
				+ "D=M\n"
				+ "@LCL\n"
				+ "M=D\n"
				+ "@" + currentFilename + "." + functionName + "\n"
				+ "0;JMP\n"
				+ "(" + returnAddressLabel + ")\n";
		writeToStream(code);
	}
	
	/**
	 * Writes the assembly code that is the translation of the <b>return</b> command.
	 */
	public void writeReturn() {
		System.out.println("// return");
		String code = "@LCL\n"
				+ "D=M\n"
				+ "@R13\n" // R13 now holds the LCL address
				+ "M=D\n"
				+ "@R14\n"
				+ "M=D\n"
				+ "@5\n"
				+ "D=A\n"
				+ "@R14\n"
				+ "M=M-D\n"
				+ "A=M\n"
				+ "D=M\n"
				+ "@R14\n"
				+ "M=D\n" // R14 holds the return address now
				+ "@SP\n"
				+ "M=M-1\n"
				+ "A=M\n"
				+ "D=M\n"
				+ "@ARG\n"
				+ "A=M\n"
				+ "M=D\n" // argument 0 now holds the return value
				+ "@ARG\n"
				+ "D=M+1\n"
				+ "@SP\n"
				+ "M=D\n" // SP now points to ARG + 1
				+ "@R13\n"
				+ "M=M-1\n"
				+ "A=M\n"
				+ "D=M\n"
				+ "@THAT\n"
				+ "M=D" // restore THAT
				+ "@R13\n"
				+ "M=M-1\n"
				+ "A=M\n"
				+ "D=M\n"
				+ "@THIS\n"
				+ "M=D\n" // restore THIS
				+ "@R13\n"
				+ "M=M-1\n"
				+ "A=M\n"
				+ "D=M\n"
				+ "@ARG\n"
				+ "M=D\n" // restore ARG
				+ "@R13\n"
				+ "M=M-1\n"
				+ "A=M\n"
				+ "D=M\n"
				+ "@LCL\n" // restore LCL
				+ "M=D\n"
				+ "@14\n"
				+ "A=M\n" // goto the return address
				+ "0;JMP";
		writeToStream(code);
	}
	
	/**
	 * Writes the assembly code that is the translation of the given <b>function</b> command.
	 * @param functioname
	 * @param numLocals
	 */
	public void writeFunction(String functionName, int numLocals) {
		System.out.println("// function " + functionName + " " + numLocals);
		currentFunction = functionName;
		String functionAsm = "(" + currentFilename + ":" + functionName + ")\n";
		for (int i = 0; i < numLocals; i++) {
			functionAsm += "@SP\n"
					+ "A=M\n"
					+ "M=0\n"
					+ "@SP\n"
					+ "M=M+1\n";
		}
		writeToStream(functionAsm);
	}
	
	/**
	 *  Writes the assembly code that is the translation of the given arithmetic command.
	 * @param command
	 */
	public void writeArithmetic(String command) {
		String randomLabelSuffix = "";
		Random random = new Random();
		char[] characters = new char[] {'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
		for (int i = 0; i < 8; i++) {
			randomLabelSuffix += characters[random.nextInt(characters.length)];
		}
		String code = null;
		switch(command) {
		case "add":
			System.out.println("// add");
			code = "@SP\n"
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
					+ "@SP\n"
					+ "M=M+1\n";
			break;
		case "sub":
			System.out.println("// sub");
			code = "@SP\n"
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
					+ "D=D-M\n"
					+ "@SP\n"
					+ "A=M\n"
					+ "M=D\n"
					+ "@SP\n"
					+ "M=M+1\n";
			break;
		case "neg":
			System.out.println("// neg");
			code = "@SP\n"
					+ "M=M-1\n"
					+ "A=M\n"
					+ "M=-M\n"
					+ "@SP\n"
					+ "M=M+1\n";
			break;
		case "eq":
			System.out.println("// eq");
			code = "@SP\n"
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
					+ "D=D-M\n"
					+ "@IF_TRUE_EQ-" + randomLabelSuffix + "\n"
					+ "D;JEQ"
					+ "D=0\n"
					+ "@END_EQ-" + randomLabelSuffix + "\n"
					+ "0;JMP\n"
					+ "(IF_TRUE_EQ-" + randomLabelSuffix + ")\n"
					+ "D=-1\n"
					+ "(END_EQ-" + randomLabelSuffix + ")\n"
					+ "@SP\n"
					+ "A=M\n"
					+ "M=D\n"
					+ "@SP\n"
					+ "M=M+1\n";
			break;
		case "gt":
			System.out.println("// gt");
			code = "@SP\n"
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
					+ "D=D-M\n"
					+ "@IF_TRUE_GT-" + randomLabelSuffix + "\n"
					+ "D;JGT"
					+ "D=0\n"
					+ "@END_GT-" + randomLabelSuffix + "\n"
					+ "0;JMP\n"
					+ "(IF_TRUE_GT-" + randomLabelSuffix + ")\n"
					+ "D=-1\n"
					+ "(END_GT-" + randomLabelSuffix + ")\n"
					+ "@SP\n"
					+ "A=M\n"
					+ "M=D\n"
					+ "@SP\n"
					+ "M=M+1\n";
		case "lt":
			System.out.println("// lt");
			code = "@SP\n"
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
					+ "D=M-D\n"
					+ "@IF_TRUE_LT-" + randomLabelSuffix + "\n"
					+ "D;JGT"
					+ "D=0\n"
					+ "@END_LT-" + randomLabelSuffix + "\n"
					+ "0;JMP\n"
					+ "(IF_TRUE_LT-" + randomLabelSuffix + ")\n"
					+ "D=-1\n"
					+ "(END_LT-" + randomLabelSuffix + ")\n"
					+ "@SP\n"
					+ "A=M\n"
					+ "M=D\n"
					+ "@SP\n"
					+ "M=M+1\n";
		case "and":
			System.out.println("// and");
			code = "@SP\n"
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
					+ "D=D&M\n"
					+ "@SP\n"
					+ "A=M\n"
					+ "M=D\n"
					+ "@SP\n"
					+ "M=M+1\n";
			break;
		case "or":
			System.out.println("// or");
			code = "@SP\n"
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
					+ "D=D|M\n"
					+ "@SP\n"
					+ "A=M\n"
					+ "M=D\n"
					+ "@SP\n"
					+ "M=M+1\n";
			break;
		case "not":
			System.out.println("// not");
			code = "@SP\n"
					+ "M=M-1\n"
					+ "A=M\n"
					+ "M=!M\n"
					+ "@SP\n"
					+ "M=M+1\n";
		}
		if (code != null)
			writeToStream(code);
	}
	
	/**
	 * Writes the assembly code that is the translation of the given command, 
	 * where command is either C_PUSH or C_POP.
	 * @param command
	 * @param segment
	 * @param index
	 */
	public void writePushPop(CommandType command, String segment, int index) {
		String code = null;
		if (command == CommandType.C_PUSH) {
			switch(segment) {
			case "constant":
				System.out.println("// push " + segment + " " + index);
				code = "@" + index + "\n" 
						+ "D=A\n"
						+ "@SP\n"
						+ "A=M\n"
						+ "M=D\n"
						+ "@SP\n"
						+ "M=M+1\n";
				break;
			case "static":
				System.out.println("// push static " + index);
				code = "@16\n"
						+ "D=A\n"
						+ "@" + index + "\n"
						+ "D=D+A\n"
						+ "@SP\n"
						+ "A=M\n"
						+ "M=D\n"
						+ "@SP\n"
						+ "M=M+1\n";
				break;
			case "local":
				System.out.println("// push local " + index);
				code = "@LCL\n"
						+ "D=M\n"
						+ "@" + index + "\n"
						+ "D=D+A\n"
						+ "@SP\n"
						+ "A=M\n"
						+ "M=D\n"
						+ "@SP\n"
						+ "M=M+1\n";
				break;
			case "argument":
				System.out.println("// push argument " + index);
				code = "@" + index + "\n"
						+ "D=A\n"
						+ "@ARG\n"
						+ "D=D+M\n"
						+ "@SP\n"
						+ "A=M\n"
						+ "M=D\n"
						+ "@SP\n"
						+ "M=M+1\n";
				break;
			case "this":
				System.out.println("// push this " + index);
				code = "@" + index + "\n"
						+ "D=A\n"
						+ "@THIS\n"
						+ "D=D+M\n"
						+ "@SP\n"
						+ "A=M\n"
						+ "M=D\n"
						+ "@SP\n"
						+ "M=M+1\n";
				break;
			case "that":
				System.out.println("// push that " + index);
				code = "@" + index + "\n"
						+ "D=A\n"
						+ "@THAT\n"
						+ "D=D+M\n"
						+ "@SP\n"
						+ "A=M\n"
						+ "M=D\n"
						+ "@SP\n"
						+ "M=M+1\n";
				break;
			case "pointer":
				System.out.println("// push pointer " + index);
				code = "@THIS\n"
						+ "D=A\n"
						+ "@" + index + "\n"
						+ "A=D+A\n"
						+ "D=M\n"
						+ "@SP\n"
						+ "A=M\n"
						+ "M=D\n"
						+ "@SP\n"
						+ "M=M+1";
				break;
			case "temp":
				int address = index + 5;
				System.out.println("// push temp " + index);
				code = "@SP\n"
						+ "A=M\n"
						+ "D=M\n"
						+ "M=0\n"
						+ "@SP\n"
						+ "M=M-1\n"
						+ "@" + address + "\n"
						+ "M=D\n";
				break;
			}
		} else if (command == CommandType.C_POP) {
			switch(segment) {
			case "constant":
				System.out.println("// pop constant " + index);
				code = "@SP\n"
						+ "A=M\n"
						+ "D=M\n"
						+ "M=0\n"
						+ "@SP\n"
						+ "M=M-1\n"
						+ "@" + index + "\n"
						+ "M=D\n";
				break;
			case "static":
				System.out.println("// pop static " + index);
				code = "@SP\n"
						+ "A=M\n"
						+ "D=M\n"
						+ "M=0\n"
						+ "@SP\n"
						+ "M=M-1\n"
						+ "@R13\n"
						+ "M=D\n"
						+ "@16\n"
						+ "D=A\n"
						+ "@" + index + "\n"
						+ "D=D+A\n"
						+ "@R14\n" 
						+ "M=D\n"// R14 = static + (index)
						+ "@R13\n"
						+ "D=M\n"
						+ "@R14\n"
						+ "A=M\n"
						+ "M=D\n";
				break;
			case "local":
				System.out.println("// pop local " + index);
				code = "@SP\n"
						+ "A=M\n"
						+ "D=M\n"
						+ "M=0\n"
						+ "@SP\n"
						+ "M=M-1\n"
						+ "@R13\n"
						+ "M=D\n"
						+ "@" + index + "\n"
						+ "D=A\n"
						+ "@LCL\n"
						+ "A=M\n"
						+ "D=D+M\n"
						+ "@R14\n"
						+ "M=D\n"
						+ "@R13\n"
						+ "D=M\n"
						+ "@R14\n"
						+ "A=M\n"
						+ "M=D\n";
				break;
			case "argument":
				System.out.println("// pop argument " + index);
				code = "@SP\n"
						+ "A=M\n"
						+ "D=M\n"
						+ "M=0\n"
						+ "@SP\n"
						+ "M=M-1\n"
						+ "@R13\n"
						+ "M=D\n"
						+ "@" + index + "\n"
						+ "D=A\n"
						+ "@ARG\n"
						+ "A=M\n"
						+ "D=D+M\n"
						+ "@R14\n"
						+ "M=D\n"
						+ "@R13\n"
						+ "D=M\n"
						+ "@R14\n"
						+ "A=M\n"
						+ "M=D\n";
				break;
			case "this":
				System.out.println("// pop this " + index);
				code = "@SP\n"
						+ "A=M\n"
						+ "D=M\n"
						+ "M=0\n"
						+ "@SP\n"
						+ "M=M-1\n"
						+ "@R13\n"
						+ "M=D\n"
						+ "@THIS\n"
						+ "D=A\n"
						+ "@" + index + "\n"
						+ "D=D+A\n"
						+ "@R14\n"
						+ "M=D\n"
						+ "@R13\n"
						+ "D=M\n"
						+ "@R14\n"
						+ "A=M\n"
						+ "M=D\n";
				break;
			case "that":
				System.out.println("// pop that " + index);
				code = "@SP\n"
						+ "A=M\n"
						+ "D=M\n"
						+ "M=0\n"
						+ "@SP\n"
						+ "M=M-1\n"
						+ "@R13\n"
						+ "M=D\n"
						+ "@THAT\n"
						+ "D=A\n"
						+ "@" + index + "\n"
						+ "D=D+A\n"
						+ "@R14\n"
						+ "M=D\n"
						+ "@R13\n"
						+ "D=M\n"
						+ "@R14\n"
						+ "A=M\n"
						+ "M=D\n";
				break;
			case "pointer":
				System.out.println("// pop pointer " + index);
				String reg = index == 0 ? "THIS" : "THAT";
				code = "@SP\n"
						+ "A=M\n"
						+ "D=M\n"
						+ "M=0\n"
						+ "@SP\n"
						+ "M=M-1\n"
						+ "@" + reg + "\n"
						+ "M=D\n";
				break;
			case "temp":
				System.out.println("// pop temp " + index);
				int address = index + 5;
				code = "@SP\n"
						+ "A=M\n"
						+ "D=M\n"
						+ "M=0\n"
						+ "@SP\n"
						+ "M=M-1\n"
						+ "@" + address + "\n"
						+ "M=D\n";
				break;
			}
		}
		if (code != null)
			writeToStream(code);
	}
	
	public void close() {
		out.flush();
		out.close();
	}
}
