package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public class JackClassArtifact implements IJackLanguageArtifact {

	String name;
	IJackLanguageArtifact parent;
	
	List<JackVariableArtifact> classVariables = new ArrayList<>();
	List<JackVariableArtifact> instanceVariables = new ArrayList<>();
	List<JackSubroutineArtifact> subroutines = new ArrayList<>();
//	List<JackFunctionArtifact> functions = new ArrayList<>();
//	List<JackMethodArtifact> methods = new ArrayList<>();
	
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
			builder.append("label " + name + "." + "new\n");
			builder.append("push constant " + instanceVariables.size() + "\n");
			builder.append("call Memory.alloc 1\n");
			builder.append("return\n");			
		}
		for (JackSubroutineArtifact sub : subroutines) {
			sub.parent = this;
			builder.append(sub.writeVMCode());
		}
//		for (JackFunctionArtifact function : functions) {
//			function.parent = this;
//			builder.append(function.writeVMCode());
//		}
//		for (JackMethodArtifact function : methods) {
//			function.parent = this;
//			builder.append(function.writeVMCode());
//		}
		return builder.toString();
	}

	@Override
	public IJackLanguageArtifact getParent() {
		return parent;
	}
}
