package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public class JackClass extends AbstractJackObject {

	private String name;
	List<JackVariable> classVariables = new ArrayList<>();
	List<JackVariable> instanceVariables = new ArrayList<>();
	List<AbstractJackSubroutine> subroutines = new ArrayList<>();
	
	public JackClass(int lineNumber, String name) {
		super(lineNumber);
		this.name = name;
		parent = this;
	}
	
	void addClassVariable(JackVariable var) {
		classVariables.add(var);
		var.parent = this;
	}
	
	void addInstanceVariable(JackVariable var) {
		instanceVariables.add(var);
		var.parent = this;
	}
	
	void addSubroutine(AbstractJackSubroutine subroutine) {
		subroutine.parent = this;
		subroutines.add(subroutine);
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		builder.append("// " + getName() + "\n");
		boolean addDefaultCtor = true;
		for (AbstractJackSubroutine sub : subroutines) {
			if (sub instanceof JackConstructor) {
				addDefaultCtor = false;
			}
		}
		if (addDefaultCtor) {
			builder.append("// " + getName() + ".new\n");
			builder.append("function " + name + "." + "new 1\n");
			builder.append("push constant " + instanceVariables.size() + "\n");
			builder.append("call Memory.alloc 1\n");
			builder.append("return\n");			
		}
		for (AbstractJackSubroutine sub : subroutines) {
			builder.append(sub.writeVMCode());
		}
		return builder.toString();
	}

	@Override
	public String getName() {
		return name;
	}
}
