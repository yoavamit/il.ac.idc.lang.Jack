package il.ac.idc.lang.compiler;

public class JackVariableDecl extends AbstractJackObject {

	private static int index = 0;
	JackVariableType type;
	JackVariableName name;
	private int id;
	
	public JackVariableDecl(int lineNumber, JackVariableType type, JackVariableName name) {
		super(lineNumber);
		this.type = type;
		this.name = name;
		id = index;
		index++;
	}

	@Override
	public String getId() {
		return getClassName() + "." + getSubroutineName() + ":var-decl-" + id;
	}

	@Override
	public String writeVMCode() {
		return null;
	}

}
