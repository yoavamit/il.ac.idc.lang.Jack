package il.ac.idc.lang.compiler;

public class JackReturnStatementArtifact extends JackStatementArtifact {
	
	JackExpressionArtifact expression;
	
	@Override
	public String writeVMCode() {
		if (expression != null) {
			expression.parent = this;
			return expression.writeVMCode();
		} else {
			return "push constant 0\n";
		}
	}

}
