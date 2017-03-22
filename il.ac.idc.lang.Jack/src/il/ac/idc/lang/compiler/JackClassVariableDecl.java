package il.ac.idc.lang.compiler;

public class JackClassVariableDecl extends AbstractJackObject {

	private static int index = 0;
	private int id;
	JackVariableModifier modifier;
	JackVariableType type;
	JackVariableName name;
	
	public JackClassVariableDecl(int lineNumber, JackVariableModifier modifier, JackVariableType type, JackVariableName name) {
		super(lineNumber);
		this.type = type;
		this.modifier = modifier;
		this.name = name;
		id = index;
		index++;
	}
	
	@Override
	public String getName() {
		return getClassName() + ":var-decl-" + id;
	}

	@Override
	public String writeVMCode() {
		// TODO Auto-generated method stub
		return null;
	}

}
