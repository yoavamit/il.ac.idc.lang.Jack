package il.ac.idc.lang.compiler;

public class JackDoStatement extends AbstractJackStatement {

	private static int id = 0;
	private JackSubroutineCallTerm call;
	
	public JackDoStatement(int lineNumber, JackSubroutineCallTerm call) {
		super(lineNumber);
		call.parent = this;
		this.call = call;
		id++;
	}

	@Override
	public String writeVMCode() {
		return "// "+ getName() + "\n" + call.writeVMCode();
	}

	@Override
	public String getName() {
		return getClassName() + "." + getSubroutineName() + ":statement-do-" + id;
	}
}
