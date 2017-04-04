package il.ac.idc.lang.hvm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import il.ac.idc.lang.hvm.Parser.CommandType;

public class VMTranslator {

	private Parser parser;
	private CodeWriter codeWriter;
	
	public VMTranslator(String outputFilename) throws FileNotFoundException {
		codeWriter = new CodeWriter(new FileOutputStream(new File(outputFilename)));
	}
	
	public void translateFile(File filename, InputStream stream) {
		String currentFilename = filename.getName().split("\\.")[0];
		codeWriter.setFileName(currentFilename);
		try {
			parser = new Parser(stream);
		} catch (IOException e) {
		}
		while(parser.hasMoreCommands()) {
			parser.advance();
			CommandType type = parser.commandType();
			String arg1 = type == CommandType.C_RETURN ? "" : parser.arg1();
			int arg2 = 0;
			if (type == CommandType.C_PUSH || type == CommandType.C_POP || type == CommandType.C_FUNCTION || type == CommandType.C_CALL)
				arg2 = parser.arg2();
			switch(type) {
			case C_ARITHMETIC:
				codeWriter.writeArithmetic(arg1);
			case C_CALL:
				codeWriter.writeCall(arg1, arg2);
			case C_FUNCTION:
				codeWriter.writeFunction(arg1, arg2);
			case C_GOTO:
				codeWriter.writeGoto(arg1);
			case C_IF:
				codeWriter.writeIf(arg1);
			case C_LABEL:
				codeWriter.writeLabel(arg1);
			case C_POP:
				codeWriter.writePushPop(type, arg1, arg2);
			case C_PUSH:
				codeWriter.writePushPop(type, arg1, arg2);
			case C_RETURN:
				codeWriter.writeReturn();
			}
		}
	}
	
	public void translateFile(File filename) {
		try {
			translateFile(filename, new FileInputStream(filename));
		} catch (FileNotFoundException e) {
		}
	}
	
	public void translate(String filename) throws IOException {
		File fileOrDir = new File(filename);
		if (fileOrDir.isFile()) {
			translateFile(fileOrDir);
		} else {
			File[] files = fileOrDir.listFiles();
			for (File file : files) {
				if (file.isFile()) {
					translateFile(file);
				}
			}
		}
	}
	
	public void close() {
		codeWriter.close();
	}
	
	public static void main(String[] args) {
		String fileOrDirName = args[0];
		String outputFilename = args[1];
		if (fileOrDirName == null) {
			return;
		}
		try {
			VMTranslator translator = new VMTranslator(outputFilename);
			translator.translate(fileOrDirName);
			translator.close();
		} catch (IOException e) {
			
		}
	}
}
