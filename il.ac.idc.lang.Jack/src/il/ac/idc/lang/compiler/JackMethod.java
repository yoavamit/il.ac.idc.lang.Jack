package il.ac.idc.lang.compiler;

public class JackMethod extends AbstractJackSubroutine {

	public JackMethod(int line, String name) {
		super(line, name);
	}

	@Override
	public String getId() {
		return getKlassName() + "." + name;
	}

	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		JackVariableDecl self = new JackVariableDecl(lineNumber, new JackVariableType(getKlassName()), new JackVariableName("this"));
		self.parent = this;
		arguments.add(0, self);
		// write variable symbol table
		for (int i = 0; i < arguments.size(); i++) {
			builder.append("// arg:" + arguments.get(i).getId() + "\n");
		}
		if(lineNumber != parent.lineNumber) {
			builder.append("// sourceLine:" + lineNumber + "\n");
		}
		String className = parent.getId();
		builder.append("function " + className + "." + name + " " + locals.size() +"\n");
		for (int i = 0; i < locals.size(); i++) {
			builder.append("// var:" + locals.get(i).getId() + "\n");
			builder.append("push constant 0\n");
		}
		// set the "this" pointer to the current object
		builder.append("push argument 0\n");
		builder.append("pop pointer 0\n");
		for (AbstractJackStatement statement : statements) {
			builder.append(statement.writeVMCode());
		}
		return builder.toString();
	}
}
