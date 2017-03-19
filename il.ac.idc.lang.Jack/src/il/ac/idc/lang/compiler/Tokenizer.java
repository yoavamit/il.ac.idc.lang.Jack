package il.ac.idc.lang.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Tokenizer {

	public enum TokenType {
		KEYWORD,
		SYMBOL,
		IDENTIFIER,
		INT_CONST,
		STRING_CONST
	}
	
	public enum Keyword {
		CLASS ("class"),
		METHOD ("method"),
		FUNCTION ("function"),
		CONSTRUCTOR ("constructor"),
		INT ("int"),
		BOOLEAN ("boolean"),
		CHAR ("char"),
		VOID ("void"),
		VAR ("var"),
		STATIC ("static"),
		FIELD ("field"),
		LET ("let"),
		DO ("do"),
		IF ("if"),
		ELSE ("else"),
		WHILE ("while"),
		RETURN ("return"),
		TRUE ("true"),
		FALSE ("false"),
		NULL ("null"),
		THIS ("this");
		
		private String value;
		
		Keyword(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	private static final Map<String, Keyword> keywords =new HashMap<>();
	private static final char[] symbols = new char[] {'(', ')', '{', '}', '[', ']',
			'.', ',', ';', '+', '=', '-', '*', '/', '&', '|', '<', '>', '~'};

	static {
		keywords.put("class", Keyword.CLASS);
		keywords.put("method", Keyword.METHOD);
		keywords.put("function", Keyword.FUNCTION);
		keywords.put("constructor", Keyword.CONSTRUCTOR);
		keywords.put("int", Keyword.INT);
		keywords.put("boolean", Keyword.BOOLEAN);
		keywords.put("char", Keyword.CHAR);
		keywords.put("void", Keyword.VOID);
		keywords.put("var", Keyword.VAR);
		keywords.put("static", Keyword.STATIC);
		keywords.put("field", Keyword.FIELD);
		keywords.put("let", Keyword.LET);
		keywords.put("do", Keyword.DO);
		keywords.put("if", Keyword.IF);
		keywords.put("else", Keyword.ELSE);
		keywords.put("while", Keyword.WHILE);
		keywords.put("return", Keyword.RETURN);
		keywords.put("true", Keyword.TRUE);
		keywords.put("false", Keyword.FALSE);
		keywords.put("null", Keyword.NULL);
		keywords.put("this", Keyword.THIS);
	}
	
	private String currentToken;
	private Scanner scanner;
	private String currentLine;
	private int currentLineNumber;
	
	/**
	 * Opens the input file/stream and gets ready to tokenize it
	 * @param stream
	 */
	public Tokenizer(InputStream stream) throws IOException {
		scanner = new Scanner(stream);
		currentToken = null;
		currentLineNumber = 0;
	}
	
	/**
	 * Do we have more tokens in the input?
	 * @return
	 */
	public boolean hasMoreTokens() {
		return scanner.hasNext() || !currentLine.isEmpty();
	}
	
	/**
	 * Returns the current line number in the source code.
	 * Line numbers are 1 based, and include comments and empty lines
	 * @return
	 */
	public int getCurrentLineNumber() {
		return currentLineNumber;
	}
	
	/**
	 * Gets the next token from the input and makes it the current token.
	 * This method should only be called if hasMoreTokens() is true.
	 * Initially, there is no current token.
	 */
	public void advance() {
		// skip empty lines and comments
		while (currentLine == null || currentLine.isEmpty() || currentLine.startsWith("//") || currentLine.startsWith("/*")) {
			currentLine = scanner.nextLine().trim();
			currentLineNumber++;
			skipComments();
		}
		// check for keyword
		for (String key : keywords.keySet()) {
			if (currentLine.startsWith(key)) {
				currentToken = key;
				currentLine = currentLine.substring(currentToken.length()).trim();
				return;
			}
		}
		// check for string constant
		if (currentLine.startsWith("\"")) {
			currentToken = currentLine.substring(0, currentLine.indexOf("\"", 1) + 1);
			currentLine = currentLine.substring(currentToken.length()).trim();
			return;
		}
		// check for integer constant
		char first = currentLine.charAt(0);
		if (first >= '0' && first <= '9') {
			currentToken = "" + first;
			int i = 1;
			first = currentLine.charAt(i);
			while(first >= '0' && first <= '9') {
				currentToken += first;
				i++;
				first = currentLine.charAt(i);
			}
			currentLine = currentLine.substring(i).trim();
			return;
		}
		// check for symbol
		for (int i = 0; i < symbols.length; i++) {
			if (first == symbols[i]) {
				currentToken = "" + first;
				currentLine = currentLine.substring(1).trim();
				return;
			}
		}
		int endTokenIndex = Math.min(nextSpaceIndex(), nextSymbolIndex());
		currentToken = currentLine.substring(0, endTokenIndex);
		currentLine = currentLine.substring(endTokenIndex).trim();
	}
	
	private void skipComments() {
		while(currentLine.startsWith("//")) {
			currentLine = scanner.nextLine().trim();
			currentLineNumber++;
		}
		if (currentLine.startsWith("/*")) {
			int endBlock = currentLine.indexOf("*/");
			while(endBlock == -1) {
				currentLine = scanner.nextLine();
				currentLineNumber++;
				endBlock = currentLine.indexOf("*/");
			}
			currentLine = currentLine.substring(endBlock + 2);
		}
	}
	
	private int nextSpaceIndex() {
		int index = currentLine.indexOf(" ");
		return index >= 0 ? index : Integer.MAX_VALUE;
	}
	
	private int nextSymbolIndex() {
		int minIndex = Integer.MAX_VALUE;
		for (int i = 0; i < symbols.length; i++) {
			int index = currentLine.indexOf(symbols[i]);
			if (index != -1 && index < minIndex) {
				minIndex = index;
			}
		}
		return minIndex;
	}
	/**
	 * Returns the type of the current token
	 * @return
	 */
	public TokenType tokenType() {
		if (keywords.containsKey(currentToken)) {
			return TokenType.KEYWORD;
		} else {
			// check for symbol
			for (int i = 0; i < symbols.length; i++) {
				if (currentToken.charAt(0) == symbols[i]) {
					return TokenType.SYMBOL;
				}
			}
			// check for string constant
			if (currentToken.startsWith("\"")) {
				return TokenType.STRING_CONST;
			}
			// check for integer constant
			try {
				Integer.parseInt(currentToken);
				return TokenType.INT_CONST;
			} catch (NumberFormatException e) {
				// then it must be an identifier
				return TokenType.IDENTIFIER;
			}
		}
	}
	
	/**
	 * Returns the keyword which is the current token.
	 * Should only be called when tokenType() is KEYWORD
	 * @return
	 */
	public Keyword keyword() {
		return keywords.get(currentToken);
	}
	
	/**
	 * Returns the character which is the current token.
	 * Should be called only when tokenType() is SYMBOL
	 * @return
	 */
	public char symbol() {
		return currentToken.charAt(0);
	}
	
	/**
	 * Returns the identifier which is the current token.
	 * Should be called only when tokenType() is IDENTIFIER
	 * @return
	 */
	public String identifier() {
		return currentToken;
	}
	
	/**
	 * Returns the integer value of the current token.
	 * Should be called only when tokenType() is INT_CONST
	 * @return
	 */
	public int intVal() {
		return Integer.parseInt(currentToken);
	}
	
	/**
	 * Returns the string value of the current token.
	 * Should be called only when tokenType() is STRING_CONST
	 * @return
	 */
	public String stringVal() {
		return currentToken.substring(1, currentToken.length() - 2);
	}
}
