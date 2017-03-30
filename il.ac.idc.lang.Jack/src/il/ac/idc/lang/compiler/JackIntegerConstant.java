package il.ac.idc.lang.compiler;

public class JackIntegerConstant extends AbstractJackTerm {

	private int constant;
	
	public JackIntegerConstant(int line, int val) {
		super(line);
		this.constant = val;
	}
	
	@Override
	public String writeVMCode() {
		String code = "";
		if (lineNumber != parent.lineNumber) {
			code = "// sourceLine:" + lineNumber + "\n";
		}
		code += "push constant " + constant + "\n";
		return code;
	}

	@Override
	public String getId() {
		return "integer-constant-" + constant;
	}
}
