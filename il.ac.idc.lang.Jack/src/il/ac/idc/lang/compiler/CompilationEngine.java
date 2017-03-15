package il.ac.idc.lang.compiler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
//		byte[] b = (indent + str + "\n").getBytes();
//		try {
//			output.write(b, 0, b.length);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	private static void writeVMCode(String str, OutputStream output) {
		byte[] b = str.getBytes();
		try {
			output.write(b, 0, b.length);
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
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
	public String compileClass() throws CompilationException {
		processNext();
		if (tokenizer.tokenType() == TokenType.KEYWORD) {
			writeToOutput("<class>");
			indent += "    ";
			writeKeyword(new Keyword[] { Keyword.CLASS }); // class
			className = tokenizer.identifier();
			JackClassArtifact klass = new JackClassArtifact(className);
			writeIdentifier(); // className
			writeSymbol(new char[] { '{' });
			if (tokenizer.tokenType() == TokenType.KEYWORD) {
				while (tokenizer.keyword() == Keyword.STATIC || tokenizer.keyword() == Keyword.FIELD) {
					compileClassVarDecl(klass);
				}
				while (tokenizer.keyword() == Keyword.FUNCTION || tokenizer.keyword() == Keyword.CONSTRUCTOR
						|| tokenizer.keyword() == Keyword.METHOD) {
					klass.subroutines.add(compileSubroutine());
				}
			}
			if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
				writeToOutput("<symbol>}</symbol>");
			}
			return klass.writeVMCode();
		}
		if (!tokenizer.hasMoreTokens()) {
			indent = indent.substring(4);
			writeToOutput("</class>");
		} else {
			throw new CompilationException("Unexpected trailing text at end of class found");
		}
		return null;
	}

	private void compileType() throws CompilationException {
		try {
			writeKeyword(new Keyword[] { Keyword.INT, Keyword.CHAR, Keyword.BOOLEAN });
		} catch (CompilationException e) {
			writeIdentifier();
		}
	}

	/**
	 * Compiles a static declaration and or a field declaration.
	 */
	private void compileClassVarDecl(JackClassArtifact klass) throws CompilationException {
		writeToOutput("<classVarDec>");
		indent += "    ";
		Keyword modifier = tokenizer.keyword();
		writeKeyword(new Keyword[] { Keyword.STATIC, Keyword.FIELD }); // static/field
		Object type = tokenizer.tokenType() == TokenType.KEYWORD ? tokenizer.keyword() : tokenizer.identifier();
		String varName = tokenizer.identifier();
		JackVariableArtifact var = new JackVariableArtifact(varName, type.toString());
		if (modifier == Keyword.STATIC) {
			klass.classVariables.add(var);
		} else {
			klass.instanceVariables.add(var);
		}
		writeIdentifier();
		while (tokenizer.symbol() != ';') {
			writeSymbol(new char[] { ',' });
			varName = tokenizer.identifier();
			writeIdentifier();
			if (modifier == Keyword.STATIC) {
				klass.classVariables.add(new JackVariableArtifact(varName, type.toString()));
			} else {
				klass.instanceVariables.add(new JackVariableArtifact(varName, type.toString()));
			}
		}
		writeSymbol(new char[] { ';' });
		indent = indent.substring(4);
		writeToOutput("</classVarDec>");
	}

	/**
	 * Compiles a complete method, function or constructor.
	 */
	private JackSubroutineArtifact compileSubroutine() throws CompilationException {
		writeToOutput("<subroutine>");
		indent += "    ";
		JackSubroutineArtifact subroutine = null;
		Keyword type = tokenizer.keyword();
		writeKeyword(new Keyword[] { Keyword.CONSTRUCTOR, Keyword.FUNCTION, Keyword.METHOD });
		try {
			compileType();
		} catch (CompilationException e) {
			writeKeyword(new Keyword[] { Keyword.VOID });
		}
		currentSubroutine = tokenizer.identifier();
		switch(type) {
		case CONSTRUCTOR:
			subroutine = new JackConstructorArtifact(currentSubroutine);
			break;
		case FUNCTION:
			subroutine = new JackFunctionArtifact(currentSubroutine);
			break;
		case METHOD:
			subroutine = new JackMethodArtifact(currentSubroutine);
			break;
		default:
			break;
		}
		writeIdentifier();
		System.out.println("(" + className + ":" + currentSubroutine + ")");
		writeSymbol(new char[] { '(' });
		compileParametersList(subroutine);
		writeSymbol(new char[] { ')' });
		writeSymbol(new char[] { '{' });

		// write variables
		while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
			List<JackVariableArtifact> vars = compileVarDecl();
			subroutine.locals.addAll(vars);
		}
		// write statements
		List<JackStatementArtifact> statements = compileStatements();
		subroutine.statements = statements;
		scopeArgVars.clear();
		scopeVars.clear();
		writeSymbol(new char[] { '}' });
		indent = indent.substring(4);
		writeToOutput("</subroutine>");
		return subroutine;
	}

	/**
	 * Compiles a (possibly empty) parameter list, not including the enclosing
	 * "()".
	 */
	private void compileParametersList(JackSubroutineArtifact subroutine) throws CompilationException {
		writeToOutput("<parameterList>");
		indent += "    ";
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
			Object type = tokenizer.tokenType() == TokenType.KEYWORD ? tokenizer.keyword() : tokenizer.identifier();
			compileType();
			String name = tokenizer.identifier();
			subroutine.arguments.add(new JackVariableArtifact(name, type.toString()));
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
	private List<JackVariableArtifact> compileVarDecl() throws CompilationException {
		writeToOutput("<varDec>");
		indent += "    ";
		List<JackVariableArtifact> list = new ArrayList<>();
		writeKeyword(new Keyword[] { Keyword.VAR });
		Object type = tokenizer.tokenType() == TokenType.KEYWORD ? tokenizer.keyword() : tokenizer.identifier();
		compileType(); // type
		String name = tokenizer.identifier();
		list.add(new JackVariableArtifact(name, type.toString()));
		writeIdentifier(); // varName
		scopeVars.add(new VariableEntry(name, type));
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
			writeSymbol(new char[] { ',' });
			name = tokenizer.identifier();
			list.add(new JackVariableArtifact(name, type.toString()));
			writeIdentifier();
			scopeVars.add(new VariableEntry(name, type));
		}
		writeSymbol(new char[] { ';' });
		indent = indent.substring(4);
		writeToOutput("</varDec>");
		return list;
	}

	/**
	 * Compiles and sequence of statements, not including the enclosing "{}".
	 */
	private List<JackStatementArtifact> compileStatements() throws CompilationException {
		writeToOutput("<statements>");
		indent += "    ";
		List<JackStatementArtifact> statements = new ArrayList<>();
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}')) {
			switch (tokenizer.keyword()) {
			case DO:
				statements.add(compileDo());
				break;
			case LET:
				statements.add(compileLet());
				break;
			case IF:
				statements.add(compileIf());
				break;
			case WHILE:
				statements.add(compileWhile());
				break;
			case RETURN:
				statements.add(compileReturn());
				break;
			default:
				break;
			}
		}
		indent = indent.substring(4);
		writeToOutput("</statements>");
		return statements;
	}

	/**
	 * Compiles a do statement.
	 */
	private JackDoStatementArtifact compileDo() throws CompilationException {
		writeToOutput("<doStatement>");
		indent += "    ";
		writeKeyword(new Keyword[] { Keyword.DO });
		writeToOutput("<subroutineCall>");
		indent += "    ";
		JackSubroutineCallTerm doStatement = new JackSubroutineCallTerm();
		String className = null;
		String functionName = tokenizer.identifier();
		doStatement.subroutineName = functionName;
		writeIdentifier();
		TokenType type = tokenizer.tokenType();
		if (type == TokenType.SYMBOL) {
			writeSymbol(new char[] { '.' });
			className = functionName;
			doStatement.accessor = functionName;
			doStatement.subroutineName = tokenizer.identifier();
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
		doStatement.parameters = compileExpressionList();
		writeSymbol(new char[] { ')' });
		writeToOutput("</subroutineCall>");
		indent = indent.substring(4);
		writeSymbol(new char[] { ';' });
		System.out.println("call " + className + ":" + functionName);
		indent = indent.substring(4);
		writeToOutput("</doStatement>");
		JackDoStatementArtifact statement = new JackDoStatementArtifact();
		statement.subroutine = doStatement;
		return statement;
	}

	/**
	 * Compiles a let statement.
	 */
	private JackLetStatementArtifact compileLet() throws CompilationException {
		writeToOutput("<letStatement>");
		indent += "    ";
		JackLetStatementArtifact let = new JackLetStatementArtifact();
		writeKeyword(new Keyword[] { Keyword.LET });
		String assignTo = tokenizer.identifier();
		writeIdentifier();
		if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
			JackArrayAccessTerm arrayAccess = new JackArrayAccessTerm();
			arrayAccess.varname = assignTo;
			writeSymbol(new char[] { '[' });
			arrayAccess.expression = compileExpression();
			writeSymbol(new char[] { ']' });
			let.arrayAccess = arrayAccess;
		} else {
			let.assignee = assignTo;
		}
		writeSymbol(new char[] { '=' });
		let.expression = compileExpression();
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
		return let;
	}

	/**
	 * Compiles a while statement.
	 */
	private JackWhileStatementArtifact compileWhile() throws CompilationException {
		writeToOutput("<whileStatement>");
		indent += "    ";
		JackWhileStatementArtifact whileStatement = new JackWhileStatementArtifact();
		writeKeyword(new Keyword[] { Keyword.WHILE });
		writeSymbol(new char[] { '(' });
		whileStatement.condition = compileExpression();
		writeSymbol(new char[] { ')' });
		writeSymbol(new char[] { '{' });
		whileStatement.statements = compileStatements();
		writeSymbol(new char[] { '}' });
		indent = indent.substring(4);
		writeToOutput("</whileStatement>");
		return whileStatement;
	}

	/**
	 * Compiles a return statement.
	 */
	private JackReturnStatementArtifact compileReturn() throws CompilationException {
		writeToOutput("<returnStatement>");
		indent += "    ";
		JackReturnStatementArtifact statement = new JackReturnStatementArtifact();
		writeKeyword(new Keyword[] { Keyword.RETURN });
		if (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
			statement.expression = compileExpression();
		} else {
			System.out.println("push constant 0");
		}
		writeSymbol(new char[] { ';' });
		indent = indent.substring(4);
		writeToOutput("</returnStatement>");
		return statement;
	}

	/**
	 * Compiles an if statement, possibly with a trailing else clause.
	 */
	private JackIfStatementArtifact compileIf() throws CompilationException {
		writeToOutput("<ifStatement>");
		indent += "    ";
		JackIfStatementArtifact ifStatement = new JackIfStatementArtifact();
		writeKeyword(new Keyword[] { Keyword.IF });
		writeSymbol(new char[] { '(' });
		ifStatement.condition = compileExpression();
		writeSymbol(new char[] { ')' });
		writeSymbol(new char[] { '{' });
		ifStatement.trueClause = compileStatements();
		writeSymbol(new char[] { '}' });
		try {
			writeKeyword(new Keyword[] { Keyword.ELSE });
			writeSymbol(new char[] { '{' });
			ifStatement.falseClause = compileStatements();
			writeSymbol(new char[] { '}' });
		} catch (CompilationException e) {
			tokenizer.advance();
		}
		indent = indent.substring(4);
		writeToOutput("</ifStatement>");
		return ifStatement;
	}

	/**
	 * Compiles an expression.
	 */
	private JackExpressionArtifact compileExpression() throws CompilationException {
		writeToOutput("<expression>");
		indent += "    ";
		JackExpressionArtifact expression = new JackExpressionArtifact();
		expression.left = compileTerm();
		if (tokenizer.tokenType() == TokenType.SYMBOL && (tokenizer.symbol() != ';' && tokenizer.symbol() != ',')
				&& tokenizer.symbol() != ')') {
			char sym = tokenizer.symbol();
			expression.op = sym;
			writeSymbol(new char[] { '+', '-', '*', '/', '&', '|', '<', '>', '=' });
			expression.right = compileTerm();
//			switch (sym) {
//			case '+':
//				System.out.println("add");
//				break;
//			case '-':
//				System.out.println("sub");
//				break;
//			case '*':
//				// TODO
//				System.out.println("mult");
//				break;
//			case '/':
//				// TODO
//				System.out.println("div");
//				break;
//			case '&':
//				System.out.println("and");
//				break;
//			case '|':
//				System.out.println("or");
//				break;
//			case '<':
//				System.out.println("lt");
//				break;
//			case '>':
//				System.out.println("gt");
//				break;
//			case '=':
//				System.out.println("eq");
//			}
		}
		indent = indent.substring(4);
		writeToOutput("</expression>");
		return expression;
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
	private JackTermArtifact compileTerm() throws CompilationException {
		writeToOutput("<term>");
		indent += "    ";
		switch (tokenizer.tokenType()) {
		case INT_CONST:
			writeToOutput("<intConst>" + tokenizer.intVal() + "</intConst>");
			JackIntegerConstant integer = new JackIntegerConstant();
			integer.constant = tokenizer.intVal();
			System.out.println("push constant " + tokenizer.intVal());
			tokenizer.advance();
			return integer;
		case STRING_CONST:
			JackStringConstantArifact string = new JackStringConstantArifact();
			string.constant = tokenizer.stringVal();
			writeToOutput("<stringConst>" + tokenizer.stringVal() + "</stringConst>");
			tokenizer.advance();
			return string;
		case SYMBOL:
			char sym = tokenizer.symbol();
			JackTermArtifact unaryTerm = null;
			switch (sym) {
			case '-':
				writeSymbol(new char[] { '-' });
				unaryTerm = new JackUnaryTermArifact();
				((JackUnaryTermArifact)unaryTerm).op = sym;
				((JackUnaryTermArifact)unaryTerm).term = compileTerm();
				compileTerm();
				System.out.println("neg");
				return unaryTerm;
			case '~':
				writeSymbol(new char[] { '~' });
				unaryTerm = new JackUnaryTermArifact();
				((JackUnaryTermArifact)unaryTerm).op = sym;
				((JackUnaryTermArifact)unaryTerm).term = compileTerm();
				compileTerm();
				System.out.println("not");
				return unaryTerm;
			case '(':
				writeSymbol(new char[] { '(' });
				JackTermArtifact term = compileExpression();
				writeSymbol(new char[] { ')' });
				return term;
			}
			return null;
		case KEYWORD:
			JackKeywordTermArtifact k = new JackKeywordTermArtifact();
			k.keyword = tokenizer.keyword().toString();
			switch (tokenizer.keyword()) {
			case THIS:
				writeKeyword(new Keyword[] { Keyword.THIS });
				System.out.println("push pointer 0");
				break;
			case NULL:
				writeKeyword(new Keyword[] { Keyword.NULL });
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
			return k;
		default:
			String identifier = tokenizer.identifier();
			tokenizer.advance();
			if (tokenizer.tokenType() == TokenType.SYMBOL) {
				sym = tokenizer.symbol();
				if (sym == '[') {
					JackArrayAccessTerm arrayAccess = new JackArrayAccessTerm();
					arrayAccess.varname = identifier;
					// handle array access
					writeToOutput("<identifier>" + identifier + "</identifier>");
					writeToOutput("<symbol>" + sym + "</symbol>");
					arrayAccess.expression = compileExpression();
					writeSymbol(new char[] { ']' });
					return arrayAccess;
				} else if (sym == '(' || sym == '.') {
					// handle function call
					writeToOutput("<subroutineCall>");
					indent += "    ";
					JackSubroutineCallTerm subroutineCall = new JackSubroutineCallTerm();
					subroutineCall.subroutineName = identifier;
					writeToOutput("<identifier>" + identifier + "</identifier>");
					writeToOutput("<symbol>" + sym + "</symbol>");
					tokenizer.advance();
					if (sym == '(') {
						subroutineCall.parameters = compileExpressionList();
						writeSymbol(new char[] { ')' });
					} else {
						subroutineCall.accessor = subroutineCall.subroutineName;
						subroutineCall.subroutineName = tokenizer.identifier();
						writeIdentifier();
						writeSymbol(new char[] { '(' });
						subroutineCall.parameters = compileExpressionList();
						writeSymbol(new char[] { ')' });
					}
					indent = indent.substring(4);
					writeToOutput("</subroutineCall>");
					return subroutineCall;
				} else /* if (sym == ',' || sym == '<' || sym == '>') */ {
					// handle variable
					JackVariableTerm term = new JackVariableTerm();
					term.varname = identifier;
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
					return term;
				}
			} else {
				return null;
			}
		}
//		indent = indent.substring(4);
//		writeToOutput("</term>");
	}

	/**
	 * Compiles a (possibly empty) comma-separated list of expressions.
	 */
	private List<JackExpressionArtifact> compileExpressionList() throws CompilationException {
		writeToOutput("<expressionList>");
		indent += "    ";
		List<JackExpressionArtifact> expressions = new ArrayList<>();
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
			expressions.add(compileExpression());
			while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
				writeSymbol(new char[] { ',' });
				expressions.add(compileExpression());
			}
		}
		indent = indent.substring(4);
		writeToOutput("</expressionList>");
		return expressions;
	}

	public static String compile(InputStream in, OutputStream out) throws CompilationException {
		try {
			CompilationEngine engine = new CompilationEngine(in, out);
			return engine.compileClass();
		} catch (FileNotFoundException e) {
			System.err.println("Cannot find file: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Cannot write to file: " + e.getMessage());
		}
		return null;
	}
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Error parsing parameters");
			System.exit(1);
		}
		String fileOrDirectory = args[args.length - 1];
		File f = new File(fileOrDirectory);
		try {
			if (f.isDirectory()) {
				for (File file : f.listFiles()) {
					if (!file.getName().endsWith(".jack")) {
						continue;
					}
					String outputFilename = file.getPath().split("\\.")[0] +".vm";
					File outputFile = new File(outputFilename);
					try {
						outputFile.createNewFile();
						FileOutputStream outputStream = new FileOutputStream(outputFile);
						FileInputStream inputStream = new FileInputStream(file);
						String code = compile(inputStream, outputStream);
						writeVMCode(code, outputStream);
					} catch (FileNotFoundException e) {
						System.err.println("Cannot find file: " + e.getMessage());
					} catch (IOException e) {
						System.err.println("Cannot write to file: " + e.getMessage());
					}
				}
			} else {
				try {
					String outputFilename = f.getPath().split("\\.")[0] +".vm";
					File outputFile = new File(outputFilename);
					outputFile.createNewFile();
					FileOutputStream outputStream = new FileOutputStream(outputFile);
					FileInputStream inputStream = new FileInputStream(f);
					String code = compile(inputStream, outputStream);
					writeVMCode(code, outputStream);
				} catch (FileNotFoundException e) {
					System.err.println("Cannot find file: " + e.getMessage());
				} catch (IOException e) {
					System.err.println("Cannot write to file: " + e.getMessage());
				}
			}
		} catch (CompilationException e) {
			e.printStackTrace();
		}
//		String program = "class Main {\n" 
//				+ "function void main() {\n"
//				+ "do Output.printInt(1 + (2 * 3));\n"
//				+ "return;\n"
//				+ "}\n"
//				+ "}";
//		ByteArrayOutputStream output = new ByteArrayOutputStream();
//		try {
//			CompilationEngine engine = new CompilationEngine(new ByteArrayInputStream(program.getBytes()), output);
//			engine.compileClass();
//		} catch (IOException e) {
//		} catch (CompilationException e) {
//		}

	}
}