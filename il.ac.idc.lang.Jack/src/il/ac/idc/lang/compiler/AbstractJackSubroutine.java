package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractJackSubroutine extends AbstractJackObject {

	String returnType;
	String name;
	List<JackVariableDecl> arguments = new ArrayList<>();
	List<JackVariableDecl> locals = new ArrayList<>();
	List<AbstractJackStatement> statements = new ArrayList<>();
	
	public AbstractJackSubroutine(int lineNumber, String name) {
		super(lineNumber);
		this.name = name;
	}
	
	void setArguments(List<JackVariableDecl> args) {
		for (JackVariableDecl arg : args) {
			arg.parent = this;
			arguments.add(arg);
		}
	}
	
	void addLocals(List<JackVariableDecl> vars) {
		for (JackVariableDecl decl : vars) {
			decl.parent = this;
			locals.add(decl);
		}
	}
	
	void setStatements(List<AbstractJackStatement> statements) {
		for (AbstractJackStatement statement : statements) {
			statement.parent = this;
			this.statements.add(statement);
		}
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		if(lineNumber != parent.lineNumber) {
			builder.append("// sourceLine:" + lineNumber + "\n");
		}
		String className = parent.getId();
		builder.append("function " + className + "." + name + " " + arguments.size() +"\n");
		for (int i = 0; i < locals.size(); i++) {
			builder.append("push constant 0\n");
		}
		for (AbstractJackStatement statement : statements) {
			builder.append(statement.writeVMCode());
		}
		return builder.toString();
	}
}
