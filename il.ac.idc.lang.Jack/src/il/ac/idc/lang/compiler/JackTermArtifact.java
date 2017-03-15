package il.ac.idc.lang.compiler;

public abstract class JackTermArtifact implements IJackLanguageArtifact {

	IJackLanguageArtifact parent;
	
	@Override
	public IJackLanguageArtifact getParent() {
		return parent;
	}
}
