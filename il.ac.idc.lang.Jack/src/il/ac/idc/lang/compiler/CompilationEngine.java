package il.ac.idc.lang.compiler;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import il.ac.idc.lang.compiler.Tokenizer.Keyword;
import il.ac.idc.lang.compiler.Tokenizer.TokenType;

public class CompilationEngine {

	private Tokenizer tokenizer;
	private BufferedOutputStream output;
	private int offset = 0;
	private String indent = "";

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
	}

	private void writeToOutput(String str) {
		byte[] b = (indent + str + "\n").getBytes();
		try {
			output.write(b, offset, b.length);
			offset += b.length;
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
					"Unexpected keyword: " + key.getValue() + ", expected one of " + expected.toString());
		}
	}

	private void writeSymbol(char[] expected) throws CompilationException {
		TokenType type = tokenizer.tokenType();
		if (type != TokenType.SYMBOL) {
			throw new CompilationException(
					"Unexpected token of type " + type + ", expected one of :" + expected.toString());
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
				throw new CompilationException(
						"Unexpected symbol: " + sym + ". Should be one of " + expected.toString());
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
			writeIdentifier(); // className
			writeSymbol(new char[] { '{' });
			if (tokenizer.tokenType() == TokenType.KEYWORD) {
				Keyword keyword = tokenizer.keyword();
				while (keyword == Keyword.STATIC || keyword == Keyword.FIELD) {
					compileClassVarDecl();
				}
				while (keyword == Keyword.FUNCTION || keyword == Keyword.CONSTRUCTOR || keyword == Keyword.METHOD) {
					compileSubroutine();
				}
			}
			writeSymbol(new char[] { '}' });
		}
		if (!tokenizer.hasMoreTokens()) {
			indent = indent.substring(4);
			writeToOutput("</class>");
		}
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
	public void compileClassVarDecl() throws CompilationException {
		writeToOutput("<classVarDec>");
		indent += "    ";
		writeKeyword(new Keyword[] { Keyword.STATIC, Keyword.FIELD }); // static/field
		compileType();
		while (tokenizer.symbol() != ';') {
			writeSymbol(new char[] { ',' });
			writeIdentifier();
		}
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
		writeIdentifier();
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
		writeSymbol(new char[] { '}' });
		indent = indent.substring(4);
		writeToOutput("</subroutine>");
		tokenizer.advance();
	}

	/**
	 * Compiles a (possibly empty) parameter list, not including the enclosing
	 * "()".
	 */
	public void compileParametersList() throws CompilationException {
		writeToOutput("<parameterList>");
		indent += "    ";
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')')) {
			compileType();
			writeIdentifier();
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
		writeKeyword(new Keyword[] { Keyword.VAR }); // var
		compileType(); // type
		writeIdentifier(); // varName
		while (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
			writeSymbol(new char[] { ',' });
			writeIdentifier();
		}
		writeSymbol(new char[] { ';' });
		indent = indent.substring(4);
		writeToOutput("</varDec>");
		tokenizer.advance();
	}

	/**
	 * Compiles and sequence of statements, not including the enclosing "{}".
	 */
	public void compileStatements() throws CompilationException {
		writeToOutput("<statements>");
		indent += "    ";
		TokenType type = tokenizer.tokenType();
		while (!(type == TokenType.SYMBOL && tokenizer.symbol() == '}')) {
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
		writeIdentifier();
		TokenType type = tokenizer.tokenType();
		if (type == TokenType.SYMBOL) {
			writeSymbol(new char[] { '.' });
			writeIdentifier();
		}
		writeSymbol(new char[] { '(' });
		compileExpressionList();
		writeSymbol(new char[] { ')' });
		writeToOutput("</subroutineCall>");
		indent = indent.substring(4);
		writeSymbol(new char[] { ';' });
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
		writeIdentifier();
		if (tokenizer.tokenType() == TokenType.SYMBOL) {
			writeSymbol(new char[] { '[' });
			compileExpression();
			writeSymbol(new char[] { ']' });
		}
		writeSymbol(new char[] { '=' });
		compileExpression();
		writeSymbol(new char[] { ';' });
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
		while (tokenizer.tokenType() == TokenType.SYMBOL) {
			writeSymbol(new char[] {'+', '-', '*', '/', '&', '|', '<', '>', '='});
			compileTerm();
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
			tokenizer.advance();
			break;
		case STRING_CONST:
			writeToOutput("<stringConst>" + tokenizer.stringVal() + "</stringConst>");
			tokenizer.advance();
			break;
		case SYMBOL:
			try {
				writeSymbol(new char[] {'-', '~'});
				compileTerm();
			} catch (CompilationException e) {
				writeSymbol(new char[] {'('});
				compileExpression();
				writeSymbol(new char[] {')'});
			}
			break;
		default:
			if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
				String identifier = tokenizer.identifier();
				tokenizer.advance();
				if (tokenizer.tokenType() == TokenType.SYMBOL) {
					char sym = tokenizer.symbol();
					if (sym == '[') {
						writeToOutput("<identifier>" + identifier + "</identifier>");
						writeToOutput("<symbol>" + sym + "</symbol>");
						compileExpression();
						writeSymbol(new char[] {']'});
					} else if (sym == '(' || sym == '.') {
						writeToOutput("<subroutineCall>");
						indent += "    ";
						writeToOutput("<identifier>" + identifier + "</identifier>");
						writeToOutput("<symbol>" + sym + "</symbol>");
						tokenizer.advance();
						if (sym == '(') {
							compileExpressionList();
							writeSymbol(new char[] {')'});
						} else {
							writeIdentifier();
							writeSymbol(new char[] {'('});
							compileExpressionList();
							writeSymbol(new char[] {')'});
						}
						indent = indent.substring(4);
						writeToOutput("</subroutineCall>");
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
}