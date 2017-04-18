package il.ac.idc.lang.compiler;

public class JackConstructor extends AbstractJackSubroutine {

	private static int index = 0;
	private int id;
	public JackConstructor(int line, String name) {
		super(line, name);
		id = index;
		index++;
		this.name = "new";
	}

	@Override
	public String getId() {
		return getKlassName() + ".new-" + id;
	}

	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		// write variable symbol table
		for (int i = 0; i < arguments.size(); i++) {
			builder.append("// arg:" + arguments.get(i).getId() + "\n");
		}
		if(lineNumber != parent.lineNumber) {
			builder.append("// sourceLine:" + lineNumber + "\n");
		}
		String className = parent.getId();
		builder.append("function " + className + ".new " + locals.size() +"\n");
		for (int i = 0; i < locals.size(); i++) {
			builder.append("// var:" + locals.get(i).getId() + "\n");
			builder.append("push constant 0\n");
		}
		JackClass klass = (JackClass) getParent();
		int numInstanceVars = 0;
		for (JackClassVariableDecl decl : klass.classVariables) {
			if (decl.modifier.getTerminal().equals("field")) {
				numInstanceVars ++;
			}
		}
		if (numInstanceVars > 0) {
			builder.append("push constant " + numInstanceVars + "\n");
			builder.append("call Memory.alloc 1\n");
			builder.append("pop pointer 0\n");
		}
		for (AbstractJackStatement statement : statements) {
			builder.append(statement.writeVMCode());
		}
		return builder.toString();
	}
}
