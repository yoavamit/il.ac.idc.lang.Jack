package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public class JackClassArtifact implements IJackLanguageArtifact {

	String name;
	IJackLanguageArtifact parent;
	
	List<JackVariableArtifact> classVariables = new ArrayList<>();
	List<JackVariableArtifact> instanceVariables = new ArrayList<>();
	List<JackSubroutineArtifact> subroutines = new ArrayList<>();
	
	public JackClassArtifact(String name) {
		this.name = name;
	}
		
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		boolean addDefaultCtor = true;
		for (JackSubroutineArtifact sub : subroutines) {
			if (sub instanceof JackConstructorArtifact) {
				addDefaultCtor = false;
			}
		}
		if (addDefaultCtor) {
			builder.append("function " + name + "." + "new 1\n");
			builder.append("push constant " + instanceVariables.size() + "\n");
			builder.append("call Memory.alloc 1\n");
			builder.append("return\n");			
		}
		for (JackSubroutineArtifact sub : subroutines) {
			sub.parent = this;
			builder.append(sub.writeVMCode());
		}
		return builder.toString();
	}

	@Override
	public IJackLanguageArtifact getParent() {
		return parent;
	}
}
