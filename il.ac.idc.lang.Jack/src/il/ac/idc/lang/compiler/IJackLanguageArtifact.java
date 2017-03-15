package il.ac.idc.lang.compiler;

public interface IJackLanguageArtifact {

	IJackLanguageArtifact getParent();
	
	String writeVMCode();
}
