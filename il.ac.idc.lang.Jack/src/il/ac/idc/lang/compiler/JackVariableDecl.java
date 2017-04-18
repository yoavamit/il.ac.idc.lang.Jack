package il.ac.idc.lang.compiler;

public class JackVariableDecl extends AbstractJackObject {

	JackVariableType type;
	JackVariableName name;
	
	public JackVariableDecl(int lineNumber, JackVariableType type, JackVariableName name) {
		super(lineNumber);
		this.type = type;
		this.name = name;
	}

	@Override
	public String getId() {
		return getSubroutineName() + "." + name.terminal + "." + type.getTerminal();
	}

	@Override
	public String writeVMCode() {
		return null;
	}

}
