package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public abstract class JackSubroutineArtifact implements IJackLanguageArtifact {

	String returnType;
	String name;
	IJackLanguageArtifact parent;
	List<JackVariableArtifact> arguments = new ArrayList<>();
	List<JackVariableArtifact> locals = new ArrayList<>();
	List<JackStatementArtifact> statements = new ArrayList<>();
	
	public JackSubroutineArtifact(String name) {
		this.name = name;
	}
	
	@Override
	public IJackLanguageArtifact getParent() {
		return parent;
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		String className = ((JackClassArtifact)parent).name;
		builder.append("function " + className + "." + name + " " + arguments.size() +"\n");
		for (int i = 0; i < locals.size(); i++) {
			builder.append("push constant 0\n");
		}
		for (JackStatementArtifact statement : statements) {
			statement.parent = this;
			builder.append(statement.writeVMCode());
		}
		return builder.toString();
	}
}
