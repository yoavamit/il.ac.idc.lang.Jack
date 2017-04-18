package il.ac.idc.lang.compiler;

public class JackDoStatement extends AbstractJackStatement {

	private static int index = 0;
	private int id;
	private JackSubroutineCallTerm call;
	
	public JackDoStatement(int lineNumber, JackSubroutineCallTerm call) {
		super(lineNumber);
		call.parent = this;
		this.call = call;
		id = index;
		index++;
	}

	@Override
	public String writeVMCode() {
		String code = "";
		if (lineNumber != parent.lineNumber) {
			code = "// sourceLine:"+ lineNumber + "\n"; 
		}
		code += call.writeVMCode();
		return code;
	}

	@Override
	public String getId() {
		return getKlassName() + "." + getSubroutineName() + ":statement-do-" + id;
	}
}
