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
		return "// "+ getName() + "\n" + call.writeVMCode();
	}

	@Override
	public String getName() {
		return getClassName() + "." + getSubroutineName() + ":statement-do-" + id;
	}
}
