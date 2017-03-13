package il.ac.idc.lang.compiler;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.xtext.util.StringInputStream;

import il.ac.idc.lang.compiler.Tokenizer.Keyword;
import il.ac.idc.lang.compiler.Tokenizer.TokenType;

public class CompilationEngine {

	private class VariableEntry {
		String name;
		Object type;

		public VariableEntry(String name, Object type) {
			this.name = name;
			this.type = type;
		}

		@Override
		public boolean equals(Object other) {
			System.out.println("comparing to " + other.toString());
			if (other instanceof VariableEntry) {
				VariableEntry entry = (VariableEntry) other;
				return entry.name.equals(this.name) && entry.type.equals(this.type);
			}
			if (other instanceof String) {
				return name.equals(other);
			}
			return false;
		}
	}

	private Tokenizer tokenizer;
	private BufferedOutputStream output;
	private String indent = "";
	private List<VariableEntry> instanceVars;
	private List<VariableEntry> classVars;
	private List<VariableEntry> scopeVars;
	private List<VariableEntry> scopeArgVars;
	private String className;
	private String currentSubroutine;

	private int indexOfVar(List<VariableEntry> list, String name) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).name.equals(name)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Create a new CompilatopnEngine with the given input and output. he next
	 * method to be called must be compileClass().
	 * 
	 * @param in
	 * @param out
	 */
	public CompilationEngine(InputStream in, OutputStream out) throws IOException {
		tokenizer = new Tokenizer(in);
		output = new BufferedOutputStream(out);
		instanceVars = new ArrayList<>();
		classVars = new ArrayList<>();
		scopeVars = new ArrayList<>();
		scopeArgVars = new ArrayList<>();
	}

	private void writeToOutput(String str) {
		byte[] b = (indent + str + "\n").getBytes();
		try {
			output.write(b, 0, b.length);
		} catch (IOException e) {

		}
	}

	private void writeIdentifier() throws CompilationException {
		if (!(tokenizer.tokenType() == TokenType.IDENTIFIER)) {
			throw new CompilationException(
					"Unexpected token of type " + tokenizer.tokenType() + ", expected \"identifier\"");
		}
		writeToOutput("<identifier>" + tokenizer.identifier() + "</identifier>");
		processNext();
	}

	private void writeKeyword(Keyword[] expected) throws CompilationException {
		String expectedKeyword = "";
		for (int i = 0; i < expected.length; i++) {
			expectedKeyword += expected[i].toString() + ", ";
		}
		if (!(tokenizer.tokenType() == TokenType.KEYWORD)) {
			throw new CompilationException(
					"Unexpected token of type " + tokenizer.tokenType() + ", expected \"keyword\"");
		}
		boolean valid = false;
		Keyword key = tokenizer.keyword();
		for (int i = 0; i < expected.length; i++) {
			if (key == expected[i]) {
				valid = true;
				break;
			}
		}
		if (valid) {
			writeToOutput("<keyword>" + tokenizer.keyword().getValue() + "</keyword>");
			processNext();
		} else {
			throw new CompilationException(
					"Unexpected keyword: " + key.getValue() + ", expected one of " + expectedKeyword);
		}
	}

	private void writeSymbol(char[] expected) throws CompilationException {
		TokenType type = tokenizer.tokenType();
		String expectedString = "";
		for (int i = 0; i < expected.length; i++) {
			expectedString += expected[i] + ",";
		}
		if (type != TokenType.SYMBOL) {
			throw new CompilationException("Unexpected token of type " + type + ", expected one of :" + expectedString);
		}
		char sym = tokenizer.symbol();
		if (expected != null && expected.length > 0) {
			boolean valid = false;
			for (int i = 0; i < expected.length; i++) {
				if (sym == expected[i]) {
					valid = true;
					break;
				}
			}
			if (valid) {
				writeToOutput("<symbol>" + sym + "</symbol>");
				processNext();
			} else {
				throw new CompilationException("Unexpected symbol: " + sym + ". Should be one of " + expectedString);
			}
		}
	}

	private void processNext() throws CompilationException {
		if (!tokenizer.hasMoreTokens()) {
			throw new CompilationException("Unexpected end of input");
		}
		tokenizer.advance();
	}

	/**
	 * Compiles and complete class.
	 */
	public void compileClass() throws CompilationException {
		processNext();
		if (tokenizer.tokenType() == TokenType.KEYWORD) {
			writeToOutput("<class>");
			indent += "    ";
			writeKeyword(new Keyword[] { Keyword.CLASS }); // class
			className = tokenizer.identifier();
			writeIdentifier(); // className
			writeSymbol(new char[] { '{' });
			if (tokenizer.tokenType() == TokenType.KEYWORD) {
				while (tokenizer.keyword() == Keyword.STATIC || tokenizer.keyword() == Keyword.FIELD) {
					compileClassVarDecl();
				}
				while (tokenizer.keyword() == Keyword.FUNCTION || tokenizer.keyword() == Keyword.CONSTRUCTOR
						|| tokenizer.keyword() == Keyword.METHOD) {
					compileSubroutine();
				}
			}
			if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
				writeToOutput("<symbol>}</symbol>");
			}
		}
		if (!tokenizer.hasMoreTokens()) {
			indent = indent.substring(4);
			writeToOutput("</class>");
		} else {
			throw new CompilationException("Unexpected trailing text at end of class found");
		}
	}

	private void compileType() throws CompilationException {
		try {
			writeKeyword(new Keyword[] { Keyword.INT, Keyword.CHAR, Keyword.BOOLEAN });
		} catch (CompilationException e) {
			writeIdentifier();
		}
	}

	private void addVar(Keyword modifier, Object type, String name) {
		if (modifier == Keyword.STATIC) {
			classVars.add(new VariableEntry(name, type));
		} else {
			instanceVars.add(new VariableEntry(name, type));
		}
	}

	/**
	 * Compiles a static declaration and or a field declaration.
	 */
	public void compileClassVarDecl() throws CompilationException {
		writeToOutput("<classVarDec>");
		indent += "    ";
		Keyword modifier = tokenizer.keyword();
		writeKeyword(new Keyword[] { Keyword.STATIC, Keyword.FIELD }); // static/field
		Object type = tokenizer.tokenType() == TokenType.KEYWORD ? tokenizer.keyword() : tokenizer.identifier();
		compileType();
		String varName = tokenizer.identifier();
		writeIdentifier();
		addVar(modifier, type, varName);
		while (tokenizer.symbol() != ';') {
			writeSymbol(new char[] { ',' });
			varName = tokenizer.identifier();
			writeIdentifier();
			addVar(modifier, type, varName);
		}
		writeSymbol(new char[] { ';' });
		indent = indent.substring(4);
		writeToOutput("</classVarDec>");
	}

	/**
	 * Compiles a complete method, function or constructor.
	 */
	public void compileSubroutine() throws CompilationException {
		writeToOutput("<subroutine>");
		indent += "    ";
		writeKeyword(new Keyword[] { Keyword.CONSTRUCTOR, Keyword.FUNCTION, Keyword.METHOD });
		try {
			compileType();
		} catch (CompilationException e) {
			writeKeyword(new Keyword[] { Keyword.VOID });
		}
		currentSubroutine = tokenizer.identifier();
		writeIdentifier();
		System.out.println("(" + className + ":" + currentSubroutine + ")");
		writeSymbol(new char[] { '(' });
		compileParametersList();
		writeSymbol(new char[] { ')' });
		writeSymbol(new char[] { '{' });

		// write variables
		while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
			compileVarDecl();
		}
		// write statements
		compileStatements();
		scopeArgVars.clear();
		scopeVars.clear();
		writeSymbol(new char[] { '}' });
		indent = indent.substring(4);
		writeToOutput("</subroutine>");
	}

	/**
	 * Compiles a (possibly empty) parameter list, not including the enclosing
	 * "()".
	 */
	public void compileParametersList() throws CompilationException {
		writeToOutput("<parameterList>");
		indent += "    ";
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
			Object type = tokenizer.tokenType() == TokenType.KEYWORD ? tokenizer.keyword() : tokenizer.identifier();
			compileType();
			String name = tokenizer.identifier();
			writeIdentifier();
			scopeArgVars.add(new VariableEntry(name, type));
			try {
				writeSymbol(new char[] { ',' });
			} catch (CompilationException e) {
			}
		}
		indent = indent.substring(4);
		writeToOutput("</parameterList>");
	}

	/**
	 * Compiles and var declaration.
	 */
	public void compileVarDecl() throws CompilationException {
		writeToOutput("<varDec>");
		indent += "    ";
		writeKeyword(new Keyword[] { Keyword.VAR });
		Object type = tokenizer.tokenType() == TokenType.KEYWORD ? tokenizer.keyword() : tokenizer.identifier();
		compileType(); // type
		String name = tokenizer.identifier();
		writeIdentifier(); // varName
		scopeVars.add(new VariableEntry(name, type));
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
			writeSymbol(new char[] { ',' });
			name = tokenizer.identifier();
			writeIdentifier();
			scopeVars.add(new VariableEntry(name, type));
		}
		writeSymbol(new char[] { ';' });
		indent = indent.substring(4);
		writeToOutput("</varDec>");
	}

	/**
	 * Compiles and sequence of statements, not including the enclosing "{}".
	 */
	public void compileStatements() throws CompilationException {
		writeToOutput("<statements>");
		indent += "    ";
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}')) {
			switch (tokenizer.keyword()) {
			case DO:
				compileDo();
				break;
			case LET:
				compileLet();
				break;
			case IF:
				compileIf();
				break;
			case WHILE:
				compileWhile();
				break;
			case RETURN:
				compileReturn();
				break;
			default:
				break;
			}
		}
		indent = indent.substring(4);
		writeToOutput("</statements>");
	}

	/**
	 * Compiles a do statement.
	 */
	public void compileDo() throws CompilationException {
		writeToOutput("<doStatement>");
		indent += "    ";
		writeKeyword(new Keyword[] { Keyword.DO });
		writeToOutput("<subroutineCall>");
		indent += "    ";
		String className = null;
		String functionName = tokenizer.identifier();
		writeIdentifier();
		TokenType type = tokenizer.tokenType();
		if (type == TokenType.SYMBOL) {
			writeSymbol(new char[] { '.' });
			className = functionName;
			functionName = tokenizer.identifier();
			writeIdentifier();
		}
		if (className == null) {
			System.out.println("push pointer 0");
			className = this.className;
		} else {
			int index = indexOfVar(scopeArgVars, className);
			if (index != -1) {
				System.out.println("push argument " + index);
				className = scopeArgVars.get(index).type.toString();
			} else if (indexOfVar(scopeVars, className) != -1) {
				index = indexOfVar(scopeVars, className);
				System.out.println("push local " + scopeVars.indexOf(className));
				className = scopeVars.get(index).type.toString();
			} else if (indexOfVar(instanceVars, className) != -1) {
				index = indexOfVar(instanceVars, className);
				System.out.println("push this " + instanceVars.indexOf(className));
				className = instanceVars.get(index).type.toString();
			} else if (indexOfVar(classVars, className) != -1) {
				index = indexOfVar(classVars, className);
				System.out.println("push static " + classVars.indexOf(className));
				className = classVars.get(index).type.toString();
			}
		}
		writeSymbol(new char[] { '(' });
		compileExpressionList();
		writeSymbol(new char[] { ')' });
		writeToOutput("</subroutineCall>");
		indent = indent.substring(4);
		writeSymbol(new char[] { ';' });
		System.out.println("call " + className + ":" + functionName);
		indent = indent.substring(4);
		writeToOutput("</doStatement>");
	}

	/**
	 * Compiles a let statement.
	 */
	public void compileLet() throws CompilationException {
		writeToOutput("<letStatement>");
		indent += "    ";
		writeKeyword(new Keyword[] { Keyword.LET });
		String assignTo = tokenizer.identifier();
		writeIdentifier();
		if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
			writeSymbol(new char[] { '[' });
			compileExpression();
			writeSymbol(new char[] { ']' });
		}
		writeSymbol(new char[] { '=' });
		compileExpression();
		writeSymbol(new char[] { ';' });
		int index = indexOfVar(scopeArgVars, assignTo);
		if (index != -1) {
			System.out.println("pop argument " + index);
		} else if (indexOfVar(scopeVars, assignTo) != -1) {
			System.out.println("pop local " + indexOfVar(scopeVars, assignTo));
		} else if (indexOfVar(instanceVars, assignTo) != -1) {
			System.out.println("pop this " + indexOfVar(instanceVars, assignTo));
		} else if (indexOfVar(classVars, assignTo) != -1) {
			System.out.println("pop static " + indexOfVar(classVars, assignTo));
		}
		indent = indent.substring(4);
		writeToOutput("</letStatement>");
	}

	/**
	 * Compiles a while statement.
	 */
	public void compileWhile() throws CompilationException {
		writeToOutput("<whileStatement>");
		indent += "    ";
		writeKeyword(new Keyword[] { Keyword.WHILE });
		writeSymbol(new char[] { '(' });
		compileExpression();
		writeSymbol(new char[] { ')' });
		writeSymbol(new char[] { '{' });
		compileStatements();
		writeSymbol(new char[] { '}' });
		indent = indent.substring(4);
		writeToOutput("</whileStatement>");
	}

	/**
	 * Compiles a return statement.
	 */
	public void compileReturn() throws CompilationException {
		writeToOutput("<returnStatement>");
		indent += "    ";
		writeKeyword(new Keyword[] { Keyword.RETURN });
		if (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
			compileExpression();
		} else {
			System.out.println("push constant 0");
		}
		writeSymbol(new char[] { ';' });
		indent = indent.substring(4);
		writeToOutput("</returnStatement>");
	}

	/**
	 * Compiles an if statement, possibly with a trailing else clause.
	 */
	public void compileIf() throws CompilationException {
		writeToOutput("<ifStatement>");
		indent += "    ";
		writeKeyword(new Keyword[] { Keyword.IF });
		writeSymbol(new char[] { '(' });
		compileExpression();
		writeSymbol(new char[] { ')' });
		writeSymbol(new char[] { '{' });
		compileStatements();
		writeSymbol(new char[] { '}' });
		try {
			writeKeyword(new Keyword[] { Keyword.ELSE });
			writeSymbol(new char[] { '{' });
			compileStatements();
			writeSymbol(new char[] { '}' });
		} catch (CompilationException e) {
			tokenizer.advance();
		}
		indent = indent.substring(4);
		writeToOutput("</ifStatement>");

	}

	/**
	 * Compiles an expression.
	 */
	public void compileExpression() throws CompilationException {
		writeToOutput("<expression>");
		indent += "    ";
		compileTerm();
		while (tokenizer.tokenType() == TokenType.SYMBOL && (tokenizer.symbol() != ';' && tokenizer.symbol() != ',')
				&& tokenizer.symbol() != ')') {
			char sym = tokenizer.symbol();
			writeSymbol(new char[] { '+', '-', '*', '/', '&', '|', '<', '>', '=' });
			compileTerm();
			switch (sym) {
			case '+':
				System.out.println("add");
				break;
			case '-':
				System.out.println("sub");
				break;
			case '*':
				// TODO
				System.out.println("mult");
				break;
			case '/':
				// TODO
				System.out.println("div");
				break;
			case '&':
				System.out.println("and");
				break;
			case '|':
				System.out.println("or");
				break;
			case '<':
				System.out.println("lt");
				break;
			case '>':
				System.out.println("gt");
				break;
			case '=':
				System.out.println("eq");
			}
		}
		indent = indent.substring(4);
		writeToOutput("</expression>");
	}

	/**
	 * Compiles a terminal. This method is faced with a slight difficulty when
	 * trying to decide between some of the alternative parsing rules.
	 * Specifically, if the current token is an identifier, the method must
	 * distinguish between a variable, an array entry and a subroutine call. A
	 * single look-ahead token, which may be one of "(", "[" or "." suffices to
	 * distinguish between the three possibilities. Any other token is not part
	 * of this of this term and should not be advanced over.
	 */
	public void compileTerm() throws CompilationException {
		writeToOutput("<term>");
		indent += "    ";
		switch (tokenizer.tokenType()) {
		case INT_CONST:
			writeToOutput("<intConst>" + tokenizer.intVal() + "</intConst>");
			System.out.println("push constant " + tokenizer.intVal());
			tokenizer.advance();
			break;
		case STRING_CONST:
			writeToOutput("<stringConst>" + tokenizer.stringVal() + "</stringConst>");
			tokenizer.advance();
			break;
		case SYMBOL:
			char sym = tokenizer.symbol();
			switch (sym) {
			case '-':
				writeSymbol(new char[] { '-' });
				compileTerm();
				System.out.println("neg");
				break;
			case '~':
				writeSymbol(new char[] { '~' });
				compileTerm();
				System.out.println("not");
				break;
			case '(':
				writeSymbol(new char[] { '(' });
				compileExpression();
				writeSymbol(new char[] { ')' });
				break;
			}
			break;
		case KEYWORD:
			switch (tokenizer.keyword()) {
			case THIS:
				writeKeyword(new Keyword[] { Keyword.THIS });
				System.out.println("push pointer 0");
				break;
			case NULL:
				writeKeyword(new Keyword[] { Keyword.NULL });
				// TODO
				break;
			case TRUE:
				writeKeyword(new Keyword[] { Keyword.TRUE });
				System.out.println("push constant 1");
				System.out.println("neg");
				break;
			case FALSE:
				writeKeyword(new Keyword[] { Keyword.FALSE });
				System.out.println("push constant 0");
				break;
			default:
				break;
			}
		default:
			String identifier = tokenizer.identifier();
			tokenizer.advance();
			if (tokenizer.tokenType() == TokenType.SYMBOL) {
				sym = tokenizer.symbol();
				if (sym == '[') {
					// handle array access
					writeToOutput("<identifier>" + identifier + "</identifier>");
					writeToOutput("<symbol>" + sym + "</symbol>");
					compileExpression();
					writeSymbol(new char[] { ']' });
				} else if (sym == '(' || sym == '.') {
					// handle function call
					writeToOutput("<subroutineCall>");
					indent += "    ";
					writeToOutput("<identifier>" + identifier + "</identifier>");
					writeToOutput("<symbol>" + sym + "</symbol>");
					tokenizer.advance();
					if (sym == '(') {
						compileExpressionList();
						writeSymbol(new char[] { ')' });
					} else {
						writeIdentifier();
						writeSymbol(new char[] { '(' });
						compileExpressionList();
						writeSymbol(new char[] { ')' });
					}
					indent = indent.substring(4);
					writeToOutput("</subroutineCall>");
				} else /* if (sym == ',' || sym == '<' || sym == '>') */ {
					// handle variable
					writeToOutput("<identifier>" + identifier + "</identifier>");
					if (indexOfVar(scopeArgVars, identifier) != -1) {
						System.out.println("push argument " + indexOfVar(scopeArgVars, identifier));
					} else if (indexOfVar(scopeVars, identifier) != -1) {
						System.out.println("push local " + indexOfVar(scopeVars, identifier));
					} else if (indexOfVar(instanceVars, identifier) != -1) {
						System.out.println("push this " + indexOfVar(instanceVars, identifier));
					} else if (indexOfVar(classVars, identifier) != -1) {
						System.out.println("push static " + indexOfVar(classVars, identifier));
					}
				}
			}
		}
		indent = indent.substring(4);
		writeToOutput("</term>");
	}

	/**
	 * Compiles a (possibly empty) comma-separated list of expressions.
	 */
	public void compileExpressionList() throws CompilationException {
		writeToOutput("<expressionList>");
		indent += "    ";
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
			compileExpression();
			while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
				writeSymbol(new char[] { ',' });
				compileExpression();
			}
		}
		indent = indent.substring(4);
		writeToOutput("</expressionList>");
	}

	public static void main(String[] args) {
		String program = "class Main {\n" 
				+ "function void main() {\n"
				+ "do Output.printInt(1 + (2 * 3));\n"
				+ "return;\n"
				+ "}\n"
				+ "}";
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			CompilationEngine engine = new CompilationEngine(new StringInputStream(program), output);
			engine.compileClass();
		} catch (IOException e) {
		} catch (CompilationException e) {
			e.printStackTrace();
		}

	}
}