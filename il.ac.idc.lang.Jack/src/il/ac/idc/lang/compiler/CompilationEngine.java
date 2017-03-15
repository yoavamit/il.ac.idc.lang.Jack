package il.ac.idc.lang.compiler;

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

	private Tokenizer tokenizer;
	
	/**
	 * Create a new CompilatopnEngine with the given input and output. he next
	 * method to be called must be compileClass().
	 * 
	 * @param in
	 * @param out
	 */
	public CompilationEngine(InputStream in, OutputStream out) throws IOException {
		tokenizer = new Tokenizer(in);
	}

	private static void writeVMCode(String str, OutputStream output) {
		byte[] b = str.getBytes();
		try {
			output.write(b, 0, b.length);
			output.flush();
		} catch (IOException e) {
		}
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
	public String compileClass() throws CompilationException {
		processNext();
		JackClassArtifact klass = null;
		if (tokenizer.tokenType() == TokenType.KEYWORD) {
			writeKeyword(Keyword.CLASS); // class
			klass = new JackClassArtifact(tokenizer.identifier());
			writeIdentifier(); // className
			writeSymbol('{');
			if (tokenizer.tokenType() == TokenType.KEYWORD) {
				while (tokenizer.keyword() == Keyword.STATIC || tokenizer.keyword() == Keyword.FIELD) {
					compileClassVarDecl(klass);
				}
				while (tokenizer.keyword() == Keyword.FUNCTION || tokenizer.keyword() == Keyword.CONSTRUCTOR
						|| tokenizer.keyword() == Keyword.METHOD) {
					klass.subroutines.add(compileSubroutine());
				}
			}
			if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != '}') {
				throw new SyntaxErrorException("Expected \"}\"");
			}
		}
		if (tokenizer.hasMoreTokens()) {
			throw new CompilationException("Unexpected trailing text at end of class found");
		}
		return klass.writeVMCode();
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
			writeSymbol(',');
			varName = tokenizer.identifier();
			writeIdentifier();
			if (modifier == Keyword.STATIC) {
				klass.classVariables.add(new JackVariableArtifact(varName, type.toString()));
			} else {
				klass.instanceVariables.add(new JackVariableArtifact(varName, type.toString()));
			}
		}
		writeSymbol(';');
	}

	/**
	 * Compiles a complete method, function or constructor.
	 */
	private JackSubroutineArtifact compileSubroutine() throws CompilationException {
		JackSubroutineArtifact subroutine = null;
		Keyword type = tokenizer.keyword();
		writeKeyword(new Keyword[] { Keyword.CONSTRUCTOR, Keyword.FUNCTION, Keyword.METHOD });
		try {
			compileType();
		} catch (CompilationException e) {
			writeKeyword(Keyword.VOID);
		}
		String subroutineName = tokenizer.identifier();
		switch(type) {
		case CONSTRUCTOR:
			subroutine = new JackConstructorArtifact(subroutineName);
			break;
		case FUNCTION:
			subroutine = new JackFunctionArtifact(subroutineName);
			break;
		case METHOD:
			subroutine = new JackMethodArtifact(subroutineName);
			break;
		default:
			break;
		}
		writeIdentifier();
		writeSymbol('(');
		subroutine.arguments = compileParametersList();
		writeSymbol(')');
		writeSymbol('{');

		// write variables
		while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
			List<JackVariableArtifact> vars = compileVarDecl();
			subroutine.locals.addAll(vars);
		}
		// write statements
		List<JackStatementArtifact> statements = compileStatements();
		subroutine.statements = statements;
		writeSymbol('}');
		return subroutine;
	}

	/**
	 * Compiles a (possibly empty) parameter list, not including the enclosing
	 * "()".
	 */
	private List<JackVariableArtifact> compileParametersList() throws CompilationException {
		List<JackVariableArtifact> arguments = new ArrayList<>();
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
			Object type = tokenizer.tokenType() == TokenType.KEYWORD ? tokenizer.keyword() : tokenizer.identifier();
			compileType();
			String name = tokenizer.identifier();
			arguments.add(new JackVariableArtifact(name, type.toString()));
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
	private List<JackVariableArtifact> compileVarDecl() throws CompilationException {
		List<JackVariableArtifact> list = new ArrayList<>();
		writeKeyword(Keyword.VAR);
		Object type = tokenizer.tokenType() == TokenType.KEYWORD ? tokenizer.keyword() : tokenizer.identifier();
		compileType(); // type
		String name = tokenizer.identifier();
		list.add(new JackVariableArtifact(name, type.toString()));
		writeIdentifier(); // varName
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
			writeSymbol(',');
			name = tokenizer.identifier();
			list.add(new JackVariableArtifact(name, type.toString()));
			writeIdentifier();
		}
		writeSymbol(';');
		return list;
	}

	/**
	 * Compiles and sequence of statements, not including the enclosing "{}".
	 */
	private List<JackStatementArtifact> compileStatements() throws CompilationException {
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
		return statements;
	}

	/**
	 * Compiles a do statement.
	 */
	private JackDoStatementArtifact compileDo() throws CompilationException {
		writeKeyword(Keyword.DO);
		JackSubroutineCallTerm doStatement = new JackSubroutineCallTerm();
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
		doStatement.parameters = compileExpressionList();
		writeSymbol(')');
		writeSymbol(';');
		JackDoStatementArtifact statement = new JackDoStatementArtifact();
		statement.subroutine = doStatement;
		return statement;
	}

	/**
	 * Compiles a let statement.
	 */
	private JackLetStatementArtifact compileLet() throws CompilationException {
		JackLetStatementArtifact let = new JackLetStatementArtifact();
		writeKeyword(Keyword.LET);
		String assignTo = tokenizer.identifier();
		writeIdentifier();
		if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
			JackArrayAccessTerm arrayAccess = new JackArrayAccessTerm();
			arrayAccess.varname = assignTo;
			writeSymbol('[');
			arrayAccess.expression = compileExpression();
			writeSymbol(']');
			let.arrayAccess = arrayAccess;
		} else {
			let.assignee = assignTo;
		}
		writeSymbol('=');
		let.expression = compileExpression();
		writeSymbol(';');
		return let;
	}

	/**
	 * Compiles a while statement.
	 */
	private JackWhileStatementArtifact compileWhile() throws CompilationException {
		JackWhileStatementArtifact whileStatement = new JackWhileStatementArtifact();
		writeKeyword(Keyword.WHILE);
		writeSymbol('(');
		whileStatement.condition = compileExpression();
		writeSymbol(')');
		writeSymbol('{');
		whileStatement.statements = compileStatements();
		writeSymbol('}');
		return whileStatement;
	}

	/**
	 * Compiles a return statement.
	 */
	private JackReturnStatementArtifact compileReturn() throws CompilationException {
		JackReturnStatementArtifact statement = new JackReturnStatementArtifact();
		writeKeyword(Keyword.RETURN);
		if (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
			statement.expression = compileExpression();
		}
		writeSymbol(';');
		return statement;
	}

	/**
	 * Compiles an if statement, possibly with a trailing else clause.
	 */
	private JackIfStatementArtifact compileIf() throws CompilationException {
		JackIfStatementArtifact ifStatement = new JackIfStatementArtifact();
		writeKeyword(Keyword.IF);
		writeSymbol('(');
		ifStatement.condition = compileExpression();
		writeSymbol(')');
		writeSymbol('{');
		ifStatement.trueClause = compileStatements();
		writeSymbol('}');
		try {
			writeKeyword(Keyword.ELSE);
			writeSymbol('{');
			ifStatement.falseClause = compileStatements();
			writeSymbol('}');
		} catch (CompilationException e) {
			processNext();
		}
		return ifStatement;
	}

	/**
	 * Compiles an expression.
	 */
	private JackExpressionArtifact compileExpression() throws CompilationException {
		JackExpressionArtifact expression = new JackExpressionArtifact();
		expression.left = compileTerm();
		if (tokenizer.tokenType() == TokenType.SYMBOL && (tokenizer.symbol() != ';' && tokenizer.symbol() != ',')
				&& tokenizer.symbol() != ')') {
			char sym = tokenizer.symbol();
			expression.op = sym;
			writeSymbol(new char[] { '+', '-', '*', '/', '&', '|', '<', '>', '=' });
			expression.right = compileTerm();
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
	private JackTermArtifact compileTerm() throws CompilationException {
		JackTermArtifact term = null;
		switch (tokenizer.tokenType()) {
		case INT_CONST:
			term = new JackIntegerConstant(tokenizer.intVal());
			processNext();
			break;
		case STRING_CONST:
			term = new JackStringConstantArifact(tokenizer.stringVal());
			processNext();
			break;
		case SYMBOL:
			char sym = tokenizer.symbol();
			switch (sym) {
			case '-':
			case '~':
				writeSymbol(new char[] {'-', '~'});
				term = new JackUnaryTermArifact();
				((JackUnaryTermArifact)term).op = sym;
				((JackUnaryTermArifact)term).term = compileTerm();
				break;
			case '(':
				writeSymbol('(');
				term = compileExpression();
				writeSymbol(')');
				break;
			}
			break;
		case KEYWORD:
			term = new JackKeywordTermArtifact(tokenizer.keyword().toString());
			writeKeyword(new Keyword[] {Keyword.THIS, Keyword.NULL, Keyword.FALSE, Keyword.TRUE});
			break;
		default:
			String identifier = tokenizer.identifier();
			processNext();
			if (tokenizer.tokenType() == TokenType.SYMBOL) {
				sym = tokenizer.symbol();
				if (sym == '[') {
					// handle array access
					term = new JackArrayAccessTerm();
					((JackArrayAccessTerm)term).varname = identifier;
					((JackArrayAccessTerm)term).expression = compileExpression();
					writeSymbol(']');
					break;
				} else if (sym == '(' || sym == '.') {
					// handle function call
					term = new JackSubroutineCallTerm();
					((JackSubroutineCallTerm)term).subroutineName = identifier;
					tokenizer.advance();
					if (sym == '(') {
						((JackSubroutineCallTerm)term).parameters = compileExpressionList();
						writeSymbol(')');
					} else {
						((JackSubroutineCallTerm)term).accessor = ((JackSubroutineCallTerm)term).subroutineName;
						((JackSubroutineCallTerm)term).subroutineName = tokenizer.identifier();
						writeIdentifier();
						writeSymbol('(');
						((JackSubroutineCallTerm)term).parameters = compileExpressionList();
						writeSymbol(')');
					}
					break;
				} else /* if (sym == ',' || sym == '<' || sym == '>') */ {
					// handle variable
					term = new JackVariableTerm();
					((JackVariableTerm)term).varname = identifier;
				}
			}
		}
		return term;
	}

	/**
	 * Compiles a (possibly empty) comma-separated list of expressions.
	 */
	private List<JackExpressionArtifact> compileExpressionList() throws CompilationException {
		List<JackExpressionArtifact> expressions = new ArrayList<>();
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
			expressions.add(compileExpression());
			while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
				writeSymbol(',');
				expressions.add(compileExpression());
			}
		}
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
	}
}