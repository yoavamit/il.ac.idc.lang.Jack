package il.ac.idc.lang.hvm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import il.ac.idc.lang.hvm.Parser.CommandType;

public class VMTranslator {

	private Parser parser;
	private CodeWriter codeWriter;
	
	public void translateFile(File filename) {
		String currentFilename = filename.getName().split(".")[0];
		codeWriter.setFileName(currentFilename);
		try {
			parser = new Parser(new FileInputStream(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		while(parser.hasMoreCommands()) {
			parser.advance();
			CommandType type = parser.commandType();
			String arg1 = parser.arg1();
			int arg2 = parser.arg2();
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
	
	public void translate(String filename, String outputFilename) throws IOException {
		codeWriter = new CodeWriter(new FileOutputStream(new File(outputFilename)));
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
		codeWriter.close();
	}
	
	public static void main(String[] args) {
		String outputFilename = args[1];
		String fileOrDirName = args[0];
		if (fileOrDirName == null) {
			return;
		}
		VMTranslator translator = new VMTranslator();
		try {
			translator.translate(fileOrDirName, outputFilename);	
		} catch (IOException e) {
			
		}
	}
}
