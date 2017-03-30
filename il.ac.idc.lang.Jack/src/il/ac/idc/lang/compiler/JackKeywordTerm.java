package il.ac.idc.lang.compiler;

public class JackKeywordTerm extends AbstractJackTerm {

	private String keyword;

	public JackKeywordTerm(int line, String keyword) {
		super(line);
		this.keyword = keyword;
	}
	
	@Override
	public String writeVMCode() {
		String code = "";
		if (lineNumber != parent.lineNumber) {
			code = "// sourceLine:" + lineNumber + "\n";
		}
		switch(keyword) {
		case "NULL":
			code += "push constant 0\n";
			break;
		case "FALSE":
			code += "push constant 0\n";
			break;
		case "TRUE":
			code += "push constant 0\nnot\n";
			break;
		case "THIS":
			code += "push pointer 0\n";
			break;
		}
		return code;
	}

	@Override
	public String getId() {
		return "keyword-constant-" + keyword;
	}
	
}
