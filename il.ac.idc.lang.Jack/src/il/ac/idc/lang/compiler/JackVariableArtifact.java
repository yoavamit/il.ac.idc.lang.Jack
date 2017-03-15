package il.ac.idc.lang.compiler;

public class JackVariableArtifact implements IJackLanguageArtifact {

	String name, type;
	
	public JackVariableArtifact(String name, String type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public IJackLanguageArtifact getParent() {
		return null;
	}

	@Override
	public String writeVMCode() {
		return null;
	}
}
