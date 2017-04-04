package il.ac.idc.lang.compiler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import il.ac.idc.lang.compiler.Tokenizer.Keyword;
import il.ac.idc.lang.compiler.Tokenizer.TokenType;
import il.ac.idc.lang.hvm.VMTranslator;

public class CompilationEngine {

	private Tokenizer tokenizer;
	
	/**
	 * Create a new CompilatopnEngine with the given input and output. he next
	 * method to be called must be compileClass().
	 * 
	 * @param in
	 * @param out
	 */
	public CompilationEngine(InputStream in) throws IOException {
		tokenizer = new Tokenizer(in);
	}

	private void writeIdentifier() throws CompilationException {
		if (!(tokenizer.tokenType() == TokenType.IDENTIFIER)) {
			throw new CompilationException(
					"Unexpected token of type " + tokenizer.tokenType() + ", expected \"identifier\"");
		}
		processNext();
	}

	private void writeKeyword(Keyword expected) throws CompilationException {
		if (tokenizer.tokenType() != TokenType.KEYWORD) {
			throw new UnexpectedTokenTypeException("Expected a keyword");
		}
		if (tokenizer.keyword() != expected) {
			throw new SyntaxErrorException("Expected " + expected);
		}
		processNext();
	}
	
	private void writeKeyword(Keyword[] expected) throws CompilationException {
		for (Keyword k : expected) {
			try {
				writeKeyword(k);
				return;
			} catch (SyntaxErrorException e) {
				
			}
		}
		String[] keys = new String[expected.length];
		for (int i = 0; i < expected.length; i++) {
			keys[i] = expected.toString();
		}
		throw new SyntaxErrorException("Invalid keyword found, should be one of: " + String.join(", ", keys));
	}

	private void writeSymbol(char expected) throws CompilationException {
		if (tokenizer.tokenType() != TokenType.SYMBOL) {
			throw new UnexpectedTokenTypeException("Expected a symbol");
		}
		if (tokenizer.symbol() != expected) {
			throw new SyntaxErrorException("Expected " + expected);
		}
		processNext();
	}
	
	private void writeSymbol(char[] expected) throws CompilationException {
		for (char sym : expected) {
			try {
				writeSymbol(sym);
				return;
			} catch (SyntaxErrorException e) {
				
			}
		}
		String[] sym = new String[expected.length];
		for (int i = 0; i < expected.length; i++) {
			sym[i] = "" + expected[i];
		}
		throw new SyntaxErrorException("Invalid symbol, should be one of: " + String.join(", ", sym));
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
	public JackClass compileClass() throws CompilationException {
		processNext();
		int lineNumber = tokenizer.getCurrentLineNumber();
		JackClass klass = null;
		if (tokenizer.tokenType() == TokenType.KEYWORD) {
			writeKeyword(Keyword.CLASS); // class
			klass = new JackClass(lineNumber, tokenizer.identifier());
			writeIdentifier(); // className
			writeSymbol('{');
			if (tokenizer.tokenType() == TokenType.KEYWORD) {
				while (tokenizer.keyword() == Keyword.STATIC || tokenizer.keyword() == Keyword.FIELD) {
					klass.addClassVariables(compileClassVarDecl());
				}
				while (tokenizer.keyword() == Keyword.FUNCTION || tokenizer.keyword() == Keyword.CONSTRUCTOR
						|| tokenizer.keyword() == Keyword.METHOD) {
					klass.addSubroutine(compileSubroutine());
				}
			}
			if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != '}') {
				throw new SyntaxErrorException("Expected \"}\"");
			}
		}
		if (tokenizer.hasMoreTokens()) {
			throw new CompilationException("Unexpected trailing text at end of class found");
		}
		return klass;
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
	private List<JackClassVariableDecl> compileClassVarDecl() throws CompilationException {
		JackVariableModifier modifier = new JackVariableModifier(tokenizer.keyword().toString());
		int lineNumber = tokenizer.getCurrentLineNumber();
		List<JackClassVariableDecl> list = new ArrayList<>();
		writeKeyword(new Keyword[] { Keyword.STATIC, Keyword.FIELD }); // static/field
		JackVariableType type = new JackVariableType((tokenizer.tokenType() == TokenType.KEYWORD ? tokenizer.keyword() : tokenizer.identifier()).toString());
		JackVariableName var = new JackVariableName(tokenizer.identifier());
		JackClassVariableDecl decl = new JackClassVariableDecl(lineNumber, modifier, type, var);
		list.add(decl);
		writeIdentifier();
		while (tokenizer.symbol() != ';') {
			writeSymbol(',');
			var = new JackVariableName(tokenizer.identifier());
			decl = new JackClassVariableDecl(lineNumber, modifier, type, var);
			writeIdentifier();
			list.add(decl);
		}
		writeSymbol(';');
		return list;
	}

	/**
	 * Compiles a complete method, function or constructor.
	 */
	private AbstractJackSubroutine compileSubroutine() throws CompilationException {
		AbstractJackSubroutine subroutine = null;
		Keyword type = tokenizer.keyword();
		int lineNumber = tokenizer.getCurrentLineNumber();
		writeKeyword(new Keyword[] { Keyword.CONSTRUCTOR, Keyword.FUNCTION, Keyword.METHOD });
		try {
			compileType();
		} catch (CompilationException e) {
			writeKeyword(Keyword.VOID);
		}
		String subroutineName = tokenizer.identifier();
		switch(type) {
		case CONSTRUCTOR:
			subroutine = new JackConstructor(lineNumber, subroutineName);
			break;
		case FUNCTION:
			subroutine = new JackFunction(lineNumber, subroutineName);
			break;
		case METHOD:
			subroutine = new JackMethod(lineNumber, subroutineName);
			break;
		default:
			break;
		}
		writeIdentifier();
		writeSymbol('(');
		subroutine.setArguments(compileParametersList());
		writeSymbol(')');
		writeSymbol('{');

		// write variables
		while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
			subroutine.addLocals(compileVarDecl());
		}
		// write statements
		List<AbstractJackStatement> statements = compileStatements();
		subroutine.setStatements(statements);
		writeSymbol('}');
		return subroutine;
	}

