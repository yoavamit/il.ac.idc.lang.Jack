package il.ac.idc.lang.compiler;

public abstract class JackStatementArtifact implements IJackLanguageArtifact {

	IJackLanguageArtifact parent;
	
	@Override
	public IJackLanguageArtifact getParent() {
		return parent;
	}
}
