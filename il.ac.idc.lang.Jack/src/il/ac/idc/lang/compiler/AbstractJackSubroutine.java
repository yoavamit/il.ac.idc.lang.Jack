package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractJackSubroutine extends AbstractJackObject {

	String returnType;
	String name;
	List<JackVariable> arguments = new ArrayList<>();
	List<JackVariable> locals = new ArrayList<>();
	List<AbstractJackStatement> statements = new ArrayList<>();
	
	public AbstractJackSubroutine(int lineNumber, String name) {
		super(lineNumber);
		this.name = name;
	}
	
	void setArguments(List<JackVariable> args) {
		for (JackVariable arg : args) {
			arg.parent = this;
			arguments.add(arg);
		}
	}
	
	void addLocals(List<JackVariable> locals) {
		for (JackVariable local : locals) {
			local.parent = this;
			this.locals.add(local);
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
		builder.append("// " + getName() + "\n");
		String className = parent.getName();
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