	/**
	 * Compiles a (possibly empty) parameter list, not including the enclosing
	 * "()".
	 */
	private List<JackVariableDecl> compileParametersList() throws CompilationException {
		List<JackVariableDecl> arguments = new ArrayList<>();
		int lineNumber = tokenizer.getCurrentLineNumber();
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
			JackVariableType type = new JackVariableType((tokenizer.tokenType() == TokenType.KEYWORD ? tokenizer.keyword() : tokenizer.identifier()).toString());
			compileType();
			JackVariableName name = new JackVariableName(tokenizer.identifier());
			arguments.add(new JackVariableDecl(lineNumber, type, name));
			writeIdentifier();
			try {
				writeSymbol(',');
			} catch (CompilationException e) {
			}
		}
		return arguments;
	}

	/**
	 * Compiles and var declaration.
	 */
	private List<JackVariableDecl> compileVarDecl() throws CompilationException {
		writeKeyword(Keyword.VAR);
		int lineNumber = tokenizer.getCurrentLineNumber();
		List<JackVariableDecl> list = new ArrayList<>();
		JackVariableType type = new JackVariableType((tokenizer.tokenType() == TokenType.KEYWORD ? tokenizer.keyword() : tokenizer.identifier()).toString());
		compileType(); // type
		JackVariableName name = new JackVariableName(tokenizer.identifier());
		JackVariableDecl decl = new JackVariableDecl(lineNumber, type, name);
		writeIdentifier(); // varName
		list.add(decl);
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
			writeSymbol(',');
			name = new JackVariableName(tokenizer.identifier());
			decl = new JackVariableDecl(lineNumber, type, name);
			list.add(decl);
			writeIdentifier();
		}
		writeSymbol(';');
		return list;
	}

	/**
	 * Compiles and sequence of statements, not including the enclosing "{}".
	 */
	private List<AbstractJackStatement> compileStatements() throws CompilationException {
		List<AbstractJackStatement> statements = new ArrayList<>();
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
		return statements;
	}

	/**
	 * Compiles a do statement.
	 */
	private JackDoStatement compileDo() throws CompilationException {
		writeKeyword(Keyword.DO);
		int line = tokenizer.getCurrentLineNumber();
		JackSubroutineCallTerm doStatement = new JackSubroutineCallTerm(line);
		String functionName = tokenizer.identifier();
		doStatement.subroutineName = functionName;
		writeIdentifier();
		if (tokenizer.tokenType() == TokenType.SYMBOL) {
			writeSymbol('.');
			doStatement.accessor = functionName;
			doStatement.subroutineName = tokenizer.identifier();
			writeIdentifier();
		}
		writeSymbol('(');
		doStatement.setParameters(compileExpressionList());
		writeSymbol(')');
		writeSymbol(';');
		JackDoStatement statement = new JackDoStatement(line, doStatement);
		return statement;
	}

	/**
	 * Compiles a let statement.
	 */
	private JackLetStatement compileLet() throws CompilationException {
		int line = tokenizer.getCurrentLineNumber();
		JackLetStatement let = new JackLetStatement(line);
		writeKeyword(Keyword.LET);
		String assignTo = tokenizer.identifier();
		writeIdentifier();
		if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
			JackArrayAccessTerm arrayAccess = new JackArrayAccessTerm(line);
			arrayAccess.varname = assignTo;
			writeSymbol('[');
			arrayAccess.setExpression(compileExpression());
			writeSymbol(']');
			let.setArrayAccess(arrayAccess);
		} else {
			let.assignee = assignTo;
		}
		writeSymbol('=');
		let.setExpression(compileExpression());
		writeSymbol(';');
		return let;
	}

	/**
	 * Compiles a while statement.
	 */
	private JackWhileStatement compileWhile() throws CompilationException {
		JackWhileStatement whileStatement = new JackWhileStatement(tokenizer.getCurrentLineNumber());
		writeKeyword(Keyword.WHILE);
		writeSymbol('(');
		whileStatement.setCondition(compileExpression());
		writeSymbol(')');
		writeSymbol('{');
		whileStatement.setStatements(compileStatements());
		writeSymbol('}');
		return whileStatement;
	}

	/**
	 * Compiles a return statement.
	 */
	private JackReturnStatement compileReturn() throws CompilationException {
		JackReturnStatement statement = new JackReturnStatement(tokenizer.getCurrentLineNumber());
		writeKeyword(Keyword.RETURN);
		if (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
			statement.setExpression(compileExpression());
		}
		writeSymbol(';');
		return statement;
	}

	/**
	 * Compiles an if statement, possibly with a trailing else clause.
	 */
	private JackIfStatement compileIf() throws CompilationException {
		JackIfStatement ifStatement = new JackIfStatement(tokenizer.getCurrentLineNumber());
		writeKeyword(Keyword.IF);
		writeSymbol('(');
		ifStatement.setCondition(compileExpression());
		writeSymbol(')');
		writeSymbol('{');
		ifStatement.setTrueClause(compileStatements());
		writeSymbol('}');
		try {
			writeKeyword(Keyword.ELSE);
			writeSymbol('{');
			ifStatement.setFalseClause(compileStatements());
			writeSymbol('}');
		} catch (CompilationException e) {
			processNext();
		}
		return ifStatement;
	}

	/**
	 * Compiles an expression.
	 */
	private JackExpression compileExpression() throws CompilationException {
		JackExpression expression = new JackExpression(tokenizer.getCurrentLineNumber());
		expression.setLeft(compileTerm());
		if (tokenizer.tokenType() == TokenType.SYMBOL && (tokenizer.symbol() != ';' && tokenizer.symbol() != ',')
				&& tokenizer.symbol() != ')') {
			char sym = tokenizer.symbol();
			expression.op = sym;
			writeSymbol(new char[] { '+', '-', '*', '/', '&', '|', '<', '>', '=' });
			expression.setRight(compileTerm());
		}
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
	private AbstractJackTerm compileTerm() throws CompilationException {
		AbstractJackTerm term = null;
		int line = tokenizer.getCurrentLineNumber();
		switch (tokenizer.tokenType()) {
		case INT_CONST:
			term = new JackIntegerConstant(line, tokenizer.intVal());
			processNext();
			break;
		case STRING_CONST:
			term = new JackStringConstant(line, tokenizer.stringVal());
			processNext();
			break;
		case SYMBOL:
			char sym = tokenizer.symbol();
			switch (sym) {
			case '-':
			case '~':
				writeSymbol(new char[] {'-', '~'});
				term = new JackUnaryTerm(line, sym, compileTerm());
				break;
			case '(':
				writeSymbol('(');
				term = compileExpression();
				writeSymbol(')');
				break;
			}
			break;
		case KEYWORD:
			term = new JackKeywordTerm(line, tokenizer.keyword().toString());
			writeKeyword(new Keyword[] {Keyword.THIS, Keyword.NULL, Keyword.FALSE, Keyword.TRUE});
			break;
		default:
			String identifier = tokenizer.identifier();
			processNext();
			if (tokenizer.tokenType() == TokenType.SYMBOL) {
				sym = tokenizer.symbol();
				if (sym == '[') {
					// handle array access
					term = new JackArrayAccessTerm(line);
					((JackArrayAccessTerm)term).varname = identifier;
					((JackArrayAccessTerm)term).setExpression(compileExpression());
					writeSymbol(']');
					break;
				} else if (sym == '(' || sym == '.') {
					// handle function call
					term = new JackSubroutineCallTerm(line);
					((JackSubroutineCallTerm)term).subroutineName = identifier;
					tokenizer.advance();
					if (sym == '(') {
						((JackSubroutineCallTerm)term).setParameters(compileExpressionList());
						writeSymbol(')');
					} else {
						((JackSubroutineCallTerm)term).accessor = ((JackSubroutineCallTerm)term).subroutineName;
						((JackSubroutineCallTerm)term).subroutineName = tokenizer.identifier();
						writeIdentifier();
						writeSymbol('(');
						((JackSubroutineCallTerm)term).setParameters(compileExpressionList());
						writeSymbol(')');
					}
					break;
				} else /* if (sym == ',' || sym == '<' || sym == '>') */ {
					// handle variable
					term = new JackVariableTerm(line);
					((JackVariableTerm)term).varname = identifier;
				}
			}
		}
		return term;
	}

	/**
	 * Compiles a (possibly empty) comma-separated list of expressions.
	 */
	private List<JackExpression> compileExpressionList() throws CompilationException {
		List<JackExpression> expressions = new ArrayList<>();
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
			expressions.add(compileExpression());
			while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
				writeSymbol(',');
				expressions.add(compileExpression());
			}
		}
		return expressions;
	}

	public static JackClass compile(InputStream in) throws CompilationException {
		try {
			CompilationEngine engine = new CompilationEngine(in);
			return engine.compileClass();
		} catch (FileNotFoundException e) {
			System.err.println("Cannot find file: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Cannot write to file: " + e.getMessage());
		}
		return null;
	}
	
	private static void writeToOutputFile(String outputFilename, String content) throws IOException {
		File outputFile = new File(outputFilename);
		outputFile.createNewFile();
		FileOutputStream outputStream = new FileOutputStream(outputFile);
		byte[] b = content.getBytes();
		try {
			outputStream.write(b, 0, b.length);
			outputStream.flush();
		} catch (IOException e) {
		} finally {
			outputStream.close();
		}
	}
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Error parsing parameters");
			System.exit(1);
		}
		String fileOrDirectory = args[args.length - 1];
		boolean debugInfo = false;
		String format = "bytecode";
		if (args.length > 1) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("--debugInfo")) {
					debugInfo = true;
				}
				if (args[i].equals("--format")) {
					format = args[i+1]; 
				}
			}
		}
		
		File f = new File(fileOrDirectory);
		try {
			if (f.isDirectory()) {
				for (File file : f.listFiles()) {
					if (file.getName().endsWith(".jack")) {
						try {
							FileInputStream inputStream = new FileInputStream(file);
							JackClass klass = compile(inputStream);
							String code = klass.writeVMCode();
							String outputFilename = file.getPath().split("\\.")[0] +".vm";
							writeToOutputFile(outputFilename, code);
						} catch (FileNotFoundException e) {
							System.err.println("Cannot find file: " + e.getMessage());
						} catch (IOException e) {
							System.err.println("Cannot write to file: " + e.getMessage());
						}
					}
				}
			} else {
				try {
					if (f.getName().endsWith("jack")) {
						FileInputStream inputStream = new FileInputStream(f);
						JackClass klass = compile(inputStream);
						String code = klass.writeVMCode();
						String outputFilename = f.getPath().split("\\.")[0] +".vm";
						switch (format) {
						case "bytecode":
							writeToOutputFile(outputFilename, code);
							if (debugInfo) {
								// handle this
							}
							break;
						case "binary":
							VMTranslator translator = new VMTranslator(f.getParent() + File.separator + "a.hack");
							translator.translateFile(f, new ByteArrayInputStream(code.getBytes()));
							break;
						}
					}
				} catch (FileNotFoundException e) {
					System.err.println("Cannot find file: " + e.getMessage());
				} catch (IOException e) {
					System.err.println("Cannot write to file: " + e.getMessage());
				}
			}
		} catch (CompilationException e) {
			e.printStackTrace();
		}
	}
}