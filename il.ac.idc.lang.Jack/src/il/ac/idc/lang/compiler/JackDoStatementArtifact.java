package il.ac.idc.lang.compiler;

public class JackDoStatementArtifact extends JackStatementArtifact {

	JackSubroutineCallTerm subroutine;

	@Override
	public String writeVMCode() {
		subroutine.parent = this;
		return subroutine.writeVMCode();
	}

}
